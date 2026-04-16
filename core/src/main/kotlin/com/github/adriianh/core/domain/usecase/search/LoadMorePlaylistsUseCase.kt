package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.repository.MusicRepository

class LoadMorePlaylistsUseCase(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(query: String, offset: Int): List<SearchResult.Playlist> {
        if (query.isBlank()) return emptyList()
        return repository.loadMorePlaylists(query, offset)
    }

    fun hasMore(offset: Int): Boolean = repository.hasMorePlaylists(offset)
}