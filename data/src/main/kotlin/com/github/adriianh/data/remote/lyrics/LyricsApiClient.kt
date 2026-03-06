package com.github.adriianh.data.remote.lyrics

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class LyricsApiClient(
    private val httpClient: HttpClient
) {

    /**
     * Fetches both plain and synced lyrics in a single request.
     * Returns null if the request fails or the track is not found.
     */
    suspend fun getLyricsResponse(artist: String, title: String): LyricsResponse? {
        return try {
            httpClient.get("https://lrclib.net/api/get") {
                parameter("artist_name", artist)
                parameter("track_name", title)
            }.body<LyricsResponse>()
        } catch (_: Exception) {
            null
        }
    }
}