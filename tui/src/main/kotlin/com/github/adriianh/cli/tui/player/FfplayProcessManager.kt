package com.github.adriianh.cli.tui.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manages the ffplay process lifecycle, process creation, termination,
 * and platform-specific suspend/resume signals.
 */
internal object FfplayProcessManager {
    val isWindows: Boolean = System.getProperty("os.name").lowercase().contains("win")

    suspend fun destroySafely(process: Process?, pid: Long?) {
        if (process == null) return
        withContext(Dispatchers.IO) {
            try {
                if (process.isAlive) {
                    if (pid != null && !isWindows) {
                        try {
                            ProcessBuilder("kill", "-SIGCONT", pid.toString())
                                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                                .redirectError(ProcessBuilder.Redirect.DISCARD)
                                .start()
                                .waitFor(150, TimeUnit.MILLISECONDS)
                        } catch (_: Exception) {
                        }
                    }
                    process.destroy()
                    val exited = process.waitFor(200, TimeUnit.MILLISECONDS)
                    if (!exited && process.isAlive) {
                        process.destroyForcibly()
                        process.waitFor(500, TimeUnit.MILLISECONDS)
                    }
                }
            } catch (_: Exception) {
                try {
                    process.destroyForcibly()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun buildProcess(url: String, volPct: Int, seekMs: Long): Process {
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

        val devNull = File(if (isWindows) "NUL" else "/dev/null")
        return ProcessBuilder(cmd)
            .redirectInput(ProcessBuilder.Redirect.from(devNull))
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    }

    fun suspendProcess(pid: Long) {
        if (isWindows) suspendProcessWindows(pid)
        else sendUnixSignal(pid, "SIGSTOP")
    }

    fun resumeProcess(pid: Long) {
        if (isWindows) resumeProcessWindows(pid)
        else sendUnixSignal(pid, "SIGCONT")
    }

    private fun sendUnixSignal(pid: Long, signal: String) {
        try {
            ProcessBuilder("kill", "-$signal", pid.toString())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
                .waitFor(200, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
        }
    }

    private fun suspendProcessWindows(pid: Long) {
        try {
            ProcessBuilder(
                "powershell", "-Command",
                $$"$proc = Get-Process -Id $$pid -ErrorAction SilentlyContinue; " +
                        $$"if ($proc) { $proc.Suspend() }"
            ).redirectErrorStream(true).start().waitFor()
        } catch (_: Exception) {
        }
    }

    private fun resumeProcessWindows(pid: Long) {
        try {
            ProcessBuilder(
                "powershell", "-Command",
                $$"$proc = [System.Diagnostics.Process]::GetProcessById($$pid); " +
                        $$"if ($proc) { $proc.Resume() }"
            ).redirectErrorStream(true).start().waitFor()
        } catch (_: Exception) {
        }
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