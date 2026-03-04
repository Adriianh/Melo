package com.github.adriianh.data.remote.piped

import com.github.adriianh.core.domain.model.Track
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
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

    /**
     * Returns a list of [Track]s from Piped's `music_songs` search, suitable for
     * use as a [com.github.adriianh.core.domain.provider.MusicProvider].
     * Duration comes from Piped in **seconds**; it is converted to milliseconds here.
     */
    suspend fun searchTracks(query: String, limit: Int = 20): List<Track> {
        return try {
            val response = httpClient.get("$baseUrl/search") {
                parameter("q", query)
                parameter("filter", "music_songs")
            }.body<PipedSearchResponse>()

            response.items
                .filter { it.type == "stream" }
                .take(limit)
                .map { it.toDomain() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun normalize(text: String): String = text
        .lowercase()
        .replace(Regex("\\s*\\(.*?\\)"), "")
        .replace(Regex("\\s*\\[.*?]"), "")
        .replace(Regex("[^a-z0-9 ]"), "")
        .trim()
}