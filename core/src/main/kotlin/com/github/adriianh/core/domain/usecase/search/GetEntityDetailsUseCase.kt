package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.MusicProvider

class GetEntityDetailsUseCase(
    private val musicProvider: MusicProvider
) {
    suspend operator fun invoke(entity: SearchResult): SearchResult {
        return when (entity) {
            is SearchResult.Album -> {
                val details = musicProvider.getAlbumDetails(entity.id) ?: return entity
                details.copy(
                    author = if (details.author == "Unknown") entity.author else details.author,
                    description = details.description ?: entity.description
                )
            }

            is SearchResult.Artist -> {
                val details = musicProvider.getArtistDetails(entity.id) ?: return entity
                details.copy(
                    description = details.description ?: entity.description
                )
            }

            is SearchResult.Playlist -> {
                val details = musicProvider.getPlaylistDetails(entity.id) ?: return entity
                details.copy(
                    author = if (details.author == "Unknown") entity.author else details.author,
                    description = details.description ?: entity.description,
                    trackCount = details.trackCount ?: entity.trackCount
                )
            }
            is SearchResult.Song -> entity
        }
    }
}