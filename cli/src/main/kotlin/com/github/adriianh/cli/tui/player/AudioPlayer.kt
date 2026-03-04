package com.github.adriianh.cli.tui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Audio player backed by ffplay (bundled with ffmpeg).
 *
 * ffplay uses SDL2 for audio output, which auto-selects the best backend per OS:
 *   Linux  → PipeWire / PulseAudio / ALSA  (no exclusive access, mixes with other apps)
 *   macOS  → CoreAudio
 *   Windows → DirectSound / WASAPI
 *
 * Pause on Linux/macOS : SIGSTOP / SIGCONT — instantaneous, no buffer delay.
 * Pause on Windows     : restart from last known position (POSIX signals unavailable).
 *
 * Volume : softvol filter via -af volume=N; restarts from current position on change.
 */
class AudioPlayer(
    private val scope: CoroutineScope,
    private val onProgress: (elapsedMs: Long) -> Unit = {},
    private val onFinish: () -> Unit = {},
    private val onError: (Throwable) -> Unit = {},
) {
    private var playJob: Job? = null
    private var playerProcess: Process? = null
    private var playerPid: Long? = null

    private val isPaused = AtomicBoolean(false)
    private val isStopped = AtomicBoolean(false)

    @Volatile private var startTimeMs: Long = 0L
    @Volatile private var pausedAtMs: Long = 0L
    @Volatile private var volumePct: Int = 75
    @Volatile private var currentUrl: String? = null

    val isPlaying: Boolean get() = playJob?.isActive == true && !isPaused.get()

    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    // ── Public API ─────────────────────────────────────────────────────────────

    fun play(url: String) {
        stop()
        currentUrl = url
        isPaused.set(false)
        isStopped.set(false)
        startTimeMs = System.currentTimeMillis()
        pausedAtMs = 0L
        launchPlayback(url, volumePct, seekMs = 0L)
    }

    fun pause() {
        if (isPaused.get() || !isPlaying) return
        isPaused.set(true)
        pausedAtMs += System.currentTimeMillis() - startTimeMs
        if (isWindows) {
            suspendProcessWindows(playerPid ?: return)
        } else {
            sendUnixSignal("SIGSTOP")
        }
    }

    fun resume() {
        if (!isPaused.get()) return
        startTimeMs = System.currentTimeMillis()
        isPaused.set(false)
        if (isWindows) {
            resumeProcessWindows(playerPid ?: return)
        } else {
            sendUnixSignal("SIGCONT")
        }
    }

    /** Volume 0–100. Restarts ffplay from current position with new softvol. */
    fun setVolume(pct: Int) {
        volumePct = pct.coerceIn(0, 100)
        val url = currentUrl ?: return
        if (playJob?.isActive == true) {
            val elapsed = if (isPaused.get()) pausedAtMs
                          else pausedAtMs + (System.currentTimeMillis() - startTimeMs)
            restartAt(url, volumePct, elapsed)
        }
    }

    fun stop() {
        isStopped.set(true)
        isPaused.set(false)
        playerProcess?.destroy()
        playerProcess = null
        playerPid = null
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

    private fun launchPlayback(url: String, volPct: Int, seekMs: Long) {
        playJob = scope.launch(Dispatchers.IO) {
            try {
                val process = buildFfplayProcess(url, volPct, seekMs)
                playerProcess = process
                playerPid = process.pid()
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

    private fun restartAt(url: String, volPct: Int, elapsedMs: Long) {
        isStopped.set(false)
        isPaused.set(false)
        playerProcess?.destroy()
        playerProcess = null
        playerPid = null
        playJob?.cancel()
        playJob = null
        pausedAtMs = elapsedMs
        startTimeMs = System.currentTimeMillis()
        launchPlayback(url, volPct, elapsedMs)
    }

    // ── Process builders ───────────────────────────────────────────────────────

    private fun buildFfplayProcess(url: String, volPct: Int, seekMs: Long): Process {
        val volume = volPct

        val cmd = mutableListOf(
            ffplayBinary(),
            "-nodisp",
            "-autoexit",
            "-loglevel", "quiet",
            "-af", "volume=${volume / 100.0}",
        )

        if (seekMs > 0) {
            cmd += listOf("-ss", (seekMs / 1000.0).toString())
        }

        cmd += listOf(
            "-reconnect", "1",
            "-reconnect_streamed", "1",
            "-reconnect_delay_max", "5",
            "-i", url,
        )

        return ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .start()
    }

    // ── Signal helpers ─────────────────────────────────────────────────────────

    private fun sendUnixSignal(signal: String) {
        val pid = playerPid ?: return
        try {
            ProcessBuilder("kill", "-$signal", pid.toString())
                .redirectErrorStream(true)
                .start()
                .waitFor()
        } catch (_: Exception) { /* best-effort */ }
    }

    /**
     * Windows: suspend all threads via NtSuspendProcess through a small PowerShell one-liner.
     * This is the closest equivalent to SIGSTOP on Windows.
     */
    private fun suspendProcessWindows(pid: Long) {
        try {
            ProcessBuilder(
                "powershell", "-Command",
                "\$proc = Get-Process -Id $pid -ErrorAction SilentlyContinue; " +
                "if (\$proc) { \$proc.Suspend() }"
            ).redirectErrorStream(true).start().waitFor()
        } catch (_: Exception) { /* best-effort */ }
    }

    private fun resumeProcessWindows(pid: Long) {
        try {
            ProcessBuilder(
                "powershell", "-Command",
                "\$proc = [System.Diagnostics.Process]::GetProcessById($pid); " +
                "if (\$proc) { \$proc.Resume() }"
            ).redirectErrorStream(true).start().waitFor()
        } catch (_: Exception) { /* best-effort */ }
    }

    // ── Binary resolution ──────────────────────────────────────────────────────

    private fun ffplayBinary(): String {
        val name = if (isWindows) "ffplay.exe" else "ffplay"
        val candidates = listOf(
            name,
            "/usr/bin/ffplay",
            "/usr/local/bin/ffplay",
            "/opt/homebrew/bin/ffplay",
            "C:\\ffmpeg\\bin\\ffplay.exe",
        )
        for (bin in candidates) {
            try {
                val p = ProcessBuilder(bin, "-version").redirectErrorStream(true).start()
                p.waitFor()
                if (p.exitValue() == 0) return bin
            } catch (_: Exception) {
                continue
            }
        }
        return name
    }
}
