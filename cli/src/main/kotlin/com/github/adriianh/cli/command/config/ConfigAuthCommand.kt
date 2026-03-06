package com.github.adriianh.cli.command.config

import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.data.remote.lastfm.LastFmApiClient
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File

class ConfigAuthCommand : CliktCommand(name = "auth") {
    private val username by option("--username", "-u", help = "Last.fm username").prompt("Last.fm username")
    private val password by option("--password", "-p", help = "Last.fm password").prompt("Last.fm password", hideInput = true)

    override fun run() {
        val apiKey = resolveEnv("LASTFM_API_KEY")
        val sharedSecret = resolveEnv("LASTFM_SHARED_SECRET")

        if (apiKey == null || sharedSecret == null) {
            echo("✗ LASTFM_API_KEY and LASTFM_SHARED_SECRET must be configured first.", err = true)
            echo("  Run: melo config set LASTFM_API_KEY <key>", err = true)
            echo("  Run: melo config set LASTFM_SHARED_SECRET <secret>", err = true)
            return
        }

        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                val json = Json { ignoreUnknownKeys = true; isLenient = true }
                json(json)
                json(json, contentType = ContentType.Text.JavaScript)
            }
        }

        echo("Authenticating with Last.fm…")
        val sessionKey = runBlocking {
            try {
                LastFmApiClient(client, apiKey, sharedSecret).getMobileSession(username, password)
            } finally {
                client.close()
            }
        }

        if (sessionKey == null) {
            echo("✗ Authentication failed. Check your username, password and API credentials.", err = true)
            return
        }

        val envFile = File("$configDir/.env")
        envFile.parentFile?.mkdirs()
        val lines = if (envFile.exists()) envFile.readLines() else emptyList()
        val key = "LASTFM_SESSION_KEY"
        val updated = if (lines.any { it.startsWith("$key=") }) {
            lines.map { if (it.startsWith("$key=")) "$key=$sessionKey" else it }
        } else {
            lines + "$key=$sessionKey"
        }
        envFile.writeText(updated.joinToString("\n") + "\n")
        echo("✓ Authenticated as $username — session key saved.")
    }
}

