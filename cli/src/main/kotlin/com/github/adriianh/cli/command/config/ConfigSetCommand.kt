package com.github.adriianh.cli.command.config

import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.config.configDir
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import java.io.File

private val ALLOWED_KEYS = setOf(
    "LASTFM_API_KEY",
    "LASTFM_SHARED_SECRET",
    "SPOTIFY_CLIENT_ID",
    "SPOTIFY_CLIENT_SECRET",
)

class ConfigSetCommand : CliktCommand(name = "set") {
    private val key   by argument(help = "Config key (${ALLOWED_KEYS.joinToString(", ")})")
    private val value by argument(help = "Value to assign")

    override fun run() {
        val upperKey = key.uppercase()
        if (upperKey !in ALLOWED_KEYS) {
            echo("✗ Unknown key: $upperKey", err = true)
            echo("  Valid keys: ${ALLOWED_KEYS.joinToString(", ")}", err = true)
            return
        }

        val envFile = File("$configDir/.env")
        envFile.parentFile.mkdirs()

        val lines: List<String> = if (envFile.exists()) {
            envFile.readLines()
        } else {
            emptyList()
        }

        val keyExists = lines.any { it.startsWith("$upperKey=") || it.startsWith("$upperKey =") }
        val newLines = if (keyExists) {
            lines.map { line ->
                if (line.startsWith("$upperKey=") || line.startsWith("$upperKey =")) "$upperKey=$value"
                else line
            }
        } else {
            lines + "$upperKey=$value"
        }

        envFile.writeText(newLines.joinToString("\n") + "\n")

        val messageKey = if (keyExists) "config.set.overwrite" else "config.set.success"
        echo(Messages.get(messageKey, "key" to upperKey, "configDir" to configDir))
    }
}

