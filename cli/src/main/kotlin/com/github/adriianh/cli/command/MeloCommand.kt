package com.github.adriianh.cli.command

import com.github.adriianh.cli.command.player.SearchCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class MeloCommand : CliktCommand(
    name = "melo"
) {
    init {
        subcommands(
            SearchCommand()
        )
    }

    override fun run() = Unit
}