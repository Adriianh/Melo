package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.MusicProvider

class GetEntityDetailsUseCase(
    private val musicProvider: MusicProvider,
) {
    suspend operator fun invoke(entity: SearchResult): SearchResult {
        return when (entity) {
            is SearchResult.Album -> resolveAlbumDetails(entity)
            is SearchResult.Artist -> resolveArtistDetails(entity)
            is SearchResult.Playlist -> {
                val details = musicProvider.getPlaylistDetails(entity.id) ?: return entity
                val isGeneric = { s: String? ->
                    val c = s?.trim()?.lowercase() ?: ""
                    c.isBlank() ||
                        c == "unknown" ||
                        c == "desconocido" ||
                        c == "playlist" ||
                        c == "lista de reproducción" ||
                        c == "lista de reproduccion" ||
                        c == "álbum" ||
                        c == "album" ||
                        c == "youtube music"
                }
                val resolvedAuthor =
                    details.author.takeIf { !isGeneric(it) }
                        ?: entity.author.takeIf { !isGeneric(it) }
                        ?: details.author.takeIf { it.isNotBlank() }
                        ?: entity.author.takeIf { it.isNotBlank() }
                        ?: "YouTube Music"

                details.copy(
                    title =
                        details.title.takeIf { it.isNotBlank() }
                            ?: entity.title.takeIf { it.isNotBlank() }
                            ?: "Playlist",
                    author = resolvedAuthor,
                    description = details.description ?: entity.description,
                    trackCount = details.trackCount ?: entity.trackCount,
                    artworkUrl = details.artworkUrl?.takeIf { it.isNotBlank() } ?: entity.artworkUrl,
                )
            }

            is SearchResult.Song -> entity
        }
    }

    private suspend fun resolveAlbumDetails(entity: SearchResult.Album): SearchResult.Album {
        val directDetails =
            if (isDirectAlbumBrowseId(entity.id) || entity.author.isBlank()) {
                musicProvider.getAlbumDetails(entity.id)
            } else {
                null
            }
        val details =
            directDetails
                ?: albumSearchQuery(entity).takeIf { it.isNotBlank() }?.let { query ->
                    findMatchingAlbum(query, entity.title.ifBlank { entity.id }, entity.author)
                }
                ?: return entity
        return details.copy(
            title =
                details.title.takeIf { it.isNotBlank() }
                    ?: entity.title.takeIf { it.isNotBlank() }
                    ?: details.songs
                        ?.firstOrNull()
                        ?.album
                        ?.takeIf { it.isNotBlank() }
                    ?: "Álbum",
            author = if (details.author == "Unknown" && entity.author.isNotBlank()) entity.author else details.author,
            description = details.description ?: entity.description,
            artworkUrl = details.artworkUrl?.takeIf { it.isNotBlank() } ?: entity.artworkUrl,
        )
    }

    private fun isDirectAlbumBrowseId(id: String): Boolean =
        id.startsWith("MPRE") ||
            id.startsWith("OLAK") ||
            id.startsWith("piped:") ||
            id.startsWith("itunes:")

    private suspend fun findMatchingAlbum(
        query: String,
        expectedTitle: String,
        expectedAuthor: String,
    ): SearchResult.Album? {
        val authorTokens = parseAuthorTokens(expectedAuthor)
        val cleanTitle = stripTitleAnnotations(expectedTitle)
        val queries =
            listOfNotNull(
                query,
                authorTokens
                    .firstOrNull()
                    ?.let { "$it $cleanTitle" }
                    ?.takeIf { cleanTitle.isNotBlank() && it != query },
                expectedTitle.takeIf { it.isNotBlank() && it != query && authorTokens.isNotEmpty() },
            ).distinct()

        for (candidateQuery in queries) {
            val candidates = musicProvider.searchAlbums(candidateQuery)
            val byAuthor = filterByAuthor(candidates, authorTokens)
            val matched =
                byAuthor.firstOrNull { it.title.equals(expectedTitle, ignoreCase = true) }
                    ?: byAuthor.firstOrNull { it.title.contains(cleanTitle, ignoreCase = true) }
                    ?: byAuthor.firstOrNull()
            if (matched != null) {
                return musicProvider.getAlbumDetails(matched.id) ?: matched
            }
        }
        return null
    }

    private fun parseAuthorTokens(expectedAuthor: String): List<String> =
        expectedAuthor
            .split(",", "&", "feat.", "ft.", " x ", " X ")
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun stripTitleAnnotations(title: String): String =
        title
            .replace(Regex("\\s*[\\[(].*?[])]"), "")
            .trim()

    private fun filterByAuthor(
        candidates: List<SearchResult.Album>,
        authorTokens: List<String>,
    ): List<SearchResult.Album> {
        if (authorTokens.isEmpty()) return candidates
        return candidates.filter { album ->
            album.author.isNotBlank() &&
                !album.author.equals("Unknown", ignoreCase = true) &&
                authorTokens.any { token ->
                    album.author.contains(token, ignoreCase = true) ||
                        token.contains(album.author, ignoreCase = true)
                }
        }
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
            name =
                details.name.takeIf { it.isNotBlank() }
                    ?: entity.name.takeIf { it.isNotBlank() } ?: "Artista",
            description = details.description ?: entity.description,
            artworkUrl = details.artworkUrl?.takeIf { it.isNotBlank() } ?: entity.artworkUrl,
        )
    }
}
