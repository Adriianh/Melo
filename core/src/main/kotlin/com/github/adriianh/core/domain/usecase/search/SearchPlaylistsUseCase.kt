package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.repository.MusicRepository

class SearchPlaylistsUseCase(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(query: String): List<SearchResult.Playlist> {
        if (query.isBlank()) return emptyList()
        return repository.searchPlaylists(query)
    }
}