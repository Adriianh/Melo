package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.mordant.terminal.Terminal

class PauseCommand : CliktCommand(name = "pause") {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Pause current playback in the active player or daemon."

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo pause"

    override fun run() {
        val result = LocalIpcClient.sendCommand("PAUSE")
        if (result.startsWith("ERROR")) {
            terminal.println(result)
        } else {
            terminal.println("Playback paused.")
        }
    }
}
