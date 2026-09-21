package com.github.adriianh.cli.tui.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
internal object PactlVolumeController {
    private val CLIENT_HEADER_RE = Regex("""^Client #(\d+)""")
    private val SINK_INPUT_RE = Regex("""^Sink Input #(\d+)""")
    private val SINK_CLIENT_RE = Regex("""^\s+Client:\s+(\d+)$""")

    val hasPactl: Boolean by lazy {
        try {
            ProcessBuilder("pactl", "--version").redirectErrorStream(true).start().waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    fun applyVolume(scope: CoroutineScope, pid: Long, pct: Int) {
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
            } catch (_: Exception) { /* best-effort */
            }
        }
    }

    /**
     * Finds ALL pactl client IDs whose `application.process.id` matches [pid],
     * then returns them all so the caller can try each one.
     *
     * ffplay registers two clients: one for itself and one for SDL2.
     * The SDL2 client is the one that owns the audio sink-input.
     */
    internal fun parseClientIds(output: String, pid: Long): List<String> {
        val results = mutableListOf<String>()
        var currentClient: String? = null
        val pidStr = "\"$pid\""
        for (line in output.lines()) {
            val clientMatch = CLIENT_HEADER_RE.find(line.trim())
            if (clientMatch != null) currentClient = clientMatch.groupValues[1]
            if ((line.contains("application.process.id =") ||
                        line.contains("pipewire.sec.pid =")) && line.contains(pidStr)
            ) {
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
    internal fun parseSinkInputByClientId(output: String, clientId: String): String? {
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
}