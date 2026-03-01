package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.SimilarTrack

interface DiscoveryRepository {
        suspend fun getSimilarTracks(artist: String, title: String): List<SimilarTrack>
}