package com.github.adriianh.cli.tui.player

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
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
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
    private val onPlayPause: () -> Unit = {},
    private val onNext: () -> Unit = {},
    private val onPrevious: () -> Unit = {},
    private val onStop: () -> Unit = {},
) {
    private var jmtc: JMTC? = null
    private var initialized = false

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
        } catch (_: Exception) {}
    }

    /** Called when playback is paused. */
    fun notifyPaused() {
        try {
            jmtc?.playingState = JMTCPlayingState.PAUSED
            jmtc?.updateDisplay()
        } catch (_: Exception) {}
    }

    /** Called when playback is resumed. */
    fun notifyResumed() {
        try {
            jmtc?.playingState = JMTCPlayingState.PLAYING
            jmtc?.updateDisplay()
        } catch (_: Exception) {}
    }

    /** Called when playback position changes. [positionMs] in milliseconds. */
    fun updatePosition(positionMs: Long) {
        try {
            jmtc?.setPosition(positionMs * 1_000L) // JMTC uses microseconds
        } catch (_: Exception) {}
    }

    /** Called when playback stops entirely. */
    fun notifyStopped() {
        try {
            jmtc?.playingState = JMTCPlayingState.STOPPED
            jmtc?.updateDisplay()
        } catch (_: Exception) {}
    }

    fun destroy() {
        try {
            jmtc?.enabled = false
        } catch (_: Exception) {}
        jmtc = null
        initialized = false
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Downloads the artwork from [url] and writes it to a temp file.
     * JMTC expects a [File] rather than raw bytes for the album art.
     * Returns null if the download fails.
     */
    private fun downloadArtworkToTempFile(url: String): File? = try {
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
        if (response.statusCode() == 200) {
            val tmp = Files.createTempFile("melo-art-", ".jpg").toFile()
            tmp.deleteOnExit()
            tmp.writeBytes(response.body())
            tmp
        } else null
    } catch (_: Exception) {
        null
    }
}
