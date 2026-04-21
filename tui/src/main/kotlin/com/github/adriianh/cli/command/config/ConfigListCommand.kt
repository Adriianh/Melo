package com.github.adriianh.cli.command.config

import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.config.configDir
import com.github.ajalt.clikt.core.CliktCommand
import java.io.File

class ConfigListCommand : CliktCommand(name = "list") {
    override fun run() {
        val envFile = File("$configDir/.env")

        if (!envFile.exists()) {
            echo(Messages.get("config.list.empty", "configDir" to configDir))
            return
        }

        val entries = envFile.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") && "=" in it }

        if (entries.isEmpty()) {
            echo(Messages.get("config.list.empty", "configDir" to configDir))
            return
        }

        echo(Messages.get("config.list.header", "configDir" to configDir))
        entries.forEach { line ->
            val (k, v) = line.split("=", limit = 2)
            // Mask the value, showing only the last 4 characters
            val masked = if (v.length > 4) "${"*".repeat(v.length - 4)}${v.takeLast(4)}" else "****"
            echo("  $k = $masked")
        }
    }
}

