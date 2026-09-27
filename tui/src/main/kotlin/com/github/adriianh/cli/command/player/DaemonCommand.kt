package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.PlayActionHandler
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.service.DaemonManager
import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess

class DaemonCommand :
    NoOpCliktCommand(
        name = "daemon",
    ) {
    override fun help(context: Context): String = "Manage the background player daemon for headless playback and IPC."

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo daemon start\n" +
            "  melo daemon start --idle-timeout 30\n" +
            "  melo daemon status\n" +
            "  melo daemon stop\n" +
            "  melo daemon run"

    init {
        subcommands(
            DaemonStartCommand(),
            DaemonRunCommand(),
            DaemonStopCommand(),
            DaemonStatusCommand(),
        )
    }
}

class DaemonRunCommand :
    CliktCommand(
        name = "run",
    ),
    KoinComponent {
    private val terminal = Terminal()
    private val idleTimeout by option(
        "--idle-timeout",
        help = "Auto-shutdown after N minutes of inactivity (0 = disabled, default: 0)",
    ).int().default(0)

    override fun help(context: Context): String = "Run the player daemon in the foreground (useful for debugging)"

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo daemon run\n" +
            "  melo daemon run --idle-timeout 30"

    override fun run() {
        startKoin { modules(appModule) }
        try {
            val getStream: GetStreamUseCase by inject()
            runBlocking {
                terminal.println(cyan("Melo Daemon is starting in idle mode..."))
                terminal.println(yellow("Note: To run in the background (detached), use: melo daemon start"))
                if (idleTimeout > 0) {
                    terminal.println(gray("Idle timeout set to $idleTimeout minutes."))
                }
                PlayActionHandler.startDaemon(getStream, terminal, idleTimeoutMinutes = idleTimeout)
            }
        } finally {
            stopKoin()
            exitProcess(0)
        }
    }
}

class DaemonStartCommand :
    CliktCommand(
        name = "start",
    ) {
    private val terminal = Terminal()
    private val idleTimeout by option(
        "--idle-timeout",
        help = "Auto-shutdown after N minutes of inactivity (0 = disabled, default: 0)",
    ).int().default(0)

    override fun help(context: Context): String = "Start the Melo player daemon in the background (detached)"

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo daemon start\n" +
            "  melo daemon start --idle-timeout 30"

    override fun run() {
        if (DaemonManager.isRunning()) {
            terminal.println(green("Melo daemon is already running."))
            return
        }

        terminal.println(cyan("Starting Melo daemon in background..."))
        if (DaemonManager.ensureDaemonRunning(terminal, idleTimeout = idleTimeout)) {
            terminal.println(green("Melo daemon started in the background."))
            if (idleTimeout > 0) {
                terminal.println(gray("Idle timeout set to $idleTimeout minutes."))
            }
            terminal.println(cyan("Logs are being written to: ") + yellow(DaemonManager.logFile.absolutePath))
            terminal.println(cyan("Use 'melo daemon status' to verify."))
        } else {
            terminal.println(
                red("Daemon failed to start. Check logs at: ") + yellow(DaemonManager.logFile.absolutePath),
            )
        }
    }
}

class DaemonStopCommand :
    CliktCommand(
        name = "stop",
    ) {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Stop the running Melo player daemon"

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo daemon stop"

    override fun run() {
        if (!DaemonManager.isRunning()) {
            terminal.println(yellow("Melo daemon is not running."))
            return
        }
        terminal.println(cyan("Stopping Melo daemon..."))
        val result = LocalIpcClient.sendCommand("STOP")
        if (result.startsWith("ERROR")) {
            terminal.println(red(result))
        } else {
            terminal.println(green("Daemon stopped successfully."))
        }
    }
}

class DaemonStatusCommand :
    CliktCommand(
        name = "status",
    ) {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Check if the Melo player daemon is currently active"

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo daemon status"

    override fun run() {
        if (DaemonManager.isRunning()) {
            terminal.println(green("Daemon is running and healthy."))
            terminal.println(cyan("Connected to IPC server."))
        } else {
            terminal.println(red("Daemon is NOT running."))
            terminal.println(yellow("Use 'melo daemon start' to launch it in the background."))
        }
    }
}
