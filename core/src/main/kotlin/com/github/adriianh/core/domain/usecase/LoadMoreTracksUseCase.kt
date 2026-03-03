package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.MusicRepository

class LoadMoreTracksUseCase(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(query: String, offset: Int): List<Track> {
        if (query.isBlank()) return emptyList()
        return repository.loadMore(query, offset)
    }

    fun hasMore(offset: Int): Boolean = repository.hasMore(offset)
}