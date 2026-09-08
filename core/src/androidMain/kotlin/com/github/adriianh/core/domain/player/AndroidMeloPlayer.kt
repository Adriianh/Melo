package com.github.adriianh.core.domain.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    init {
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
        })
    }

    override fun load(url: String, track: Track, initialPositionMs: Long) {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setAlbumArtist(track.artist)
            .setArtworkUri(track.artworkUrl?.let { Uri.parse(it) })
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaId(track.id)
            .setMediaMetadata(metadata)
            .build()

        runOnMain {
            if (initialPositionMs > 0) {
                exoPlayer.setMediaItem(mediaItem, initialPositionMs)
            } else {
                exoPlayer.setMediaItem(mediaItem)
            }
            exoPlayer.prepare()
            _state.update {
                it.copy(
                    currentTrack = track,
                    progressMs = initialPositionMs,
                    durationMs = track.durationMs,
                    isFinished = false
                )
            }
            exoPlayer.play()
        }
    }

    override fun play() {
        runOnMain {
            exoPlayer.play()
        }
    }

    override fun pause() {
        runOnMain {
            exoPlayer.pause()
        }
    }

    override fun stop() {
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

    override fun release() {
        stopProgressUpdate()
        scope.cancel()
        runOnMain {
            exoPlayer.release()
        }
    }

    override fun setIdleTrack(track: Track, initialPositionMs: Long) {
        runOnMain {
            val metadata = MediaMetadata.Builder()
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
                    error = null
                )
            }
        }
    }

    private fun startProgressUpdate() {
        runOnMain {
            progressJob?.cancel()
            progressJob = scope.launch {
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