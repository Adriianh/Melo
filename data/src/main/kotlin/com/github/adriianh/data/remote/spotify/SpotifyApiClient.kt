package com.github.adriianh.data.remote.spotify

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class SpotifyApiClient(
    private val httpClient: HttpClient,
    private val authClient: SpotifyAuthClient
) {

    suspend fun search(query: String, limit: Int = 20): SpotifySearchResponse {
        val token = authClient.getAccessToken()

        return httpClient.get("https://api.spotify.com/v1/search") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("q", query)
            parameter("type", "track")
            parameter("limit", limit)
        }.body()
    }

    suspend fun getTrack(id: String): SpotifyTrackDto? {
        return try {
            val token = authClient.getAccessToken()
            httpClient.get("https://api.spotify.com/v1/tracks/$id") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }.body<SpotifyTrackDto>()
        } catch (e: Exception) {
            null
        }
    }
}