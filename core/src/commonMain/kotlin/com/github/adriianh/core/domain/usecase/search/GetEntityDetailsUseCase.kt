package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.MusicProvider

class GetEntityDetailsUseCase(
    private val musicProvider: MusicProvider
) {
    suspend operator fun invoke(entity: SearchResult): SearchResult {
        return when (entity) {
            is SearchResult.Album -> resolveAlbumDetails(entity)
            is SearchResult.Artist -> resolveArtistDetails(entity)
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

    private suspend fun resolveAlbumDetails(entity: SearchResult.Album): SearchResult.Album {
        val details =
            musicProvider.getAlbumDetails(entity.id)
                ?: albumSearchQuery(entity).takeIf { it.isNotBlank() }?.let { query ->
                    findMatchingAlbum(query, entity.author)
                }
                ?: return entity
        return details.copy(
            title = details.title.takeIf { it.isNotBlank() }
                ?: entity.title.takeIf { it.isNotBlank() }
                ?: details.songs?.firstOrNull()?.album?.takeIf { it.isNotBlank() }
                ?: "Álbum",
            author = if (details.author == "Unknown" && entity.author.isNotBlank()) entity.author else details.author,
            description = details.description ?: entity.description,
            artworkUrl = details.artworkUrl?.takeIf { it.isNotBlank() } ?: entity.artworkUrl
        )
    }

    private suspend fun findMatchingAlbum(
        query: String,
        expectedAuthor: String,
    ): SearchResult.Album? {
        val candidates = musicProvider.searchAlbums(query)
        val matched =
            if (expectedAuthor.isNotBlank()) {
                candidates.firstOrNull { album ->
                    album.author.contains(expectedAuthor, ignoreCase = true) ||
                        expectedAuthor.contains(album.author, ignoreCase = true)
                }
            } else {
                candidates.firstOrNull()
            }
        return matched?.let { musicProvider.getAlbumDetails(it.id) ?: it }
    }

    private fun albumSearchQuery(entity: SearchResult.Album): String =
        when {
            entity.title.isNotBlank() && entity.author.isNotBlank() -> "${entity.author} ${entity.title}"
            entity.title.isNotBlank() -> entity.title
            else -> entity.id
        }

    private suspend fun resolveArtistDetails(entity: SearchResult.Artist): SearchResult.Artist {
        var details = musicProvider.getArtistDetails(entity.id)
        if (details == null) {
            val query = entity.name.takeIf { it.isNotBlank() } ?: entity.id
            if (query.isNotBlank()) {
                val found = musicProvider.searchArtists(query).firstOrNull()
                details = found?.let { musicProvider.getArtistDetails(it.id) ?: it }
            }
        }
        if (details == null) return entity
        return details.copy(
            name = details.name.takeIf { it.isNotBlank() }
                ?: entity.name.takeIf { it.isNotBlank() } ?: "Artista",
            description = details.description ?: entity.description,
            artworkUrl = details.artworkUrl?.takeIf { it.isNotBlank() } ?: entity.artworkUrl
        )
    }
}