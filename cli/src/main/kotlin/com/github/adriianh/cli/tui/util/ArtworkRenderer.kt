package com.github.adriianh.cli.tui.util

import dev.tamboui.image.ImageData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Loads remote artwork images for display in the TUI.
 *
 * Receives the shared [HttpClient] from the DI container instead of owning
 * a separate instance — this avoids spinning up a second CIO engine with its
 * own thread pool alongside the one already used for all API calls.
 */
class ArtworkRenderer(private val httpClient: HttpClient) {

    suspend fun load(artworkUrl: String): ImageData? {
        return try {
            val bytes = httpClient.get(artworkUrl).body<ByteArray>()
            ImageData.fromBytes(bytes)
        } catch (_: Exception) {
            null
        }
    }
}