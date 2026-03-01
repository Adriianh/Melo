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

    suspend fun getLyrics(artist: String, title: String): String? {
        return try {
            val response = httpClient.get("https://lrclib.net/api/get") {
                parameter("artist_name", artist)
                parameter("track_name", title)
            }
            response.body<LyricsResponse>().plainLyrics
        } catch (e: Exception) {
            null
        }
    }
}