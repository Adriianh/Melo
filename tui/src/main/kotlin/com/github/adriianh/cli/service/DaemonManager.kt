package com.github.adriianh.cli.service

import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.adriianh.core.domain.model.Track
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

object DaemonManager {
    private val json = Json { ignoreUnknownKeys = true }
    val logFile: File get() = File(configDir, "daemon.log")

    fun isRunning(): Boolean {
        val response = LocalIpcClient.sendCommand("PING")
        return response == "PONG" || response.startsWith("OK")
    }

    fun startProcess(idleTimeout: Int = 0): Boolean {
        val javaHome = System.getProperty("java.home")
        val javaBin = if (javaHome != null) "$javaHome/bin/java" else "java"
        val classpath = System.getProperty("java.class.path")
        val mainClass = "com.github.adriianh.cli.MeloKt"

        val targetLogFile = logFile
        if (!targetLogFile.parentFile.exists()) targetLogFile.parentFile.mkdirs()

        val command = mutableListOf<String>()
        val isNative = System.getProperty("org.graalvm.nativeimage.imagecode") != null
        if (isNative) {
            val executablePath =
                ProcessHandle
                    .current()
                    .info()
                    .command()
                    .orElse("melo")
            if (executablePath != null) {
                command.add(executablePath)
            }
        } else {
            command.addAll(listOf(javaBin, "-cp", classpath, mainClass))
        }
        command.addAll(listOf("daemon", "run"))
        if (idleTimeout > 0) {
            command.addAll(listOf("--idle-timeout", idleTimeout.toString()))
        }

        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
        processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(logFile))

        return try {
            val process = processBuilder.start()
            process.isAlive
        } catch (_: Exception) {
            false
        }
    }

    fun ensureDaemonRunning(
        terminal: Terminal? = null,
        maxWaitMs: Long = 4000L,
        idleTimeout: Int = 0,
    ): Boolean {
        if (isRunning()) return true

        terminal?.println(gray("○ Starting Melo background daemon..."))
        var ready = false
        if (startProcess(idleTimeout)) {
            val deadline = System.currentTimeMillis() + maxWaitMs
            while (System.currentTimeMillis() < deadline && !ready) {
                Thread.sleep(150)
                ready = isRunning()
            }
            if (ready) {
                terminal?.println(cyan("✓ Daemon started and ready."))
            }
        }
        return ready
    }

    fun playTrack(track: Track): Boolean {
        val payload = json.encodeToString(Track.serializer(), track)
        val res = LocalIpcClient.sendCommand("PLAY_NOW", payload)
        return res.startsWith("OK")
    }

    fun playTracks(tracks: List<Track>): Boolean {
        if (tracks.isEmpty()) return false
        val payload = json.encodeToString(ListSerializer(Track.serializer()), tracks)
        val res = LocalIpcClient.sendCommand("PLAY_LIST", payload)
        return res.startsWith("OK")
    }
}
