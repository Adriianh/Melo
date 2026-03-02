package com.github.adriianh.data.remote.lastfm

import com.github.adriianh.core.domain.model.SimilarTrack
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class LastFmApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
) {

    suspend fun getTopTags(artist: String, limit: Int = 5): List<String> {
        return try {
            val response = httpClient.get("https://ws.audioscrobbler.com/2.0/") {
                parameter("method", "artist.getTopTags")
                parameter("artist", artist)
                parameter("api_key", apiKey)
                parameter("format", "json")
                parameter("limit", limit)
            }.body<LastFmTopTagsResponse>()

            response.toptags.tag.map { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSimilarTracks(artist: String, title: String, limit: Int = 10): List<SimilarTrack> {
        return try {
            val response = httpClient.get("https://ws.audioscrobbler.com/2.0/") {
                parameter("method", "track.getSimilar")
                parameter("track", title)
                parameter("artist", artist)
                parameter("api_key", apiKey)
                parameter("format", "json")
                parameter("limit", limit)
            }.body<LastFmSimilarTracksResponse>()

            response.similartracks.track.map {
                SimilarTrack(
                    title = it.name,
                    artist = it.artist.name,
                    match = it.match
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Error getting similar tracks: ${e.message}")
            emptyList()
        }
    }
}