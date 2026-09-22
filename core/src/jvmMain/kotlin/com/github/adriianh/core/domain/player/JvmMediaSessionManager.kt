package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track
import io.github.selemba1000.JMTC
import io.github.selemba1000.JMTCButtonCallback
import io.github.selemba1000.JMTCCallbacks
import io.github.selemba1000.JMTCEnabledButtons
import io.github.selemba1000.JMTCMediaType
import io.github.selemba1000.JMTCMusicProperties
import io.github.selemba1000.JMTCPlayingState
import io.github.selemba1000.JMTCSettings
import io.github.selemba1000.JMTCTimelineProperties
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.util.UUID

/**
 * Shared JVM (Desktop) OS media-session integration via JMTC:
 *   - Linux  → MPRIS2 over D-Bus
 *   - Windows → SystemMediaTransportControls (SMTC)
 *   - macOS  → not yet supported by JMTC (gracefully no-ops)
 *
 * Used by both Melo UIs on Desktop:
 *  - **Reactive mode** (Compose app): constructed with a [PlaybackManager];
 *    now-playing metadata, timeline and button availability are derived from
 *    its `playbackState`/`queueState` flows, and OS media keys drive it.
 *  - **Imperative mode** (TUI): constructed with control callbacks; the app
 *    pushes track/position/state changes via [updateTrack], [updatePosition],
 *    [notifyPaused], [notifyResumed] and [notifyStopped].
 */
