package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.repository.MusicRepository

class SearchAlbumsUseCase(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(query: String): List<SearchResult.Album> {
        if (query.isBlank()) return emptyList()
        return repository.searchAlbums(query)
    }
}