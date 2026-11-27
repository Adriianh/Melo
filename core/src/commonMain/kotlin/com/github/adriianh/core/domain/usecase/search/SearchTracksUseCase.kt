package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchPage
import com.github.adriianh.core.domain.repository.MusicRepository

class SearchTracksUseCase(
    private val repository: MusicRepository,
) {
    suspend operator fun invoke(query: String): List<Track> {
        if (query.isBlank()) return emptyList()
        return repository.search(query)
    }

    suspend fun searchPage(
        query: String,
        continuation: String? = null,
    ): SearchPage<Track> {
        if (query.isBlank() && continuation.isNullOrBlank()) return SearchPage()
        return repository.searchTracksPage(query, continuation)
    }
}
