package com.github.adriianh.data.remote.deezer

import com.github.adriianh.core.domain.model.SimilarTrack
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.utils.io.CancellationException

private const val BASE_URL = "https://api.deezer.com"

/**
 * Client for the Deezer public API.
 * No authentication required — all endpoints used here are freely accessible.
 */
class DeezerApiClient(
    private val httpClient: HttpClient,
) {

    /**
     * Resolves the Deezer artist ID for the given [artistName].
     * Returns null if the artist cannot be found.
     */
    suspend fun findArtistId(artistName: String): Long? {
        return try {
            val response = httpClient.get("$BASE_URL/search/artist") {
                parameter("q", artistName)
                parameter("limit", 1)
            }.body<DeezerSearchResponse<DeezerArtistDto>>()
            response.data.firstOrNull()?.id
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns a radio-style mix for the given artist — tracks from that artist and
     * similar artists curated by Deezer. This is the primary source for similar track discovery.
     */
    suspend fun getArtistRadio(artistId: Long, limit: Int = 25): List<SimilarTrack> {
        return try {
            val response = httpClient.get("$BASE_URL/artist/$artistId/radio") {
                parameter("limit", limit)
            }.body<DeezerSearchResponse<DeezerTrackDto>>()

            response.data
                .filter { it.duration in 30..600 }
                .map { track ->
                    SimilarTrack(
                        title  = track.title,
                        artist = track.artist.name,
                        // Deezer doesn't provide a similarity score; rank is used as a proxy,
                        // normalised into [0.0, 1.0] relative to a practical maximum of 1_000_000.
                        match  = (track.rank.coerceAtMost(1_000_000L) / 1_000_000.0),
                    )
                }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Returns the top tracks for the given artist.
     * Used as a secondary fallback when [getArtistRadio] returns no results.
     */
    suspend fun getArtistTopTracks(artistId: Long, limit: Int = 20): List<SimilarTrack> {
        return try {
            val response = httpClient.get("$BASE_URL/artist/$artistId/top") {
                parameter("limit", limit)
            }.body<DeezerSearchResponse<DeezerTrackDto>>()

            response.data
                .filter { it.duration in 30..600 }
                .mapIndexed { index, track ->
                    SimilarTrack(
                        title  = track.title,
                        artist = track.artist.name,
                        // Top tracks are ordered by popularity; invert index for a descending score.
                        match  = 1.0 - (index.toDouble() / response.data.size.coerceAtLeast(1)),
                    )
                }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * High-level helper: given an artist name, finds its Deezer ID and returns a radio mix.
     * Returns an empty list if the artist cannot be resolved or any request fails.
     */
    suspend fun getSimilarTracks(artist: String, limit: Int = 25): List<SimilarTrack> {
        val artistId = findArtistId(artist) ?: return emptyList()
        val radio = getArtistRadio(artistId, limit)
        if (radio.isNotEmpty()) return radio
        return getArtistTopTracks(artistId, limit)
    }
}