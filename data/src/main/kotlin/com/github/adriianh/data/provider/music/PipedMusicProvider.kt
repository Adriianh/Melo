package com.github.adriianh.data.provider

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.data.remote.piped.PipedApiClient

/**
 * MusicProvider backed by Piped's YouTube Music search (`music_songs` filter).
 *
 * Track IDs are prefixed with `piped:` to distinguish them from iTunes/Spotify IDs.
 * Full track metadata (including artwork) is fetched via the /streams endpoint on
 * [getTrack].
 */
class PipedMusicProvider(
    private val apiClient: PipedApiClient
) : MusicProvider {

    override suspend fun search(query: String): List<Track> =
        apiClient.searchTracks(query, limit = 20)

    override suspend fun searchAll(query: String): List<Track> =
        apiClient.searchTracks(query, limit = 100)

    override suspend fun getTrack(id: String): Track? {
        val videoId = id.removePrefix("piped:")
        if (videoId.isBlank()) return null
        return apiClient.getTrackDetails(videoId)
    }
}