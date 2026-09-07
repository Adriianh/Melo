package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track
import com.sun.jna.NativeLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds

class JvmMeloPlayer : MeloPlayer {
    private var factory: MediaPlayerFactory? = null
    private var mediaPlayer: MediaPlayer? = null
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + kotlinx.coroutines.SupervisorJob())
    private var progressJob: Job? = null
    private val loadStartTimes = ConcurrentHashMap<String, Long>()
    private var lastLoadedTrackId: String? = null
    private var currentVolume = 75

    private val initJob: Job = scope.launch(Dispatchers.IO) {
        initializePlayer()
    }

    private fun initializePlayer() {
        try {
            val args = mutableListOf(
                "--no-video",
                "--no-xlib",
                "--no-osd",
                "--no-sub-autodetect-file",
                "--no-spu",
                "--no-stats",
                "--no-snapshot-preview",
                "--network-caching=1500",
                "--file-caching=1000",
                "--clock-jitter=0",
            )
            detectedPluginsDir?.let {
                args.add("--plugin-path=${it.absolutePath}")
            }
            val createdFactory = MediaPlayerFactory(args)
            factory = createdFactory
            val player = createdFactory.mediaPlayers().newMediaPlayer()
            mediaPlayer = player

            player.events().addMediaPlayerEventListener(
                object : MediaPlayerEventAdapter() {
                    override fun playing(mediaPlayer: MediaPlayer?) {
                        _state.update {
                            it.copy(
                                isPlaying = true,
                                isBuffering = false,
                                isFinished = false,
                                error = null
                            )
                        }
                        scope.launch {
                            delay(500.milliseconds)
                            mediaPlayer?.audio()?.setVolume(currentVolume)
                            delay(2000.milliseconds)
                            mediaPlayer?.audio()?.setVolume(currentVolume)
                        }
                        startProgressUpdate()
                        val id = lastLoadedTrackId ?: _state.value.currentTrack?.id
                        if (id != null) {
                            loadStartTimes.remove(id)
                        }
                    }

                    override fun buffering(mediaPlayer: MediaPlayer?, newCache: Float) {
                        val isBuffering = newCache < 100f
                        _state.update { it.copy(isBuffering = isBuffering) }
                    }

                    override fun paused(mediaPlayer: MediaPlayer?) {
                        _state.update { it.copy(isPlaying = false) }
                        stopProgressUpdate()
                    }

                    override fun stopped(mediaPlayer: MediaPlayer?) {
                        _state.update { it.copy(isPlaying = false) }
                        stopProgressUpdate()
                    }

                    override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) {
                        _state.update { it.copy(durationMs = newLength) }
                    }

                    override fun error(mediaPlayer: MediaPlayer?) {
                        _state.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                isFinished = false,
                                error = "Playback error: Check your internet connection or stream source."
                            )
                        }
                    }

                    override fun finished(mediaPlayer: MediaPlayer?) {
                        _state.update { it.copy(isPlaying = false, isFinished = true) }
                        stopProgressUpdate()
                    }
                }
            )
            player.audio().setVolume(currentVolume)
        } catch (e: Throwable) {
            log.warning("Could not initialize VLC MediaPlayerFactory: ${e.message}")
            _state.update {
                it.copy(error = "VLC media engine could not be loaded: ${e.message}")
            }
        }
    }

    override fun load(url: String, track: Track) {
        _state.update {
            it.copy(
                currentTrack = track,
                progressMs = 0,
                durationMs = track.durationMs,
                isBuffering = true,
                isFinished = false,
                error = null
            )
        }
        val now = System.currentTimeMillis()
        lastLoadedTrackId = track.id
        loadStartTimes[track.id] = now
        scope.launch(Dispatchers.IO) {
            initJob.join()
            val player = mediaPlayer
            if (player == null) {
                _state.update {
                    it.copy(
                        currentTrack = track,
                        isPlaying = false,
                        isBuffering = false,
                        error = "VLC media engine is not available. Please install VLC 64-bit."
                    )
                }
                return@launch
            }
            try {
                player.controls().stop()
                val (mediaTarget, options) = resolveMediaTarget(url)
                player.media().play(mediaTarget, *options)
                player.audio().setVolume(currentVolume)
            } catch (e: Throwable) {
                _state.update {
                    it.copy(
                        isBuffering = false,
                        isPlaying = false,
                        error = "Playback error: ${e.message}"
                    )
                }
            }
        }
    }

    private fun resolveMediaTarget(url: String): Pair<String, Array<String>> {
        val localPath = when {
            url.startsWith("file://") || url.startsWith("file:/") -> {
                runCatching {
                    val cleanUri = if (url.startsWith("file://") && !url.startsWith("file:///")) {
                        "file:///" + url.removePrefix("file://")
                    } else url
                    File(java.net.URI(cleanUri.replace('\\', '/'))).absolutePath
                }.getOrNull() ?: url.removePrefix("file://").removePrefix("file:/")
            }
            File(url).exists() -> url
            else -> null
        }

        if (localPath != null && File(localPath).exists()) {
            val localOptions = arrayOf(
                ":file-caching=500",
                ":no-video",
                ":no-spu",
            )
            return Pair(File(localPath).absolutePath, localOptions)
        }

        return Pair(url, buildVlcOptions(url))
    }

    override fun play() {
        _state.update { it.copy(isPlaying = true) }
        scope.launch(Dispatchers.IO) {
            initJob.join()
            mediaPlayer?.controls()?.play()
            mediaPlayer?.audio()?.setVolume(currentVolume)
        }
    }

    override fun pause() {
        _state.update { it.copy(isPlaying = false) }
        stopProgressUpdate()
        scope.launch(Dispatchers.IO) {
            initJob.join()
            mediaPlayer?.controls()?.setPause(true)
        }
    }

    override fun stop() {
        stopProgressUpdate()
        _state.update {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                isFinished = false,
                error = null
            )
        }
        scope.launch(Dispatchers.IO) {
            initJob.join()
            mediaPlayer?.controls()?.stop()
        }
    }

    override fun seekTo(positionMs: Long) {
        _state.update { it.copy(progressMs = positionMs) }
        scope.launch(Dispatchers.IO) {
            initJob.join()
            mediaPlayer?.controls()?.setTime(positionMs)
        }
    }

    override fun setVolume(volume: Float) {
        val target = (volume * 100).toInt().coerceIn(0, 100)
        if (currentVolume == target) return
        currentVolume = target
        scope.launch(Dispatchers.IO) {
            initJob.join()
            mediaPlayer?.audio()?.setVolume(currentVolume)
        }
    }

    override fun release() {
        stopProgressUpdate()
        scope.launch(Dispatchers.IO) {
            initJob.join()
            mediaPlayer?.release()
            factory?.release()
        }
    }

    private fun buildVlcOptions(url: String): Array<String> {
        val baseOptions = arrayOf(
            ":network-caching=1500",
            ":http-reconnect=true",
            ":no-check-certificates",
            ":no-video",
            ":no-spu",
        )
        if (!url.contains("googlevideo.com")) {
            return baseOptions + arrayOf(
                ":http-user-agent=$BROWSER_UA",
                ":user-agent=$BROWSER_UA",
            )
        }
        val client = extractQueryParam(url, "c") ?: ""
        val userAgent = youTubeUserAgent(client)
        val origin =
            if (client == "ANDROID_MUSIC") "https://music.youtube.com" else "https://www.youtube.com"
        val avformatOptions = avformatOptionsForUrl(url)
        return baseOptions + avformatOptions + arrayOf(
            ":http-user-agent=$userAgent",
            ":user-agent=$userAgent",
            ":http-referrer=$origin/",
        )
    }

    private fun extractQueryParam(url: String, param: String): String? {
        return url.substringAfter("?", "")
            .split("&")
            .map { it.split("=", limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == param }
            ?.get(1)
    }

    private fun avformatOptionsForUrl(url: String): Array<String> {
        val itag = extractQueryParam(url, "itag")?.toIntOrNull()
        val format = when (itag) {
            249, 250, 251 -> "webm"
            139, 140, 141 -> "mp4"
            else -> null
        }
        return if (format != null) {
            arrayOf(":demux=avformat", ":avformat-format=$format")
        } else {
            arrayOf(":demux=avformat")
        }
    }

    private fun youTubeUserAgent(client: String): String = when (client) {
        "IOS" -> "com.google.ios.youtube/20.51.39 (iPhone16,2; U; CPU iOS 18_2 like Mac OS X;)"
        "ANDROID_VR" -> "com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/132.0.6808.3)"
        "ANDROID_MUSIC" -> "com.google.android.apps.youtube.music/7.21.50 (Linux; U; Android 14; en_US; Pixel 8 Pro; Build/UD1A.231105.004; Cronet/132.0.6808.3) gzip"
        "TVHTML5" -> "Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) AppleWebkit/605.1.15 (KHTML, like Gecko) SamsungBrowser/9.2 TV Safari/605.1.15"
        "TVHTML5_SIMPLY_EMBEDDED_PLAYER" -> "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15"
        else -> BROWSER_UA
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val time = mediaPlayer?.status()?.time() ?: 0L
                _state.update { it.copy(progressMs = time) }
                delay(200.milliseconds)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    companion object {
        private val log = Logger.getLogger("Melo.JvmMeloPlayer")
        private const val BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/605.1.15"
        private var detectedPluginsDir: File? = null

        init {
            initVlcSearchPaths()
        }

        private fun initVlcSearchPaths() {
            val resourcesDir =
                System.getProperty("compose.application.resources.dir")?.let { File(it) }
            val appDir = resourcesDir?.parentFile
            val candidates = listOfNotNull(
                resourcesDir?.resolve("vlc"),
                resourcesDir?.resolve("windows/vlc"),
                appDir?.resolve("resources/vlc"),
                appDir?.resolve("vlc"),
                File("resources/vlc"),
                File("app/resources/vlc"),
                File("vlc"),
                File("C:\\Program Files\\VideoLAN\\VLC"),
                File("C:\\Program Files (x86)\\VideoLAN\\VLC"),
            )
            for (dir in candidates) {
                if (dir.exists() && (File(dir, "libvlc.dll").exists() || File(
                        dir,
                        "libvlc.so"
                    ).exists())
                ) {
                    log.info("Found VLC libraries in: ${dir.absolutePath}")
                    try {
                        NativeLibrary.addSearchPath("libvlc", dir.absolutePath)
                        NativeLibrary.addSearchPath("libvlccore", dir.absolutePath)
                        val pluginsDir = File(dir, "plugins")
                        if (pluginsDir.exists()) {
                            detectedPluginsDir = pluginsDir
                            System.setProperty("VLC_PLUGIN_PATH", pluginsDir.absolutePath)
                        }
                    } catch (e: Throwable) {
                        log.warning("Could not set VLC search paths for ${dir.absolutePath}: ${e.message}")
                    }
                    break
                }
            }
        }
    }
}