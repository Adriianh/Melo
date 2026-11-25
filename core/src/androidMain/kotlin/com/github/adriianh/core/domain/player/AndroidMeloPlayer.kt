package com.github.adriianh.core.domain.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
class AndroidMeloPlayer(context: Context) : MeloPlayer {
    companion object {
        @Volatile
        private var simpleCache: SimpleCache? = null
        private val cacheLock = Any()
        private const val MAX_AUDIO_CACHE_SIZE_BYTES = 350L * 1024 * 1024 // 350 MB

        fun getAudioCache(context: Context): SimpleCache {
            return simpleCache ?: synchronized(cacheLock) {
                simpleCache ?: run {
                    val cacheDir = File(context.cacheDir, "melo_audio_cache")
                    val databaseProvider = StandaloneDatabaseProvider(context)
                    val evictor = LeastRecentlyUsedCacheEvictor(MAX_AUDIO_CACHE_SIZE_BYTES)
                    SimpleCache(cacheDir, evictor, databaseProvider).also {
                        simpleCache = it
                    }
                }
            }
        }
    }

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 15_000,
            /* maxBufferMs = */ 60_000,
            /* bufferForPlaybackMs = */ 500,
            /* bufferForPlaybackAfterRebufferMs = */ 1000
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(8000)
        .setReadTimeoutMs(8000)

    private val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(getAudioCache(context))
        .setUpstreamDataSourceFactory(upstreamFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    private val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(cacheDataSourceFactory)

    private val mainHandler = Handler(Looper.getMainLooper())

    private inline fun runOnMain(crossinline action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post { action() }
        }
    }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setLooper(Looper.getMainLooper())
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(loadControl)
        .setAudioAttributes(audioAttributes, true)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val _availableAudioOutputs = MutableStateFlow<List<AudioOutputDevice>>(emptyList())
    override val availableAudioOutputs: StateFlow<List<AudioOutputDevice>> = _availableAudioOutputs.asStateFlow()
    private val _selectedAudioOutputId = MutableStateFlow<String?>(null)
    override val selectedAudioOutputId: StateFlow<String?> = _selectedAudioOutputId.asStateFlow()

