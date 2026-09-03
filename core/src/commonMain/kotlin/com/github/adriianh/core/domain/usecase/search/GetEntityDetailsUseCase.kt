package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.MusicProvider

class GetEntityDetailsUseCase(
    private val musicProvider: MusicProvider
) {
    suspend operator fun invoke(entity: SearchResult): SearchResult {
        return when (entity) {
            is SearchResult.Album -> {
                var details = musicProvider.getAlbumDetails(entity.id)
                if (details == null && entity.title.isNotBlank()) {
                    val query =
                        if (entity.author.isNotBlank()) "${entity.author} ${entity.title}" else entity.title
                    details = musicProvider.getAlbumDetails(query)
                }
                if (details == null) return entity
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
                val isGeneric = { s: String? ->
                    val c = s?.trim()?.lowercase() ?: ""
                    c.isBlank() || c == "unknown" || c == "desconocido" || c == "playlist" ||
                            c == "lista de reproducción" || c == "lista de reproduccion" ||
                            c == "álbum" || c == "album" || c == "youtube music"
                }
                val resolvedAuthor = details.author.takeIf { !isGeneric(it) }
                    ?: entity.author.takeIf { !isGeneric(it) }
                    ?: details.author.takeIf { it.isNotBlank() }
                    ?: entity.author.takeIf { it.isNotBlank() }
                    ?: "YouTube Music"

                details.copy(
                    title = details.title.takeIf { it.isNotBlank() }
                        ?: entity.title.takeIf { it.isNotBlank() }
                        ?: "Playlist",
                    author = resolvedAuthor,
                    description = details.description ?: entity.description,
                    trackCount = details.trackCount ?: entity.trackCount,
                    artworkUrl = details.artworkUrl?.takeIf { it.isNotBlank() } ?: entity.artworkUrl
                )
            }

            is SearchResult.Song -> entity
        }
    }
}