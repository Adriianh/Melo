package com.github.adriianh.data.provider.music

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.models.YouTubeClient

/**
 * MusicProvider backed by InnerTube API.
 *
 * Track IDs are prefixed with `piped:` to maintain compatibility with existing
 * databases and play queues, though they actually come from InnerTube now.
 * Acts as a drop-in replacement for PipedMusicProvider.
 */
class InnerTubeMusicProvider(
    private val fallback: MusicProvider? = null
) : MusicProvider {

    override suspend fun search(query: String): List<Track> {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()

        return result?.items?.filterIsInstance<com.github.adriianh.innertube.models.SongItem>()
            ?.map { item ->
                Track(
                    id = "piped:${item.id}",
                    title = item.title,
                    artist = item.artists.firstOrNull()?.name ?: "Unknown",
                    durationMs = item.duration?.times(1000L) ?: 0L,
                    album = item.album?.name ?: "",
                    genres = emptyList(),
                    artworkUrl = item.thumbnail,
                    sourceId = item.id
                )
            }
            ?: (fallback?.search(query) ?: emptyList())
    }

    override suspend fun searchAlbums(query: String): List<SearchResult.Album> {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
        return result?.items?.filterIsInstance<com.github.adriianh.innertube.models.AlbumItem>()?.map { item ->
            SearchResult.Album(
                id = item.playlistId,
                title = item.title,
                author = item.artists?.joinToString(", ") { it.name } ?: "Unknown",
                year = item.year?.toString(),
                artworkUrl = item.thumbnail
            )
        } ?: fallback?.searchAlbums(query) ?: emptyList()
    }

    override suspend fun searchArtists(query: String): List<SearchResult.Artist> {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
        return result?.items?.filterIsInstance<com.github.adriianh.innertube.models.ArtistItem>()?.map { item ->
            SearchResult.Artist(
                id = item.id,
                name = item.title,
                artworkUrl = item.thumbnail
            )
        } ?: fallback?.searchArtists(query) ?: emptyList()
    }

    override suspend fun searchPlaylists(query: String): List<SearchResult.Playlist> {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST).getOrNull()
        return result?.items?.filterIsInstance<com.github.adriianh.innertube.models.PlaylistItem>()?.map { item ->
            SearchResult.Playlist(
                id = item.id,
                title = item.title,
                author = item.author?.name ?: "Unknown",
                trackCount = item.songCountText?.filter { it.isDigit() }?.toIntOrNull(),
                artworkUrl = item.thumbnail
            )
        } ?: fallback?.searchPlaylists(query) ?: emptyList()
    }

    override suspend fun searchAll(query: String): List<Track> {
        // We could use continuation to get more results, but for now we delegate to search()
        // just like the old provider did with a different limit
        return search(query)
    }

    override suspend fun getTrack(id: String): Track? {
        val videoId = id.removePrefix("piped:")
        if (videoId.isBlank()) return null

        return try {
            val response = YouTube.player(videoId, null, YouTubeClient.WEB_REMIX).getOrNull()

            if (response != null) {
                val details = response.videoDetails
                if (details != null) {
                    Track(
                        id = "piped:$videoId",
                        title = details.title,
                        artist = details.author,
                        durationMs = details.lengthSeconds.toLong() * 1000L,
                        album = "",
                        genres = emptyList(),
                        artworkUrl = details.thumbnail.thumbnails.lastOrNull()?.url,
                        sourceId = videoId
                    )
                } else {
                    fallback?.getTrack(id)
                }
            } else {
                fallback?.getTrack(id)
            }
        } catch (_: Exception) {
            fallback?.getTrack(id)
        }
    }
}