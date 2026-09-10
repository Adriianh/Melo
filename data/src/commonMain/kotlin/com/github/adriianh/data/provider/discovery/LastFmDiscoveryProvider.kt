package com.github.adriianh.data.provider.discovery

import com.github.adriianh.core.domain.model.SimilarTrack
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import com.github.adriianh.data.remote.lastfm.LastFmApiClient

class LastFmDiscoveryProvider(
    private val apiClient: LastFmApiClient
) : DiscoveryProvider {

    override suspend fun getSimilarTracks(artist: String, title: String, limit: Int): List<SimilarTrack> {
        return apiClient.getSimilarTracks(artist, title, limit)
    }

    override suspend fun getGenres(artist: String): List<String> {
        return apiClient.getTopTags(artist)
    }
}