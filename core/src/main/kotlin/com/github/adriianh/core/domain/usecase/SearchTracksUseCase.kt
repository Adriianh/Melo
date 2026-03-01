package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.MusicRepository

class SearchTracksUseCase(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(query: String): List<Track> {
        if (query.isBlank()) return emptyList()
        return repository.search(query)
    }
}