    private val audioDeviceCallback =
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                refreshAudioOutputs()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                refreshAudioOutputs()
            }
        }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    init {
        audioManager?.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
        refreshAudioOutputs()
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressUpdate() else stopProgressUpdate()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        isFinished = playbackState == Player.STATE_ENDED,
                        durationMs = if (playbackState == Player.STATE_READY) exoPlayer.duration else it.durationMs
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        error = error.message ?: "Playback error",
                    )
                }
            }
        })
    }

    override fun load(
        url: String,
        track: Track,
        initialPositionMs: Long,
    ) {
        val metadata =
            MediaMetadata
                .Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setAlbumArtist(track.artist)
                .setArtworkUri(track.artworkUrl?.let { Uri.parse(it) })
                .build()

        val mediaItem =
            MediaItem
                .Builder()
                .setUri(url)
                .setMediaId(track.id)
                .setMediaMetadata(metadata)
                .build()

        _state.update {
            it.copy(
                currentTrack = track,
                progressMs = initialPositionMs,
                durationMs = track.durationMs,
                isBuffering = true,
                isFinished = false,
                error = null,
            )
        }

        runOnMain {
            if (initialPositionMs > 0) {
                exoPlayer.setMediaItem(mediaItem, initialPositionMs)
            } else {
                exoPlayer.setMediaItem(mediaItem)
            }
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    override fun play() {
        runOnMain {
            // ExoPlayer ignores play() while idle: resume only after re-preparing the item
            // left by stop(), otherwise the media controls become a no-op.
            if (exoPlayer.playbackState == Player.STATE_IDLE && exoPlayer.mediaItemCount > 0) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
        }
    }

    override fun pause() {
        runOnMain {
            exoPlayer.pause()
        }
    }

    override fun stop() {
        stopProgressUpdate()
        _state.update {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                isFinished = false,
                error = null,
            )
        }
        runOnMain {
            exoPlayer.stop()
        }
    }

    override fun seekTo(positionMs: Long) {
        runOnMain {
            exoPlayer.seekTo(positionMs)
        }
    }

    override fun setVolume(volume: Float) {
        runOnMain {
            exoPlayer.volume = volume.coerceIn(0f, 1f)
        }
    }

    override fun refreshAudioOutputs() {
        val devices =
            queryMediaOutputDeviceInfos().map { info ->
                AudioOutputDevice(
                    id = info.id.toString(),
                    name = resolveAndroidDeviceName(info),
                    kind = resolveAndroidDeviceKind(info.type),
                    isDefault = info.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                )
            }
        _availableAudioOutputs.value = devices
        val selected = _selectedAudioOutputId.value
        if (selected != null && devices.none { it.id == selected }) {
            _selectedAudioOutputId.value = null
            runOnMain { exoPlayer.setPreferredAudioDevice(null) }
        }
    }

    override fun selectAudioOutput(deviceId: String?): Boolean {
        val target =
            if (deviceId == null) {
                null
            } else {
                queryMediaOutputDeviceInfos().firstOrNull { it.id.toString() == deviceId }
            }
        val resolvedId = if (deviceId != null && target != null) deviceId else null
        _selectedAudioOutputId.value = resolvedId
        runOnMain {
            exoPlayer.setPreferredAudioDevice(target)
        }
        return resolvedId == deviceId
    }

    override fun release() {
        stopProgressUpdate()
        runCatching { audioManager?.unregisterAudioDeviceCallback(audioDeviceCallback) }
        scope.cancel()
        runOnMain {
            exoPlayer.release()
        }
    }

    private fun queryMediaOutputDeviceInfos(): List<AudioDeviceInfo> {
        return audioManager
            ?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .orEmpty()
            .filter { isSupportedMediaOutputType(it.type) }
            .distinctBy { it.id }
    }

    private fun isSupportedMediaOutputType(type: Int): Boolean =
        when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            -> true
            else ->
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)) ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && type == AudioDeviceInfo.TYPE_HEARING_AID)
        }

    private fun resolveAndroidDeviceKind(type: Int): AudioOutputKind =
        when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioOutputKind.SPEAKER
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> AudioOutputKind.HEADPHONES
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> AudioOutputKind.BLUETOOTH
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> AudioOutputKind.USB
            AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC -> AudioOutputKind.HDMI
            else ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
                ) {
                    AudioOutputKind.BLUETOOTH
                } else {
                    AudioOutputKind.OTHER
                }
        }

    private fun resolveAndroidDeviceName(info: AudioDeviceInfo): String {
        val productName = info.productName?.toString()?.trim().orEmpty()
        return when (info.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES ->
                productName.ifBlank { "Wired Headphones" }
            else -> productName.ifBlank { "Audio Output ${info.id}" }
        }
    }

    override fun setIdleTrack(
        track: Track,
        initialPositionMs: Long,
    ) {
        runOnMain {
            exoPlayer.stop()
            val metadata =
                MediaMetadata
                    .Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setAlbumArtist(track.artist)
                    .setArtworkUri(track.artworkUrl?.let { Uri.parse(it) })
                    .build()
            exoPlayer.playlistMetadata = metadata
            _state.update {
                it.copy(
                    currentTrack = track,
                    progressMs = initialPositionMs,
                    durationMs = track.durationMs,
                    isPlaying = false,
                    isBuffering = false,
                    isFinished = false,
                    error = null,
                )
            }
        }
    }

    private fun startProgressUpdate() {
        runOnMain {
            progressJob?.cancel()
            progressJob =
                scope.launch {
                    while (true) {
                        _state.update { it.copy(progressMs = exoPlayer.currentPosition) }
                        delay(200.milliseconds)
                    }
                }
        }
    }

    private fun stopProgressUpdate() {
        runOnMain {
            progressJob?.cancel()
            progressJob = null
        }
    }
}
