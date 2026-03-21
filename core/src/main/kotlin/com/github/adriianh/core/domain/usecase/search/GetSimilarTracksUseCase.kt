package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.SimilarTrack
import com.github.adriianh.core.domain.repository.DiscoveryRepository

class GetSimilarTracksUseCase(
    private val repository: DiscoveryRepository
) {
    suspend operator fun invoke(artist: String, title: String, limit: Int = 50): List<SimilarTrack> {
        if (artist.isBlank() || title.isBlank()) return emptyList()
        return repository.getSimilarTracks(artist, title, limit)
            .sortedByDescending { it.match }
    }
}