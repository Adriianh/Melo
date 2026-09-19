package com.github.adriianh.cli.tui.player

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.player.PlaybackState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Audio player backed by ffplay (bundled with ffmpeg).
 *
 * ffplay uses SDL2 for audio output, which auto-selects the best backend per OS:
 *   Linux → PipeWire / PulseAudio / ALSA
 *   macOS → CoreAudio
 *   Windows → DirectSound / WASAPI
 *
 * Pause: SIGSTOP / SIGCONT — instantaneous, no buffer delay.
 * Volume: pactl set-sink-input-volume on Linux/PulseAudio (no interruption);
 *          fallback: stored and applied on next play() via -af volume= filter.
 *
 * Implements [MeloPlayer] so the same playback contract used by the Compose
 * app (and driven by `data.PlaybackManagerImpl`) can drive this ffplay engine.
 */
class AudioPlayer(
    private val scope: CoroutineScope,
    private val onProgress: (elapsedMs: Long) -> Unit = {},
    private val onFinish: () -> Unit = {},
    private val onError: (Throwable) -> Unit = {},
) : MeloPlayer {
    companion object {
        private val CLIENT_HEADER_RE = Regex("""^Client #(\d+)""")
        private val SINK_INPUT_RE = Regex("""^Sink Input #(\d+)""")
        private val SINK_CLIENT_RE = Regex("""^\s+Client:\s+(\d+)$""")
    }
    private var playJob: Job? = null
    private var playerProcess: Process? = null
    private var playerPid: Long? = null

    private val isPaused = AtomicBoolean(false)

    private val sessionId = AtomicLong(0L)

    @Volatile private var startTimeMs: Long = 0L
    @Volatile private var pausedAtMs: Long = 0L
    @Volatile private var volumePct: Int = 75
    @Volatile private var currentUrl: String? = null

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    val isPlaying: Boolean get() = playJob?.isActive == true && !isPaused.get()

    private val isWindows = System.getProperty("os.name").lowercase().contains("win")
    private val hasPactl: Boolean by lazy {
        try {
            ProcessBuilder("pactl", "--version").redirectErrorStream(true).start().waitFor() == 0
        } catch (_: Exception) { false }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    fun play(url: String) {
        startPlayback(url = url, seekMs = 0L)
    }

    /**
     * Stages a stream and starts playback immediately, mirroring [JvmMeloPlayer.load]
     * semantics: load() already starts playing, [play] is only needed to resume or
     * restart after the process has exited.
     */
    override fun load(url: String, track: Track, initialPositionMs: Long) {
        _state.update {
            it.copy(
                currentTrack = track,
                progressMs = initialPositionMs,
                durationMs = track.durationMs,
                isBuffering = true,
                isFinished = false,
                error = null,
            )
        }
        startPlayback(url = url, seekMs = initialPositionMs)
    }

    override fun play() {
        if (isPaused.get()) {
            resume()
            return
        }
        val url = currentUrl
        if (url != null && playJob?.isActive != true) {
            startPlayback(url = url, seekMs = _state.value.progressMs)
        }
    }

    private fun startPlayback(url: String, seekMs: Long) {
        val session = sessionId.incrementAndGet()
        val previousJob = playJob
        val previousProcess = playerProcess

        _state.update {
            it.copy(
                isPlaying = true,
                isBuffering = false,
                isFinished = false,
                error = null,
            )
        }

        playJob = scope.launch {
            // Ensure the previous playback completely stops before starting a new one
            previousProcess?.destroy()
            previousJob?.cancel()
            previousProcess?.waitFor()
            previousJob?.join()

            if (sessionId.get() != session) return@launch // another play() was called while we waited

            currentUrl = url
            isPaused.set(false)
            startTimeMs = System.currentTimeMillis()
            pausedAtMs = 0L
            launchPlayback(url, volumePct, seekMs = seekMs, session = session)
        }
    }

    override fun pause() {
        if (isPaused.get() || !isPlaying) return
        isPaused.set(true)
        pausedAtMs += System.currentTimeMillis() - startTimeMs
        if (isWindows) suspendProcessWindows(playerPid ?: return)
        else           sendUnixSignal("SIGSTOP")
        _state.update { it.copy(isPlaying = false, isBuffering = false) }
    }

    fun resume() {
        if (!isPaused.get()) return
        startTimeMs = System.currentTimeMillis()
        isPaused.set(false)
        if (isWindows) resumeProcessWindows(playerPid ?: return)
        else           sendUnixSignal("SIGCONT")
        _state.update { it.copy(isPlaying = true) }
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

    /** Contract override: volume is 0f..1f in the shared playback stack. */
    override fun setVolume(volume: Float) {
        setVolume((volume * 100).toInt())
    }

    override fun seekTo(positionMs: Long) {
        _state.update { it.copy(progressMs = positionMs) }
        seek(positionMs)
    }

    /**
     * Seek to [ms] milliseconds from the start of the current stream.
     * Restarts ffplay with -ss, preserving volume and paused state.
     */
    fun seek(ms: Long) {
        val url = currentUrl ?: return
        val clampedMs = ms.coerceAtLeast(0L)
        val wasPaused = isPaused.get()

        val session = sessionId.incrementAndGet()
        val previousJob = playJob
        val previousProcess = playerProcess

        playJob = scope.launch {
            previousProcess?.destroy()
            previousJob?.cancel()
            previousProcess?.waitFor()
            previousJob?.join()

            if (sessionId.get() != session) return@launch

            playerProcess = null
            playerPid = null
            isPaused.set(false)

            pausedAtMs = if (wasPaused) clampedMs else 0L
            startTimeMs = System.currentTimeMillis()

            if (wasPaused) isPaused.set(true)

            launchPlayback(url, volumePct, seekMs = clampedMs, session = session)

            if (wasPaused) {
                // We launch another coroutine to suspend the process shortly after it starts
                scope.launch {
                    delay(300)
                    if (isPaused.get() && sessionId.get() == session) {
                        if (isWindows) suspendProcessWindows(playerPid ?: return@launch)
                        else sendUnixSignal("SIGSTOP")
                    }
                }
            }
        }
    }
    override fun stop() {
        val session = sessionId.incrementAndGet() // invalidate any running job
        val previousJob = playJob
        val previousProcess = playerProcess
        
        isPaused.set(false)
        currentUrl = null
        pausedAtMs = 0L

        _state.update {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                isFinished = false,
                error = null,
            )
        }

        playJob = scope.launch {
            previousProcess?.destroy()
            previousJob?.cancel()
            previousProcess?.waitFor()
            previousJob?.join()

            if (sessionId.get() != session) return@launch
            playerProcess = null
            playerPid = null
        }
    }

    override fun release() {
        stop()
        playJob?.cancel()
    }

    override fun setIdleTrack(track: Track, initialPositionMs: Long) {
        _state.update {
            it.copy(
                currentTrack = track,
                progressMs = initialPositionMs,
                durationMs = track.durationMs,
                isPlaying = false,
                isBuffering = false,
                isFinished = false,
                error = null,
            )
        }
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private suspend fun launchPlayback(url: String, volPct: Int, seekMs: Long, session: Long) {
        try {
            val process = buildFfplayProcess(url, volPct, seekMs)
            playerProcess = process
            playerPid = process.pid()
            startTimeMs = System.currentTimeMillis()

            val progressJob = scope.launch {
                while (isActive && sessionId.get() == session) {
                    if (!isPaused.get()) {
                        val elapsed = pausedAtMs + (System.currentTimeMillis() - startTimeMs)
                        val positionMs = elapsed + seekMs
                        _state.update { it.copy(progressMs = positionMs) }
                        onProgress(positionMs)
                    }
                    delay(1000)
                }
            }

            withContext(Dispatchers.IO) {
                process.waitFor()
            }

            progressJob.cancel()
            _state.update { it.copy(isPlaying = false, isFinished = true) }
            if (sessionId.get() == session) onFinish()
        } catch (e: CancellationException) {
            playerProcess?.destroy()
            throw e
        } catch (e: Exception) {
            playerProcess?.destroy()
            _state.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    isFinished = false,
                    error = e.message,
                )
            }
            if (sessionId.get() == session) onError(e)
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
        scope.launch {
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
            val clientMatch = CLIENT_HEADER_RE.find(line.trim())
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
            val indexMatch = SINK_INPUT_RE.find(line.trim())

            if (indexMatch != null) {
                currentIndex = indexMatch.groupValues[1]
            }

            val clientMatch = SINK_CLIENT_RE.find(line)
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
            "-loglevel", "error",
            "-af", "volume=$volume",
        )

        if (seekMs > 0) {
            cmd += listOf("-ss", (seekMs / 1000.0).toString())
        }

        // Only add reconnect options for HTTP/HTTPS streams, not local files
        if (url.startsWith("http://") || url.startsWith("https://")) {
            cmd += listOf(
                "-reconnect", "1",
                "-reconnect_streamed", "1",
                "-reconnect_delay_max", "5",
            )
        }

        cmd += listOf("-i", url)

        return ProcessBuilder(cmd)
            .redirectErrorStream(true)
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
