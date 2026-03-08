package com.github.adriianh.cli.tui.player

import com.github.adriianh.cli.tui.*

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
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import java.io.File
import java.nio.file.Files

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
    private var initialized = false

    /** Single reusable temp file for JMTC artwork — overwritten on each track change. */
    private val artworkTempFile: File by lazy {
        Files.createTempFile("melo-art-", ".jpg").toFile().also { it.deleteOnExit() }
    }

    fun init() {
        if (initialized) return
        try {
            jmtc = JMTC.getInstance(JMTCSettings("melo", "melo"))

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
        } catch (_: Exception) { }
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
        // Clean up temp file
        try { artworkTempFile.delete() } catch (_: Exception) {}
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Downloads the artwork from [url] and writes it to the single reusable temp file.
     * JMTC expects a [File] rather than raw bytes for the album art.
     *
     * Uses the shared Ktor [HttpClient] instead of creating a new java.net.http.HttpClient
     * per call (which would spin up a dedicated thread pool each time).
     */
    private fun downloadArtworkToTempFile(url: String): File? = try {
        val bytes = kotlinx.coroutines.runBlocking {
            httpClient.get(url).body<ByteArray>()
        }
        artworkTempFile.writeBytes(bytes)
        artworkTempFile
    } catch (_: Exception) {
        null
    }
}
