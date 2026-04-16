package com.github.adriianh.data.provider.music

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.data.remote.piped.PipedApiClient

/**
 * MusicProvider backed by Piped's YouTube Music search (`music_songs` filter).
 *
 * Track IDs are prefixed with `piped:` to distinguish them from iTunes/Spotify IDs.
 * Full track metadata (including artwork) is fetched via the /streams endpoint on
 * [getTrack].
 */
class PipedMusicProvider(
    private val apiClient: PipedApiClient
) : MusicProvider {

    override suspend fun search(query: String): List<Track> =
        apiClient.searchTracks(query, limit = 20)

    override suspend fun searchAlbums(query: String): List<SearchResult.Album> {
        return apiClient.searchAlbums(query, limit = 20).map { item ->
            SearchResult.Album(
                id = item.url.substringAfter("list="),
                title = item.name.ifBlank { item.title },
                author = item.uploaderName,
                year = null,
                artworkUrl = item.thumbnail
            )
        }
    }

    override suspend fun searchArtists(query: String): List<SearchResult.Artist> {
        return apiClient.searchArtists(query, limit = 20).map { item ->
            SearchResult.Artist(
                id = item.url.substringAfter("channel/"),
                name = item.name.ifBlank { item.title },
                artworkUrl = item.thumbnail
            )
        }
    }

    override suspend fun searchPlaylists(query: String): List<SearchResult.Playlist> {
        return apiClient.searchPlaylists(query, limit = 20).map { item ->
            SearchResult.Playlist(
                id = item.url.substringAfter("list="),
                title = item.name.ifBlank { item.title },
                author = item.uploaderName,
                trackCount = if (item.videos > 0) item.videos else null,
                artworkUrl = item.thumbnail
            )
        }
    }

    override suspend fun searchAll(query: String): List<Track> =
        apiClient.searchTracks(query, limit = 100)

    override suspend fun searchAllAlbums(query: String): List<SearchResult.Album> {
        return apiClient.searchAlbums(query, limit = 100).map { item ->
            SearchResult.Album(
                id = item.url.substringAfter("list="),
                title = item.name.ifBlank { item.title },
                author = item.uploaderName,
                year = null,
                artworkUrl = item.thumbnail
            )
        }
    }

    override suspend fun searchAllArtists(query: String): List<SearchResult.Artist> {
        return apiClient.searchArtists(query, limit = 100).map { item ->
            SearchResult.Artist(
                id = item.url.substringAfter("channel/"),
                name = item.name.ifBlank { item.title },
                artworkUrl = item.thumbnail
            )
        }
    }

    override suspend fun searchAllPlaylists(query: String): List<SearchResult.Playlist> {
        return apiClient.searchPlaylists(query, limit = 100).map { item ->
            SearchResult.Playlist(
                id = item.url.substringAfter("list="),
                title = item.name.ifBlank { item.title },
                author = item.uploaderName,
                trackCount = if (item.videos > 0) item.videos else null,
                artworkUrl = item.thumbnail
            )
        }
    }

    override suspend fun getTrack(id: String): Track? {
        val videoId = id.removePrefix("piped:")
        if (videoId.isBlank()) return null
        return apiClient.getTrackDetails(videoId)
    }
}