package com.github.adriianh.cli.tui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Audio player that streams audio via ffmpeg → PulseAudio/PipeWire.
 *
 * Using PulseAudio as the output sink means ffmpeg participates in the normal
 * system audio mixer — no exclusive device access, no conflicts with other apps.
 *
 * Pause  : SIGSTOP / SIGCONT sent to the ffmpeg process (Linux/macOS).
 * Volume : softvol filter passed to ffmpeg at play time; re-starts playback
 *          from the current position when changed (acceptable UX trade-off).
 *
 * Requires: ffmpeg with PulseAudio support (ffmpeg -f pulse).
 */
class AudioPlayer(
    private val scope: CoroutineScope,
    private val onProgress: (elapsedMs: Long) -> Unit = {},
    private val onFinish: () -> Unit = {},
    private val onError: (Throwable) -> Unit = {},
) {
    private var playJob: Job? = null
    private var ffmpegProcess: Process? = null
    private var ffmpegPid: Long? = null

    private val isPaused = AtomicBoolean(false)
    private val isStopped = AtomicBoolean(false)

    @Volatile private var startTimeMs: Long = 0L
    @Volatile private var pausedAtMs: Long = 0L
    @Volatile private var volumePct: Int = 75

    @Volatile private var currentUrl: String? = null

    val isPlaying: Boolean get() = playJob?.isActive == true && !isPaused.get()

    // ── Public API ─────────────────────────────────────────────────────────────

    fun play(url: String) {
        stop()
        currentUrl = url
        isPaused.set(false)
        isStopped.set(false)
        startTimeMs = System.currentTimeMillis()
        pausedAtMs = 0L
        launchPlayback(url, volumePct)
    }

    fun pause() {
        if (isPaused.get() || !isPlaying) return
        isPaused.set(true)
        pausedAtMs += System.currentTimeMillis() - startTimeMs
        sendSignal("SIGSTOP")
    }

    fun resume() {
        if (!isPaused.get()) return
        startTimeMs = System.currentTimeMillis()
        isPaused.set(false)
        sendSignal("SIGCONT")
    }

    /** Volume 0–100. Takes effect immediately via a softvol filter restart. */
    fun setVolume(pct: Int) {
        volumePct = pct.coerceIn(0, 100)
        val url = currentUrl ?: return
        if (playJob?.isActive == true) {
            val elapsed = if (isPaused.get()) pausedAtMs else pausedAtMs + (System.currentTimeMillis() - startTimeMs)
            restartWithVolume(url, volumePct, elapsed)
        }
    }

    fun stop() {
        isStopped.set(true)
        isPaused.set(false)
        ffmpegProcess?.destroy()
        ffmpegProcess = null
        ffmpegPid = null
        playJob?.cancel()
        playJob = null
        currentUrl = null
        pausedAtMs = 0L
    }

    fun togglePlayPause() {
        when {
            isPaused.get() -> resume()
            isPlaying      -> pause()
        }
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private fun launchPlayback(url: String, volPct: Int, seekMs: Long = 0L) {
        playJob = scope.launch(Dispatchers.IO) {
            try {
                val process = buildFfmpegProcess(url, volPct, seekMs)
                ffmpegProcess = process
                ffmpegPid = process.pid()

                startTimeMs = System.currentTimeMillis()

                val progressJob = scope.launch(Dispatchers.IO) {
                    while (isActive && !isStopped.get()) {
                        if (!isPaused.get()) {
                            val elapsed = pausedAtMs + (System.currentTimeMillis() - startTimeMs)
                            onProgress(elapsed + seekMs)
                        }
                        delay(500)
                    }
                }

                process.waitFor()
                progressJob.cancel()

                if (!isStopped.get()) onFinish()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isStopped.get()) onError(e)
            }
        }
    }

    private fun restartWithVolume(url: String, volPct: Int, elapsedMs: Long) {
        isStopped.set(false)
        ffmpegProcess?.destroy()
        ffmpegProcess = null
        ffmpegPid = null
        playJob?.cancel()
        playJob = null
        isPaused.set(false)
        pausedAtMs = elapsedMs
        startTimeMs = System.currentTimeMillis()
        launchPlayback(url, volPct, elapsedMs)
    }

    private fun sendSignal(signal: String) {
        val pid = ffmpegPid ?: return
        try {
            ProcessBuilder("kill", "-$signal", pid.toString())
                .redirectErrorStream(true)
                .start()
                .waitFor()
        } catch (_: Exception) { /* best-effort */ }
    }

    private fun buildFfmpegProcess(url: String, volPct: Int, seekMs: Long): Process {
        val volume = volPct / 100.0

        val cmd = mutableListOf(
            ffmpegBinary(),
            "-reconnect", "1",
            "-reconnect_streamed", "1",
            "-reconnect_delay_max", "5",
        )

        if (seekMs > 0) {
            cmd += listOf("-ss", (seekMs / 1000.0).toString())
        }

        cmd += listOf(
            "-i", url,
            "-af", "volume=$volume",
            "-vn",
            "-f", "pulse",
            "-name", "Melo",
            "default"
        )

        return ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
    }

    private fun ffmpegBinary(): String {
        val candidates = listOf("ffmpeg", "/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg")
        for (bin in candidates) {
            try {
                val p = ProcessBuilder(bin, "-version").redirectErrorStream(true).start()
                p.waitFor()
                if (p.exitValue() == 0) return bin
            } catch (_: Exception) {
                continue
            }
        }
        return "ffmpeg"
    }
}
