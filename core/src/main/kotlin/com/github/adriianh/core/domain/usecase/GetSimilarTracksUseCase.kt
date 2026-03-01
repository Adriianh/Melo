package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.model.SimilarTrack
import com.github.adriianh.core.domain.repository.DiscoveryRepository

class GetSimilarTracksUseCase(
    private val repository: DiscoveryRepository
) {
    suspend operator fun invoke(artist: String, title: String): List<SimilarTrack> {
        if (artist.isBlank() || title.isBlank()) return emptyList()
        return repository.getSimilarTracks(artist, title)
            .sortedByDescending { it.match }
    }
}