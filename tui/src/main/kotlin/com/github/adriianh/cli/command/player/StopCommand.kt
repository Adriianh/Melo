package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.mordant.terminal.Terminal

class StopCommand : CliktCommand(name = "stop") {
    private val terminal = Terminal()
    override fun help(context: Context): String = "Stop playback entirely"
    override fun run() {
        val result = LocalIpcClient.sendCommand("STOP")
        if (result.startsWith("ERROR")) terminal.println(result)
        else terminal.println("Playback stopped.")
    }
}