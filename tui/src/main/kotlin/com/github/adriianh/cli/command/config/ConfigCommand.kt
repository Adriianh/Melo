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
        )
    }

    override fun help(context: Context): String = Messages.get("help.config_command")

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo config list\n" +
            "  melo config set LASTFM_API_KEY \"your_api_key\"\n" +
            "  melo config set SPOTIFY_CLIENT_ID \"your_client_id\""

    override fun run() = Unit
}
