package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.mordant.terminal.Terminal

class PauseCommand : CliktCommand(name = "pause") {
    private val terminal = Terminal()
    override fun help(context: Context): String = "Pause playback"
    override fun run() {
        val result = LocalIpcClient.sendCommand("PAUSE")
        if (result.startsWith("ERROR")) terminal.println(result)
        else terminal.println("Playback paused.")
    }
}