package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.SimilarTrack
import com.github.adriianh.core.domain.repository.DiscoveryRepository
import com.github.adriianh.core.domain.provider.DiscoveryProvider

class DiscoveryRepositoryImpl(
    private val discoveryProvider: DiscoveryProvider
) : DiscoveryRepository {

    override suspend fun getSimilarTracks(artist: String, title: String): List<SimilarTrack> {
        return discoveryProvider.getSimilarTracks(artist, title)
    }
}