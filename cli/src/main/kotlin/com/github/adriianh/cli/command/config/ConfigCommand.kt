package com.github.adriianh.cli.command.config

import com.github.adriianh.cli.config.Messages
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands

class ConfigCommand : CliktCommand(name = "config") {
    init {
        subcommands(
            ConfigSetCommand(),
            ConfigListCommand(),
            ConfigAuthCommand(),
        )
    }

    override fun help(context: Context): String = Messages.get("help.config_command")

    override fun run() = Unit
}