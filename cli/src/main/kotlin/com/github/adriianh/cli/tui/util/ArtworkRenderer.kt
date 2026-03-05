package com.github.adriianh.cli.tui.util

import dev.tamboui.image.ImageData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

object ArtworkRenderer {

    private val httpClient = HttpClient(CIO)

    suspend fun load(artworkUrl: String): ImageData? {
        return try {
            val response = httpClient.get(artworkUrl)
            val bytes = response.body<ByteArray>()
            ImageData.fromBytes(bytes)
        } catch (e: Exception) {
            null
        }
    }
}