class JvmMediaSessionManager(
    private val httpClient: HttpClient,
    onPlayPause: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onStop: () -> Unit = {},
    private val playbackManager: PlaybackManager? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : MediaSessionManager {

    private var jmtc: JMTC? = null

    @Volatile
    private var initialized = false

    private var observerJob: Job? = null
    private var artworkJob: Job? = null

    @Volatile
    private var currentTrackId: String? = null

    @Volatile
    private var currentDurationMs: Long = 0L

    @Volatile
    private var currentArtFile: File? = null
    private val isLinux = System.getProperty("os.name")?.lowercase()?.contains("linux") == true

    private val onTogglePlayPause: () -> Unit =
        if (playbackManager != null) ({ playbackManager.togglePlayPause() }) else onPlayPause
    private val onPlayNext: () -> Unit =
        if (playbackManager != null) ({ playbackManager.playNext() }) else onNext
    private val onPlayPrevious: () -> Unit =
        if (playbackManager != null) ({ playbackManager.playPrevious() }) else onPrevious
    private val onStopPlayback: () -> Unit =
        if (playbackManager != null) ({ playbackManager.release() }) else onStop

    private fun toMediaSessionTime(timeMs: Long): Long = if (isLinux) timeMs * 1_000L else timeMs

    override fun init() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            try {
                val isWindows = System.getProperty("os.name")?.lowercase()?.contains("win") == true
                if (isWindows) {
                    val localAppData = System.getenv("LOCALAPPDATA")
                    val existing = System.getProperty("jna.library.path") ?: ""
                    val extraPaths = listOfNotNull(
                        localAppData?.let { "$it\\melo-tui" },
                        localAppData?.let { "$it\\melo-tui\\bin" },
                        File(".").absolutePath
                    )
                    val combined = (listOf(existing).filter { it.isNotBlank() } + extraPaths).joinToString(File.pathSeparator)
                    System.setProperty("jna.library.path", combined)
                }

                val sessionId = "melo-${UUID.randomUUID()}"
                val instance = JMTC.getInstance(JMTCSettings(sessionId, "Melo"))
                jmtc = instance

                val callbacks = JMTCCallbacks()
                callbacks.onPlay = JMTCButtonCallback {
                    jmtc?.playingState = JMTCPlayingState.PLAYING
                    onTogglePlayPause()
                }
                callbacks.onPause = JMTCButtonCallback {
                    jmtc?.playingState = JMTCPlayingState.PAUSED
                    onTogglePlayPause()
                }
                callbacks.onStop = JMTCButtonCallback {
                    jmtc?.playingState = JMTCPlayingState.STOPPED
                    onStopPlayback()
                }
                callbacks.onNext = JMTCButtonCallback { onPlayNext() }
                callbacks.onPrevious = JMTCButtonCallback { onPlayPrevious() }

                instance.setCallbacks(callbacks)
                instance.mediaType = JMTCMediaType.Music
                instance.enabledButtons = JMTCEnabledButtons(
                    /* play     */ true,
                    /* pause    */ true,
                    /* stop     */ false,
                    /* next     */ true,
                    /* previous */ true
                )
                instance.enabled = true
                instance.playingState = JMTCPlayingState.STOPPED
                instance.updateDisplay()

                if (playbackManager != null && observerJob == null) {
                    startObservers()
                }
                initialized = true
            } catch (e: Throwable) {
                if (System.getProperty("melo.debug") == "true" || System.getenv("MELO_DEBUG") == "1") {
                    System.err.println("[Melo] Failed to initialize MediaSession (SMTC/MPRIS): ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startObservers() {
        val pm = playbackManager ?: return
        observerJob = scope.launch {
            launch {
                pm.queueState.collect { queue ->
                    val instance = jmtc ?: return@collect
                    try {
                        val hasNext = queue.currentIndex < queue.tracks.lastIndex
                        val hasPrev = queue.currentIndex > 0
                        instance.enabledButtons = JMTCEnabledButtons(
                            /* play     */ true,
                            /* pause    */ true,
                            /* stop     */ false,
                            /* next     */ hasNext,
                            /* previous */ hasPrev
                        )
                    } catch (_: Throwable) {
                    }
                }
            }

            launch {
                var lastPlaying: Boolean? = null
                pm.playbackState.collect { state ->
                    val instance = jmtc ?: return@collect
                    try {
                        val track = state.currentTrack
                        if (track == null) {
                            if (currentTrackId != null) {
                                currentTrackId = null
                                currentDurationMs = 0L
                                lastPlaying = null
                                cleanupCurrentArt()
                                instance.playingState = JMTCPlayingState.STOPPED
                                instance.updateDisplay()
                            }
                            return@collect
                        }

                        val effectiveDurationMs = when {
                            state.durationMs > 0 -> state.durationMs
                            track.durationMs > 0 -> track.durationMs
                            else -> 0L
                        }

                        val trackChanged = track.id != currentTrackId
                        if (trackChanged) {
                            currentTrackId = track.id
                            currentDurationMs = effectiveDurationMs
                            updateTrack(track, effectiveDurationMs)
                        } else if (effectiveDurationMs > 0 && effectiveDurationMs != currentDurationMs) {
                            currentDurationMs = effectiveDurationMs
                            updateTimeline(effectiveDurationMs)
                        }

                        if (state.isPlaying != lastPlaying) {
                            lastPlaying = state.isPlaying
                            instance.playingState = if (state.isPlaying) {
                                JMTCPlayingState.PLAYING
                            } else {
                                JMTCPlayingState.PAUSED
                            }
                            instance.updateDisplay()
                        }

                        if (state.progressMs >= 0) {
                            instance.setPosition(toMediaSessionTime(state.progressMs))
                        }
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }

    /** Called when a new track starts playing (or its rich metadata resolves). */
    fun updateTrack(track: Track, durationMs: Long) {
        val instance = jmtc ?: return
        val effectiveDurationMs = if (durationMs > 0) durationMs else track.durationMs
        currentTrackId = track.id
        currentDurationMs = effectiveDurationMs
        try {
            cleanupCurrentArt()
            instance.mediaProperties = JMTCMusicProperties(
                /* title       */ track.title,
                /* artist      */ track.artist,
                /* albumTitle  */ track.album,
                /* albumArtist */ track.artist,
                /* genres      */ emptyArray(),
                /* albumTracks */ 0,
                /* track       */ 0,
                /* art         */ null
            )
            updateTimeline(effectiveDurationMs)
            instance.playingState = JMTCPlayingState.PLAYING
            instance.updateDisplay()

            // Asynchronously download and apply album artwork using cache-busting unique temp files
            artworkJob?.cancel()
            val artworkUrl = track.artworkUrl
            if (!artworkUrl.isNullOrBlank()) {
                artworkJob = scope.launch(Dispatchers.IO) {
                    val file = downloadArtwork(artworkUrl)
                    if (file != null && currentTrackId == track.id) {
                        try {
                            val oldFile = currentArtFile
                            currentArtFile = file
                            instance.mediaProperties = JMTCMusicProperties(
                                /* title       */ track.title,
                                /* artist      */ track.artist,
                                /* albumTitle  */ track.album,
                                /* albumArtist */ track.artist,
                                /* genres      */ emptyArray(),
                                /* albumTracks */ 0,
                                /* track       */ 0,
                                /* art         */ file
                            )
                            instance.updateDisplay()
                            try {
                                oldFile?.delete()
                            } catch (_: Throwable) {
                            }
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    /** Called when the total duration is resolved later. */
    fun updateTimeline(durationMs: Long) {
        val instance = jmtc ?: return
        try {
            instance.setTimelineProperties(
                JMTCTimelineProperties(
                    /* start     */ 0L,
                    /* end       */ toMediaSessionTime(durationMs),
                    /* seekStart */ 0L,
                    /* seekEnd   */ toMediaSessionTime(durationMs)
                )
            )
            instance.updateDisplay()
        } catch (_: Throwable) {
        }
    }

    /** Called when playback position changes. [positionMs] in milliseconds. */
    fun updatePosition(positionMs: Long) {
        try {
            jmtc?.setPosition(toMediaSessionTime(positionMs))
        } catch (_: Throwable) {
        }
    }

    /** Called when playback is paused. */
    fun notifyPaused() {
        try {
            jmtc?.playingState = JMTCPlayingState.PAUSED
            jmtc?.updateDisplay()
        } catch (_: Throwable) {
        }
    }

    /** Called when playback is resumed. */
    fun notifyResumed() {
        try {
            jmtc?.playingState = JMTCPlayingState.PLAYING
            jmtc?.updateDisplay()
        } catch (_: Throwable) {
        }
    }

    /** Called when playback stops entirely. */
    fun notifyStopped() {
        try {
            jmtc?.playingState = JMTCPlayingState.STOPPED
            jmtc?.updateDisplay()
        } catch (_: Throwable) {
        }
    }

    /** Legacy alias kept for the TUI/CLI call sites that use [release]. */
    fun destroy() = release()

    override fun release() {
        observerJob?.cancel()
        observerJob = null
        artworkJob?.cancel()
        cleanupCurrentArt()
        try {
            jmtc?.enabled = false
        } catch (_: Throwable) {
        }
        jmtc = null
        initialized = false
        currentTrackId = null
        currentDurationMs = 0L
    }

    private suspend fun downloadArtwork(url: String): File? = try {
        val formattedUrl = if (url.startsWith("//")) "https:$url" else url
        val bytes = httpClient.get(formattedUrl).readRawBytes()
        if (bytes.isEmpty()) null
        else {
            val tempFile =
                withContext(Dispatchers.IO) {
                    Files.createTempFile("melo-art-${System.currentTimeMillis()}-", ".jpg")
                }.toFile()
                    .also {
                        it.deleteOnExit()
                    }
            tempFile.writeBytes(bytes)
            tempFile
        }
    } catch (_: Throwable) {
        null
    }

    private fun cleanupCurrentArt() {
        try {
            currentArtFile?.delete()
        } catch (_: Throwable) {
        }
        currentArtFile = null
    }
}