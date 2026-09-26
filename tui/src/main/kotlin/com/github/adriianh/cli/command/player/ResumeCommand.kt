package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.mordant.terminal.Terminal

class ResumeCommand : CliktCommand(name = "resume") {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Resume paused playback in the active player or daemon."

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo resume"

    override fun run() {
        val result = LocalIpcClient.sendCommand("RESUME")
        if (result.startsWith("ERROR")) {
            terminal.println(result)
        } else {
            terminal.println("Playback resumed.")
        }
    }
}
