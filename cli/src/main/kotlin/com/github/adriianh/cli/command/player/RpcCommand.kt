package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal

class RpcCommand : NoOpCliktCommand(
    name = "rpc"
) {
    override fun help(context: Context): String = "Manage Discord Rich Presence"

    init {
        subcommands(
            RpcToggleCommand(),
            RpcStatusCommand()
        )
    }
}

class RpcToggleCommand : CliktCommand(
    name = "toggle"
) {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Toggle Discord Rich Presence on/off"

    override fun run() {
        val result = LocalIpcClient.sendCommand("RPC_TOGGLE")
        if (result.startsWith("ERROR")) {
            terminal.println(yellow("Daemon is not running. Start it with 'melo daemon run'"))
        } else {
            terminal.println(green(result))
        }
    }
}

class RpcStatusCommand : CliktCommand(
    name = "status"
) {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Show current RPC status"

    override fun run() {
        val result = LocalIpcClient.sendCommand("QUEUE_LIST")
        if (result.startsWith("ERROR")) {
            terminal.println(yellow("Daemon is not running."))
        } else {
            terminal.println(green("Daemon is active. RPC is managed by the daemon."))
            terminal.println(cyan("Use 'melo rpc toggle' to change state."))
        }
    }
}