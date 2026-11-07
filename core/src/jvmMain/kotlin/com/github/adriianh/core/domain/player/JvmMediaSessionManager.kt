package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.platform.MeloDataDirectory
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
import java.io.RandomAccessFile
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files

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
    /**
     * Stable MPRIS identity for this instance, e.g. `"Melo"` for the GUI or `"Melo.tui"`
     * for the TUI. Only used on Linux; Windows (SMTC) and macOS ignore it.
     */
    private val sessionName: String = "Melo",
) : MediaSessionManager {

    private var jmtc: JMTC? = null

    /** Held while this process owns the MPRIS bus name for [sessionName]. Linux only. */
    private var sessionLockFile: RandomAccessFile? = null
    private var sessionLock: FileLock? = null

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

                // JMTC fixes the D-Bus name and the MPRIS Identity to playerName in its
                // constructor, and JMTC.getInstance is a process-wide singleton, so the
                // name is decided once per JVM. Instances that cannot take the canonical
                // name fall back to the MPRIS "<name>.instance<PID>" convention.
                val playerName = resolvePlayerName()
                val instance = JMTC.getInstance(JMTCSettings(playerName, "melo"))
                jmtc = instance

                val callbacks = JMTCCallbacks()
                callbacks.onPlay = JMTCButtonCallback {
                    scope.launch {
                        try {
                            onTogglePlayPause()
                        } catch (_: Throwable) {
                        }
                    }
                }
                callbacks.onPause = JMTCButtonCallback {
                    scope.launch {
                        try {
                            onTogglePlayPause()
                        } catch (_: Throwable) {
                        }
                    }
                }
                callbacks.onStop = JMTCButtonCallback {
                    scope.launch {
                        try {
                            onStopPlayback()
                        } catch (_: Throwable) {
                        }
                    }
                }
                callbacks.onNext = JMTCButtonCallback {
                    scope.launch {
                        try {
                            onPlayNext()
                        } catch (_: Throwable) {
                        }
                    }
                }
                callbacks.onPrevious = JMTCButtonCallback {
                    scope.launch {
                        try {
                            onPlayPrevious()
                        } catch (_: Throwable) {
                        }
                    }
                }

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
                // A failed init must not keep the bus name reserved for the process lifetime.
                releaseSessionLock()
                if (System.getProperty("melo.debug") == "true" || System.getenv("MELO_DEBUG") == "1") {
                    System.err.println("[Melo] Failed to initialize MediaSession (SMTC/MPRIS): ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Resolves the MPRIS player name for this process and takes the matching lock.
     *
     * The first instance of a given [sessionName] (GUI vs TUI) owns the clean name; any
     * further instance of the same role gets the MPRIS `"<name>.instance<PID>"` suffix so
     * it stays individually controllable instead of orphaning the previous one.
     *
     * Non-Linux platforms have no bus name to contend for, so [sessionName] is returned
     * untouched and no lock file is created.
     */
    private fun resolvePlayerName(): String {
        if (!isLinux) return sessionName
        return try {
            val lockDir = File(MeloDataDirectory.resolve(), "locks")
            if (!lockDir.exists()) lockDir.mkdirs()
            val lockFile = File(lockDir, "$sessionName.lock")
            val raf = RandomAccessFile(lockFile, "rw")
            val lock =
                try {
                    raf.channel.tryLock()
                } catch (_: OverlappingFileLockException) {
                    // Another manager in this same JVM already holds it.
                    null
                }
            if (lock == null) {
                raf.close()
                "$sessionName.instance${ProcessHandle.current().pid()}"
            } else {
                sessionLockFile = raf
                sessionLock = lock
                sessionName
            }
        } catch (_: Throwable) {
            // Never let lock trouble stop the media session from coming up.
            "$sessionName.instance${ProcessHandle.current().pid()}"
        }
    }

    /**
     * Releases the MPRIS bus name lock. Must run *after* `enabled = false`, because JMTC
     * requests the bus name with REPLACE_EXISTING and a successor would otherwise steal
     * the name from a still-live owner.
     *
     * The lock file itself is intentionally left in place: unlinking it would let a second
     * process lock an orphaned inode while a third locks the fresh file.
     */
    private fun releaseSessionLock() {
        try {
            sessionLock?.release()
        } catch (_: Throwable) {
        }
        try {
            sessionLockFile?.close()
        } catch (_: Throwable) {
        }
        sessionLock = null
        sessionLockFile = null
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
        // Only after the bus name is released, so the next instance can take it cleanly.
        releaseSessionLock()
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