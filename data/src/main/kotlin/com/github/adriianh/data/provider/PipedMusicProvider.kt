package com.github.adriianh.data.provider

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.data.remote.piped.PipedApiClient

/**
 * MusicProvider backed by Piped's YouTube Music search (`music_songs` filter).
 *
 * Results are songs found on YouTube Music, which covers a broader catalogue than
 * iTunes — including regional/independent releases not available on the iTunes Store
 *
 * Track IDs are prefixed with `piped:` to distinguish them from iTunes/Spotify IDs.
 * Artwork is not returned by Piped's search endpoint; it is left null so the TUI
 * falls back gracefully.
 */
class PipedMusicProvider(
    private val apiClient: PipedApiClient
) : MusicProvider {

    override suspend fun search(query: String): List<Track> =
        apiClient.searchTracks(query, limit = 20)

    override suspend fun searchAll(query: String): List<Track> =
        apiClient.searchTracks(query, limit = 100)

    /** Piped results carry the video ID as their source; track detail lookup is not supported. */
    override suspend fun getTrack(id: String): Track? = null
}