package com.github.adriianh.data.remote.musicbrainz

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class MusicBrainzApiClient {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            header("User-Agent", "Melo/1.0 (adriianh@proton.me)")
            header("Accept", "application/json")
        }
    }

    private suspend fun fetchRecordings(query: String, limit: Int = 20): MusicBrainzRecordingSearchResponse {
        return httpClient.get("https://musicbrainz.org/ws/2/recording/") {
            parameter("query", query)
            parameter("fmt", "json")
            parameter("limit", limit)
        }.body()
    }

    suspend fun search(query: String, limit: Int = 20): List<MusicBrainzRecording> {
        return try {
            fetchRecordings(query, limit).recordings
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("MusicBrainz search error: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTrack(artist: String, title: String): MusicBrainzRecording? {
        return try {
            fetchRecordings("recording:\"$title\" AND artist:\"$artist\"", limit = 1)
                .recordings.firstOrNull()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("MusicBrainz error: ${e.message}")
            null
        }
    }

    suspend fun getCoverArtUrl(releaseId: String): String? {
        return try {
            val response = httpClient.get(
                "https://coverartarchive.org/release/$releaseId"
            ).body<CoverArtResponse>()
            response.images.firstOrNull { it.front }?.thumbnails?.large
                ?: response.images.firstOrNull { it.front }?.thumbnails?.medium
                ?: response.images.firstOrNull()?.image
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}