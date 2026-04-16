package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.provider.MusicProvider

class GetSearchSuggestionsUseCase(
    private val musicProvider: MusicProvider
) {
    suspend operator fun invoke(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return musicProvider.getSearchSuggestions(query)
    }
}