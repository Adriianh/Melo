package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.repository.MusicRepository

class LoadMoreArtistsUseCase(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(query: String, offset: Int): List<SearchResult.Artist> {
        if (query.isBlank()) return emptyList()
        return repository.loadMoreArtists(query, offset)
    }

    fun hasMore(offset: Int): Boolean = repository.hasMoreArtists(offset)
}