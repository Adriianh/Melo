package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.mordant.terminal.Terminal

class PrevCommand : CliktCommand(name = "prev") {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Skip to the previous track or restart the current track."

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo prev"

    override fun run() {
        val result = LocalIpcClient.sendCommand("PREV")
        if (result.startsWith("ERROR")) {
            terminal.println(result)
        } else {
            terminal.println("Skipped to previous track.")
        }
    }
}
