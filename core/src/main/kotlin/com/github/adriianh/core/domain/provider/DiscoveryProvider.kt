package com.github.adriianh.core.domain.provider

import com.github.adriianh.core.domain.model.SimilarTrack

interface DiscoveryProvider {
    suspend fun getSimilarTracks(artist: String, title: String): List<SimilarTrack>
    suspend fun getGenres(artist: String): List<String>
}