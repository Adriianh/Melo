package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.PlayActionHandler
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

class DaemonCommand : NoOpCliktCommand(
    name = "daemon"
) {
    override fun help(context: Context): String = "Manage the Melo player daemon/service"

    init {
        subcommands(
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

    override fun help(context: Context): String = "Run the player daemon in the foreground"

    override fun run() {
        startKoin { modules(appModule) }
        try {
            val getStream: GetStreamUseCase by inject()
            runBlocking {
                terminal.println(cyan("Melo Daemon is starting in idle mode..."))
                terminal.println(yellow("To run in background, use: melo daemon run &"))
                PlayActionHandler.startDaemon(getStream, terminal)
            }
        } finally {
            stopKoin()
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