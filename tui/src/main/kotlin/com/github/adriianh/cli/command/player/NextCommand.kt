package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.mordant.terminal.Terminal

class NextCommand : CliktCommand(name = "next") {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Skip to the next track in the playback queue."

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo next"

    override fun run() {
        val result = LocalIpcClient.sendCommand("NEXT")
        if (result.startsWith("ERROR")) {
            terminal.println(result)
        } else {
            terminal.println("Skipped to next track.")
        }
    }
}
