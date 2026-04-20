package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.PlayActionHandler
import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File
import kotlin.system.exitProcess

class DaemonCommand : NoOpCliktCommand(
    name = "daemon"
) {
    override fun help(context: Context): String = "Manage the Melo player daemon/service"

    init {
        subcommands(
            DaemonStartCommand(),
            DaemonRunCommand(),
            DaemonStopCommand(),
            DaemonStatusCommand()
        )
    }
}

class DaemonRunCommand : CliktCommand(
    name = "run"
), KoinComponent {
    private val terminal = Terminal()

    override fun help(context: Context): String =
        "Run the player daemon in the foreground (useful for debugging)"

    override fun run() {
        startKoin { modules(appModule) }
        try {
            val getStream: GetStreamUseCase by inject()
            runBlocking {
                terminal.println(cyan("Melo Daemon is starting in idle mode..."))
                terminal.println(yellow("Note: To run in the background (detached), use: melo daemon start"))
                PlayActionHandler.startDaemon(getStream, terminal)
            }
        } finally {
            stopKoin()
            exitProcess(0)
        }
    }
}

class DaemonStartCommand : CliktCommand(
    name = "start"
) {
    private val terminal = Terminal()

    override fun help(context: Context): String =
        "Start the player daemon in the background (detached)"

    override fun run() {
        val javaHome = System.getProperty("java.home")
        val javaBin = if (javaHome != null) "$javaHome/bin/java" else "java"
        val classpath = System.getProperty("java.class.path")
        val mainClass = "com.github.adriianh.cli.MeloKt"

        val logFile = File(configDir, "daemon.log")
        if (!logFile.parentFile.exists()) logFile.parentFile.mkdirs()

        // We try to use the same command that started us if possible
        // but spawning 'melo' directly is safer if it's in the PATH
        val command = mutableListOf<String>()

        // Check if we are running as a native image or jar
        val isNative = System.getProperty("org.graalvm.nativeimage.imagecode") != null
        if (isNative) {
            val executablePath = ProcessHandle.current().info().command().orElse("melo")
            if (executablePath != null) {
                command.add(executablePath)
            }
        } else {
            command.addAll(listOf(javaBin, "-cp", classpath, mainClass))
        }

        command.addAll(listOf("daemon", "run"))

        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
        processBuilder.redirectError(ProcessBuilder.Redirect.appendTo(logFile))

        try {
            val process = processBuilder.start()
            if (process.isAlive) {
                terminal.println(green("Melo daemon started in the background."))
                terminal.println(cyan("Logs are being written to: ") + yellow(logFile.absolutePath))
                terminal.println(cyan("Use 'melo daemon status' to verify."))
            } else {
                terminal.println(red("Daemon failed to start immediately. Check logs."))
            }
        } catch (e: Exception) {
            terminal.println(red("Error starting background process: ${e.message}"))
            terminal.println(yellow("Try running manually with: nohup melo daemon run > ${logFile.absolutePath} 2>&1 &"))
        }
    }
}

class DaemonStopCommand : CliktCommand(
    name = "stop"
) {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Stop the running daemon"

    override fun run() {
        terminal.println(cyan("Stopping Melo daemon..."))
        val result = LocalIpcClient.sendCommand("STOP")
        if (result.startsWith("ERROR")) {
            terminal.println(red(result))
        } else {
            terminal.println(green("Daemon stopped successfully."))
        }
    }
}

class DaemonStatusCommand : CliktCommand(
    name = "status"
) {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Check the daemon status"

    override fun run() {
        val result = LocalIpcClient.sendCommand("QUEUE_LIST")
        if (result.startsWith("ERROR")) {
            terminal.println(red("Daemon is NOT running."))
            terminal.println(yellow("Use 'melo daemon run' to start it."))
        } else {
            terminal.println(green("Daemon is running and healthy."))
            terminal.println(cyan("Connected to IPC server."))
        }
    }
}