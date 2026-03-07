package com.github.adriianh.cli.command.config

import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.data.remote.lastfm.LastFmApiClient
import com.github.adriianh.data.repository.ScrobblingRepositoryImpl
import com.github.ajalt.clikt.core.CliktCommand
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.net.URI

class ConfigAuthCommand : CliktCommand(name = "auth") {
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

        val repository = ScrobblingRepositoryImpl(
            client = LastFmApiClient(client, apiKey, sharedSecret),
            configDir = configDir,
        )

        val authUrl = runBlocking { repository.startWebAuth() }
        if (authUrl == null) {
            echo("✗ Could not obtain a token from Last.fm. Check your API credentials.", err = true)
            client.close()
            return
        }

        echo("Opening Last.fm in your browser to authorize Melo…")
        echo("  $authUrl")

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(authUrl))
        } else {
            echo("  (Could not open browser automatically — please open the URL above manually.)")
        }

        echo("")
        echo("Once you have authorized Melo in the browser, press ENTER to continue.")
        readlnOrNull()

        val token = repository.getPendingToken()
        if (token == null) {
            echo("✗ No pending token found. Please run 'melo config auth' again.", err = true)
            client.close()
            return
        }

        val success = runBlocking { repository.completeWebAuth(token) }
        client.close()

        if (success) {
            echo("✓ Authenticated with Last.fm — session key saved.")
        } else {
            echo("✗ Authorization failed. Make sure you approved the request in the browser, then try again.", err = true)
        }
    }
}