package com.github.adriianh.data.provider

import com.github.adriianh.core.domain.model.SimilarTrack
import com.github.adriianh.core.domain.provider.DiscoveryProvider

/**
 * Chains multiple [DiscoveryProvider]s: tries each in order and returns the first
 * non-empty result. Last.fm is the primary source; Deezer is the fallback.
 */
class CompositeDiscoveryProvider(
    private val providers: List<DiscoveryProvider>,
) : DiscoveryProvider {

    override suspend fun getSimilarTracks(artist: String, title: String): List<SimilarTrack> {
        for (provider in providers) {
            val result = runCatching { provider.getSimilarTracks(artist, title) }
                .getOrElse { emptyList() }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    override suspend fun getGenres(artist: String): List<String> {
        for (provider in providers) {
            val result = runCatching { provider.getGenres(artist) }
                .getOrElse { emptyList() }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }
}