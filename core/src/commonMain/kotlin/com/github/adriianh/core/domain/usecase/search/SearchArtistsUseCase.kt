package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.search.SearchPage
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.repository.MusicRepository

class SearchArtistsUseCase(
    private val repository: MusicRepository,
) {
    suspend operator fun invoke(query: String): List<SearchResult.Artist> {
        if (query.isBlank()) return emptyList()
        return repository.searchArtists(query)
    }

    suspend fun searchPage(
        query: String,
        continuation: String? = null,
    ): SearchPage<SearchResult.Artist> {
        if (query.isBlank() && continuation.isNullOrBlank()) return SearchPage()
        return repository.searchArtistsPage(query, continuation)
    }
}
