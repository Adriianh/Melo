package com.github.adriianh.data.remote.itunes

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class ItunesApiClient(
    private val httpClient: HttpClient
) {

    suspend fun search(query: String, limit: Int = 20): List<ItunesTrackDto> {
        return try {
            httpClient.get("https://itunes.apple.com/search") {
                parameter("term", query)
                parameter("media", "music")
                parameter("entity", "song")
                parameter("limit", limit)
            }.body<ItunesSearchResponse>().results
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getTrack(id: String): ItunesTrackDto? {
        return try {
            httpClient.get("https://itunes.apple.com/lookup") {
                parameter("id", id)
                parameter("entity", "song")
            }.body<ItunesSearchResponse>().results.firstOrNull()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Searches iTunes for a track by title + artist and returns the best matching
     * artwork URL, or null if nothing is found. Used as a fallback to provide
     * artwork for tracks that come from providers without artwork (e.g. Piped).
     */
    suspend fun searchArtwork(title: String, artist: String): String? {
        return try {
            val results = httpClient.get("https://itunes.apple.com/search") {
                parameter("term", "$title $artist")
                parameter("media", "music")
                parameter("entity", "song")
                parameter("limit", 5)
            }.body<ItunesSearchResponse>().results

            val normalizedTitle  = title.lowercase().trim()
            val normalizedArtist = artist.lowercase().trim()

            val best = results.firstOrNull { dto ->
                dto.artistName.lowercase().contains(normalizedArtist) &&
                dto.trackName.lowercase().contains(normalizedTitle) &&
                dto.artworkUrl100 != null
            }

            best?.artworkUrl100?.replace("100x100bb", "300x300bb")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
