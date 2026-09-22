package com.github.adriianh.cli.tui.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manages the ffplay process lifecycle, process creation, termination,
 * and platform-specific suspend/resume signals.
 */
internal object FfplayProcessManager {
    val isWindows: Boolean = System.getProperty("os.name").lowercase().contains("win")

    private val activeProcesses = ConcurrentHashMap.newKeySet<Process>()

    init {
        try {
            Runtime.getRuntime().addShutdownHook(Thread({
                killAll()
            }, "melo-ffplay-cleanup"))
        } catch (_: Throwable) {
        }
    }

    /**
     * Immediately and synchronously terminates all currently running or registered ffplay processes.
     */
    fun killAll() {
        val processes = activeProcesses.toList()
        activeProcesses.clear()
        for (proc in processes) {
            try {
                destroyImmediately(proc, proc.pid())
            } catch (_: Throwable) {
            }
        }
        try {
            ProcessHandle.current().children().forEach { child ->
                try {
                    child.destroyForcibly()
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
        }
    }

    /**
     * Synchronously destroys a process, ensuring any suspended state is resumed
     * before sending SIGTERM / SIGKILL.
     */
    fun destroyImmediately(process: Process?, pid: Long?) {
        if (process == null) return
        activeProcesses.remove(process)
        try {
            if (process.isAlive) {
                if (pid != null && !isWindows) {
                    try {
                        ProcessBuilder("kill", "-SIGCONT", pid.toString())
                            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                            .start()
                            .waitFor(100, TimeUnit.MILLISECONDS)
                    } catch (_: Exception) {
                    }
                }
                process.destroy()
                val exited = process.waitFor(150, TimeUnit.MILLISECONDS)
                if (!exited && process.isAlive) {
                    process.destroyForcibly()
                    process.waitFor(300, TimeUnit.MILLISECONDS)
                }
            }
        } catch (_: Exception) {
            try {
                process.destroyForcibly()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun destroySafely(process: Process?, pid: Long?) {
        if (process == null) return
        withContext(Dispatchers.IO + NonCancellable) {
            destroyImmediately(process, pid)
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

        val process = ProcessBuilder(cmd)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
        try {
            process.outputStream.close()
        } catch (_: Throwable) {
        }
        activeProcesses.add(process)
        return process
    }

    fun suspendProcess(pid: Long) {
        if (!isWindows) sendUnixSignal(pid, "SIGSTOP")
    }

    fun resumeProcess(pid: Long) {
        if (!isWindows) sendUnixSignal(pid, "SIGCONT")
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

    fun findFfplayBinary(): String? {
        val name = if (isWindows) "ffplay.exe" else "ffplay"
        val candidates = mutableListOf(name)
        if (isWindows) {
            val localAppData = System.getenv("LOCALAPPDATA")
            if (!localAppData.isNullOrBlank()) {
                candidates.add("$localAppData\\melo-tui\\bin\\ffplay.exe")
                candidates.add("$localAppData\\melo-tui\\ffplay.exe")
                candidates.add("$localAppData\\Microsoft\\WinGet\\Links\\ffplay.exe")
            }
            val userProfile = System.getenv("USERPROFILE")
            if (!userProfile.isNullOrBlank()) {
                candidates.add("$userProfile\\scoop\\shims\\ffplay.exe")
                candidates.add("$userProfile\\scoop\\apps\\ffmpeg\\current\\bin\\ffplay.exe")
            }
            val programFiles = System.getenv("ProgramFiles") ?: "C:\\Program Files"
            candidates.add("$programFiles\\ffmpeg\\bin\\ffplay.exe")
            candidates.add("C:\\ffmpeg\\bin\\ffplay.exe")
            candidates.add("C:\\ProgramData\\chocolatey\\bin\\ffplay.exe")
        } else {
            candidates.addAll(
                listOf(
                    "/usr/bin/ffplay",
                    "/usr/local/bin/ffplay",
                    "/opt/homebrew/bin/ffplay",
                )
            )
        }
        for (bin in candidates) {
            try {
                val p = ProcessBuilder(bin, "-version").redirectErrorStream(true).start()
                p.waitFor()
                if (p.exitValue() == 0) return bin
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    fun ffplayBinary(): String {
        return findFfplayBinary()
            ?: if (isWindows) {
                throw IllegalStateException("ffplay not found. Please install FFmpeg (e.g., winget install Gyan.FFmpeg)")
            } else {
                throw IllegalStateException("ffplay not found. Please install ffmpeg")
            }
    }
}