package com.github.adriianh.cli.tui.player

import com.github.adriianh.cli.tui.*

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Audio player backed by ffplay (bundled with ffmpeg).
 *
 * ffplay uses SDL2 for audio output, which auto-selects the best backend per OS:
 *   Linux  → PipeWire / PulseAudio / ALSA
 *   macOS  → CoreAudio
 *   Windows → DirectSound / WASAPI
 *
 * Pause  : SIGSTOP / SIGCONT — instantaneous, no buffer delay.
 * Volume : pactl set-sink-input-volume on Linux/PulseAudio (no interruption);
 *          fallback: stored and applied on next play() via -af volume= filter.
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

    private val sessionId = AtomicLong(0L)

    @Volatile private var startTimeMs: Long = 0L
    @Volatile private var pausedAtMs: Long = 0L
    @Volatile private var volumePct: Int = 75
    @Volatile private var currentUrl: String? = null

    val isPlaying: Boolean get() = playJob?.isActive == true && !isPaused.get()

    private val isWindows = System.getProperty("os.name").lowercase().contains("win")
    private val hasPactl: Boolean by lazy {
        try {
            ProcessBuilder("pactl", "--version").redirectErrorStream(true).start().waitFor() == 0
        } catch (_: Exception) { false }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    fun play(url: String) {
        stop()
        currentUrl = url
        isPaused.set(false)
        startTimeMs = System.currentTimeMillis()
        pausedAtMs = 0L
        val session = sessionId.incrementAndGet()
        launchPlayback(url, volumePct, seekMs = 0L, session = session)
    }

    fun pause() {
        if (isPaused.get() || !isPlaying) return
        isPaused.set(true)
        pausedAtMs += System.currentTimeMillis() - startTimeMs
        if (isWindows) suspendProcessWindows(playerPid ?: return)
        else           sendUnixSignal("SIGSTOP")
    }

    fun resume() {
        if (!isPaused.get()) return
        startTimeMs = System.currentTimeMillis()
        isPaused.set(false)
        if (isWindows) resumeProcessWindows(playerPid ?: return)
        else           sendUnixSignal("SIGCONT")
    }

    /**
     * Set volume 0–100 without interrupting playback.
     *
     * On Linux with PulseAudio/PipeWire: uses `pactl set-sink-input-volume` to
     * adjust the volume of the ffplay stream in real time — zero interruption.
     *
     * On other platforms: stores the value; applied on the next play() call.
     */
    fun setVolume(pct: Int) {
        volumePct = pct.coerceIn(0, 100)
        if (hasPactl) {
            applyVolumeViaPactl(playerPid ?: return, volumePct)
        }
    }

    /**
     * Seek to [ms] milliseconds from the start of the current stream.
     * Restarts ffplay with -ss, preserving volume and paused state.
     */
    fun seek(ms: Long) {
        val url = currentUrl ?: return
        val clampedMs = ms.coerceAtLeast(0L)
        val wasPaused = isPaused.get()

        playerProcess?.destroy()
        playerProcess = null
        playerPid = null
        playJob?.cancel()
        playJob = null
        isPaused.set(false)

        pausedAtMs = if (wasPaused) clampedMs else 0L
        startTimeMs = System.currentTimeMillis()

        val session = sessionId.incrementAndGet()
        if (wasPaused) isPaused.set(true)

        launchPlayback(url, volumePct, seekMs = clampedMs, session = session)

        if (wasPaused) {
            scope.launch(Dispatchers.IO) {
                delay(300)
                if (isPaused.get() && sessionId.get() == session) {
                    if (isWindows) suspendProcessWindows(playerPid ?: return@launch)
                    else sendUnixSignal("SIGSTOP")
                }
            }
        }
    }
    fun stop() {
        sessionId.incrementAndGet() // invalidate any running job
        isPaused.set(false)
        playerProcess?.destroy()
        playerProcess = null
        playerPid = null
        playJob?.cancel()
        playJob = null
        currentUrl = null
        pausedAtMs = 0L
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private fun launchPlayback(url: String, volPct: Int, seekMs: Long, session: Long) {
        playJob = scope.launch(Dispatchers.IO) {
            try {
                val process = buildFfplayProcess(url, volPct, seekMs)
                playerProcess = process
                playerPid = process.pid()
                startTimeMs = System.currentTimeMillis()

                val progressJob = scope.launch(Dispatchers.IO) {
                    while (isActive && sessionId.get() == session) {
                        if (!isPaused.get()) {
                            val elapsed = pausedAtMs + (System.currentTimeMillis() - startTimeMs)
                            onProgress(elapsed + seekMs)
                        }
                        delay(500)
                    }
                }

                process.waitFor()
                progressJob.cancel()

                if (sessionId.get() == session) onFinish()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (sessionId.get() == session) onError(e)
            }
        }
    }

    /**
     * Adjusts the volume of the ffplay sink-input via pactl without interrupting playback.
     *
     * Two-step lookup because SDL (used by ffplay) registers its sink-input under a
     * generic "SDL Application" name without a direct process.id property:
     *   1. `pactl list clients`     → find the client ID whose process.id matches [pid]
     *   2. `pactl list sink-inputs` → find the sink-input whose client.id matches step 1
     *   3. `pactl set-sink-input-volume <index> <pct>%`
     *
     * Works on PipeWire (PulseAudio compat layer), PulseAudio, and any setup where
     * pactl is available.
     */
    private fun applyVolumeViaPactl(pid: Long, pct: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val clientsOutput = ProcessBuilder("pactl", "list", "clients")
                    .redirectErrorStream(true).start()
                    .inputStream.bufferedReader().readText()
                val clientIds = parseClientIds(clientsOutput, pid)
                if (clientIds.isEmpty()) return@launch

                val sinksOutput = ProcessBuilder("pactl", "list", "sink-inputs")
                    .redirectErrorStream(true).start()
                    .inputStream.bufferedReader().readText()

                val sinkIndex = clientIds.firstNotNullOfOrNull { cid ->
                    parseSinkInputByClientId(sinksOutput, cid)
                } ?: return@launch

                ProcessBuilder("pactl", "set-sink-input-volume", sinkIndex, "$pct%")
                    .redirectErrorStream(true).start().waitFor()
            } catch (_: Exception) { /* best-effort */ }
        }
    }

    /**
     * Finds ALL pactl client IDs whose `application.process.id` matches [pid],
     * then returns them all so the caller can try each one.
     *
     * ffplay registers two clients: one for itself and one for SDL2.
     * The SDL2 client is the one that owns the audio sink-input.
     */
    private fun parseClientIds(output: String, pid: Long): List<String> {
        val results = mutableListOf<String>()
        var currentClient: String? = null
        val pidStr = "\"$pid\""
        for (line in output.lines()) {
            val clientMatch = Regex("""^Client #(\d+)""").find(line.trim())
            if (clientMatch != null) currentClient = clientMatch.groupValues[1]
            if ((line.contains("application.process.id =") ||
                 line.contains("pipewire.sec.pid =")) && line.contains(pidStr)) {
                currentClient?.let { results.add(it) }
            }
        }
        return results
    }


    /**
     * Finds the sink-input index whose header `Client:` field matches [clientId].
     *
     * `pactl list sink-inputs` header format:
     *   Sink Input #84
     *       Driver: PipeWire
     *       Client: 42 ← this field, not the properties block
     */
    private fun parseSinkInputByClientId(output: String, clientId: String): String? {
        var currentIndex: String? = null
        for (line in output.lines()) {
            val indexMatch = Regex("""^Sink Input #(\d+)""").find(line.trim())

            if (indexMatch != null) {
                currentIndex = indexMatch.groupValues[1]
            }

            val clientMatch = Regex("""^\s+Client:\s+(\d+)$""").find(line)
            if (clientMatch != null && clientMatch.groupValues[1] == clientId) {
                return currentIndex
            }
        }
        return null
    }

    private fun buildFfplayProcess(url: String, volPct: Int, seekMs: Long): Process {
        val volume = volPct / 100.0

        val cmd = mutableListOf(
            ffplayBinary(),
            "-nodisp",
            "-autoexit",
            "-loglevel", "quiet",
            "-af", "volume=$volume",
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

    private fun sendUnixSignal(signal: String) {
        val pid = playerPid ?: return
        try {
            ProcessBuilder("kill", "-$signal", pid.toString())
                .redirectErrorStream(true)
                .start()
                .waitFor()
        } catch (_: Exception) { }
    }

    private fun suspendProcessWindows(pid: Long) {
        try {
            ProcessBuilder(
                "powershell", "-Command",
                $$"$proc = Get-Process -Id $$pid -ErrorAction SilentlyContinue; " +
                        $$"if ($proc) { $proc.Suspend() }"
            ).redirectErrorStream(true).start().waitFor()
        } catch (_: Exception) { }
    }

    private fun resumeProcessWindows(pid: Long) {
        try {
            ProcessBuilder(
                "powershell", "-Command",
                $$"$proc = [System.Diagnostics.Process]::GetProcessById($$pid); " +
                        $$"if ($proc) { $proc.Resume() }"
            ).redirectErrorStream(true).start().waitFor()
        } catch (_: Exception) { }
    }

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
