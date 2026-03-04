package com.github.adriianh.data.remote.piped

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.math.abs

class PipedApiClient(
    private val httpClient: HttpClient
) {

    private val baseUrl = "https://api.piped.private.coffee"

    suspend fun search(query: String, title: String, artist: String, durationMs: Long): String? {
        return try {
            val response = httpClient.get("$baseUrl/search") {
                parameter("q", query)
                parameter("filter", "music_songs")
            }.body<PipedSearchResponse>()

            val cleanTitle = normalize(title)
            val cleanArtist = normalize(artist)

            val best = response.items
                .filter { it.type == "stream" }
                .firstOrNull { item ->
                    val itemTitle = normalize(item.title)
                    val itemUploader = normalize(item.uploaderName)
                    val titleMatches = itemTitle.contains(cleanTitle) || cleanTitle.contains(itemTitle)
                    val artistMatches = itemUploader.contains(cleanArtist) || cleanArtist.contains(itemUploader)
                    val durationMatches = durationMs == 0L ||
                        abs(item.duration * 1000 - durationMs) < 10_000
                    titleMatches && artistMatches && durationMatches
                }
                ?: response.items.firstOrNull { it.type == "stream" }

            best?.url?.substringAfter("v=")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getStreamUrl(videoId: String): String? {
        return try {
            val response = httpClient.get("$baseUrl/streams/$videoId")
                .body<PipedStreamsResponse>()

            // Prefer opus/webm for best quality, fallback to any available stream
            response.audioStreams
                .filter { it.url.isNotBlank() }
                .maxByOrNull { it.bitrate }
                ?.url
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun normalize(text: String): String = text
        .lowercase()
        .replace(Regex("\\s*\\(.*?\\)"), "")
        .replace(Regex("\\s*\\[.*?]"), "")
        .replace(Regex("[^a-z0-9 ]"), "")
        .trim()
}