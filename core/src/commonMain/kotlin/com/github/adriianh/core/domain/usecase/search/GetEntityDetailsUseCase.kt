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
                    title = details.title.takeIf { it.isNotBlank() }
                        ?: entity.title.takeIf { it.isNotBlank() }
                        ?: details.songs?.firstOrNull()?.album?.takeIf { it.isNotBlank() }
                        ?: "Álbum",
                    author = if (details.author == "Unknown" && entity.author.isNotBlank()) entity.author else details.author,
                    description = details.description ?: entity.description,
                    artworkUrl = details.artworkUrl?.takeIf { it.isNotBlank() } ?: entity.artworkUrl
                )
            }

            is SearchResult.Artist -> {
                val details = musicProvider.getArtistDetails(entity.id) ?: return entity
                details.copy(
                    name = details.name.takeIf { it.isNotBlank() }
                        ?: entity.name.takeIf { it.isNotBlank() } ?: "Artista",
                    description = details.description ?: entity.description,
                    artworkUrl = details.artworkUrl?.takeIf { it.isNotBlank() } ?: entity.artworkUrl
                )
            }

            is SearchResult.Playlist -> {
                val details = musicProvider.getPlaylistDetails(entity.id) ?: return entity
                details.copy(
                    title = details.title.takeIf { it.isNotBlank() }
                        ?: entity.title.takeIf { it.isNotBlank() }
                        ?: "Playlist",
                    author = if (details.author == "Unknown" && entity.author.isNotBlank()) entity.author else details.author,
                    description = details.description ?: entity.description,
                    trackCount = details.trackCount ?: entity.trackCount,
                    artworkUrl = details.artworkUrl?.takeIf { it.isNotBlank() } ?: entity.artworkUrl
                )
            }

            is SearchResult.Song -> entity
        }
    }
}