package com.github.adriianh.core.domain.usecase.search
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.MusicProvider
class GetEntityDetailsUseCase(
    private val musicProvider: MusicProvider
) {
    suspend operator fun invoke(entity: SearchResult): SearchResult {
        return when (entity) {
            is SearchResult.Album -> musicProvider.getAlbumDetails(entity.id) ?: entity
            is SearchResult.Artist -> musicProvider.getArtistDetails(entity.id) ?: entity
            is SearchResult.Playlist -> musicProvider.getPlaylistDetails(entity.id) ?: entity
            is SearchResult.Song -> entity
        }
    }
}