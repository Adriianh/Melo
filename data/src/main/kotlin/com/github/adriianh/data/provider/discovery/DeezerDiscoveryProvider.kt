package com.github.adriianh.data.provider.discovery

import com.github.adriianh.core.domain.model.SimilarTrack
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.data.remote.deezer.DeezerApiClient

/**
 * Discovery provider backed by the Deezer public API.
 * Used as a fallback when Last.fm is unavailable or returns no results.
 */
class DeezerDiscoveryProvider(
    private val apiClient: DeezerApiClient,
) : DiscoveryProvider {

    override suspend fun getSimilarTracks(artist: String, title: String): List<SimilarTrack> {
        return apiClient.getSimilarTracks(artist)
    }

    override suspend fun getGenres(artist: String): List<String> = emptyList()
}