package com.github.adriianh.cli.tui.player

import com.github.adriianh.cli.tui.MeloScreen
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
import java.io.File
import java.nio.file.Files
import java.util.UUID

/**
 * Integrates with the OS media session layer via JMTC:
 *   - Linux  → MPRIS2 over D-Bus
 *   - Windows → SystemMediaTransportControls (SMTC)
 *   - macOS  → not yet supported by JMTC
 *
 * Exposes callbacks so [AudioPlayer] / [MeloScreen] can react to
 * media-key presses from the OS or lock-screen controls.
 */
class MediaSessionManager(
    private val httpClient: HttpClient,
    private val onPlayPause: () -> Unit = {},
    private val onNext: () -> Unit = {},
    private val onPrevious: () -> Unit = {},
    private val onStop: () -> Unit = {},
) {
    private var jmtc: JMTC? = null

    @Volatile
    private var initialized = false

    private var currentArtFile: File? = null

    fun init() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            try {
                val sessionId = "melo-${UUID.randomUUID()}"
                jmtc = JMTC.getInstance(JMTCSettings(sessionId, "melo"))

                val callbacks = JMTCCallbacks()
                callbacks.onPlay = JMTCButtonCallback {
                    jmtc?.playingState = JMTCPlayingState.PLAYING
                    onPlayPause()
                }
                callbacks.onPause = JMTCButtonCallback {
                    jmtc?.playingState = JMTCPlayingState.PAUSED
                    onPlayPause()
                }
                callbacks.onStop = JMTCButtonCallback {
                    jmtc?.playingState = JMTCPlayingState.STOPPED
                    onStop()
                }
                callbacks.onNext = JMTCButtonCallback { onNext() }
                callbacks.onPrevious = JMTCButtonCallback { onPrevious() }

                jmtc?.setCallbacks(callbacks)
                jmtc?.mediaType = JMTCMediaType.Music
                jmtc?.enabledButtons = JMTCEnabledButtons(
                    /* play     */ true,
                    /* pause    */ true,
                    /* stop     */ false,
                    /* next     */ true,
                    /* previous */ true,
                )
                jmtc?.enabled = true
                jmtc?.playingState = JMTCPlayingState.STOPPED
                jmtc?.updateDisplay()

                initialized = true
            } catch (_: Throwable) { }
        }
    }

    /** Called when a new track starts playing. */
    fun updateTrack(track: Track, durationMs: Long) {
        val instance = jmtc ?: return
        try {
            val artworkFile = track.artworkUrl?.let { downloadArtworkToTempFile(it) }

            instance.mediaProperties = JMTCMusicProperties(
                /* title       */ track.title,
                /* artist      */ track.artist,
                /* albumTitle  */ track.album,
                /* albumArtist */ track.artist,
                /* genres      */ emptyArray(),
                /* albumTracks */ 0,
                /* track       */ 0,
                /* art         */ artworkFile,
            )
            instance.setTimelineProperties(
                JMTCTimelineProperties(
                    /* start     */ 0L,
                    /* end       */ durationMs * 1_000L, // JMTC uses microseconds
                    /* seekStart */ 0L,
                    /* seekEnd   */ durationMs * 1_000L,
                )
            )
            instance.playingState = JMTCPlayingState.PLAYING
            instance.updateDisplay()
        } catch (_: Exception) { }
    }

    /** Called when total duration is resolved later. */
    fun updateTimeline(durationMs: Long) {
        try {
            jmtc?.setTimelineProperties(
                JMTCTimelineProperties(
                    /* start     */ 0L,
                    /* end       */ durationMs * 1_000L,
                    /* seekStart */ 0L,
                    /* seekEnd   */ durationMs * 1_000L,
                )
            )
            jmtc?.updateDisplay()
        } catch (_: Exception) {
        }
    }

    /** Called when playback is paused. */
    fun notifyPaused() {
        try {
            jmtc?.playingState = JMTCPlayingState.PAUSED
            jmtc?.updateDisplay()
        } catch (_: Exception) { }
    }

    /** Called when playback is resumed. */
    fun notifyResumed() {
        try {
            jmtc?.playingState = JMTCPlayingState.PLAYING
            jmtc?.updateDisplay()
        } catch (_: Exception) { }
    }

    /** Called when playback position changes. [positionMs] in milliseconds. */
    fun updatePosition(positionMs: Long) {
        try {
            jmtc?.setPosition(positionMs * 1_000L) // JMTC uses microseconds
        } catch (_: Exception) { }
    }

    /** Called when playback stops entirely. */
    fun notifyStopped() {
        try {
            jmtc?.playingState = JMTCPlayingState.STOPPED
            jmtc?.updateDisplay()
        } catch (_: Exception) { }
    }

    fun destroy() {
        try {
            jmtc?.enabled = false
        } catch (_: Exception) { }
        jmtc = null
        initialized = false
        try {
            currentArtFile?.delete()
        } catch (_: Exception) { }
        currentArtFile = null
    }

    /**
     * Downloads the artwork from [url] and writes it to a new unique temp file.
     * Uses cache-busting filenames so desktop environments (MPRIS2/SMTC) reload
     * the image instead of hitting their pixbuf file path cache.
     */
    private fun downloadArtworkToTempFile(url: String): File? = try {
        val formattedUrl = if (url.startsWith("//")) "https:$url" else url
        val bytes = kotlinx.coroutines.runBlocking {
            val response = httpClient.get(formattedUrl)
            response.readRawBytes()
        }
        if (bytes.isEmpty()) null
        else {
            val tempFile =
                Files.createTempFile("melo-art-${System.currentTimeMillis()}-", ".jpg").toFile()
                    .also {
                        it.deleteOnExit()
                    }
            tempFile.writeBytes(bytes)
            val old = currentArtFile
            currentArtFile = tempFile
            try {
                old?.delete()
            } catch (_: Throwable) {
            }
            tempFile
        }
    } catch (_: Exception) {
        null
    }
}