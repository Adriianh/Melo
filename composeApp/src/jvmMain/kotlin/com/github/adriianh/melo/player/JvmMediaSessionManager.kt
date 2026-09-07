package com.github.adriianh.melo.player

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.MediaSessionManager
import com.github.adriianh.core.domain.player.PlaybackManager
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
 * Integrates Melo with the Desktop OS media session layer via JMTC:
 *   - Linux → MPRIS2 over D-Bus
 *   - Windows → SystemMediaTransportControls (SMTC)
 *   - macOS → not yet supported by JMTC (gracefully no-ops)
 *
 * Bridges the OS media keys / notification widgets directly to [PlaybackManager].
 */
class JvmMediaSessionManager(
    private val playbackManager: PlaybackManager,
    private val httpClient: HttpClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : MediaSessionManager {

    private var jmtc: JMTC? = null

    @Volatile
    private var initialized = false

    private var observerJob: Job? = null
    private var artworkJob: Job? = null
    private var currentTrackId: String? = null
    private var currentDurationMs: Long = 0L
    private var currentArtFile: File? = null
    private val isLinux = System.getProperty("os.name")?.lowercase()?.contains("linux") == true

    private fun toMediaSessionTime(timeMs: Long): Long = if (isLinux) timeMs * 1_000L else timeMs

    override fun init() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            try {
                val sessionId = "melo-${UUID.randomUUID()}"
                val instance = JMTC.getInstance(JMTCSettings(sessionId, "Melo"))
                jmtc = instance

                val callbacks = JMTCCallbacks()
                callbacks.onPlay = JMTCButtonCallback { playbackManager.togglePlayPause() }
                callbacks.onPause = JMTCButtonCallback { playbackManager.togglePlayPause() }
                callbacks.onStop = JMTCButtonCallback { playbackManager.release() }
                callbacks.onNext = JMTCButtonCallback { playbackManager.playNext() }
                callbacks.onPrevious = JMTCButtonCallback { playbackManager.playPrevious() }

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

                startObservers()
                initialized = true
            } catch (_: Throwable) {
            }
        }
    }

    private fun startObservers() {
        observerJob?.cancel()
        observerJob = scope.launch {
            launch {
                playbackManager.queueState.collect { queue ->
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
                playbackManager.playbackState.collect { state ->
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
                            updateTrack(instance, track, effectiveDurationMs)
                        } else if (effectiveDurationMs > 0 && effectiveDurationMs != currentDurationMs) {
                            currentDurationMs = effectiveDurationMs
                            instance.setTimelineProperties(
                                JMTCTimelineProperties(
                                    /* start     */ 0L,
                                    /* end       */ toMediaSessionTime(effectiveDurationMs),
                                    /* seekStart */ 0L,
                                    /* seekEnd   */ toMediaSessionTime(effectiveDurationMs)
                                )
                            )
                            instance.updateDisplay()
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

    private fun updateTrack(instance: JMTC, track: Track, durationMs: Long) {
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
            instance.setTimelineProperties(
                JMTCTimelineProperties(
                    /* start     */ 0L,
                    /* end       */ toMediaSessionTime(durationMs),
                    /* seekStart */ 0L,
                    /* seekEnd   */ toMediaSessionTime(durationMs)
                )
            )
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

    override fun release() {
        observerJob?.cancel()
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
}
