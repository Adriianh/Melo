package com.github.adriianh.data.provider.music

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.models.AlbumItem
import com.github.adriianh.innertube.models.ArtistItem
import com.github.adriianh.innertube.models.PlaylistItem
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
        return result?.items?.filterIsInstance<AlbumItem>()?.map { item ->
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
        return result?.items?.filterIsInstance<ArtistItem>()?.map { item ->
            SearchResult.Artist(
                id = item.id,
                name = item.title,
                artworkUrl = item.thumbnail
            )
        } ?: fallback?.searchArtists(query) ?: emptyList()
    }

    override suspend fun searchPlaylists(query: String): List<SearchResult.Playlist> {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST).getOrNull()
        return result?.items?.filterIsInstance<PlaylistItem>()?.map { item ->
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

    override suspend fun getAlbumDetails(id: String): SearchResult.Album? {
        val result = YouTube.album(id).getOrNull() ?: return fallback?.getAlbumDetails(id)
        val albumItem = result.album
        val tracks = result.songs.map { song ->
            Track(
                id = "piped:${song.id}",
                title = song.title,
                artist = song.artists.firstOrNull()?.name ?: "Unknown",
                durationMs = song.duration?.times(1000L) ?: 0L,
                album = albumItem.title,
                genres = emptyList(),
                artworkUrl = song.thumbnail ?: albumItem.thumbnail,
                sourceId = song.id
            )
        }
        val otherVersions = result.otherVersions.map {
            SearchResult.Album(
                id = it.playlistId,
                title = it.title,
                author = it.artists?.joinToString(", ") { a -> a.name } ?: "Unknown",
                year = it.year?.toString(),
                artworkUrl = it.thumbnail
            )
        }
        return SearchResult.Album(
            id = albumItem.playlistId,
            title = albumItem.title,
            author = albumItem.artists?.joinToString(", ") { it.name } ?: "Unknown",
            year = albumItem.year?.toString(),
            artworkUrl = albumItem.thumbnail,
            songs = tracks,
            otherVersions = otherVersions
        )
    }

    override suspend fun getArtistDetails(id: String): SearchResult.Artist? {
        val result = YouTube.artist(id).getOrNull() ?: return fallback?.getArtistDetails(id)
        val topSongs = result.sections.find { it.title.equals("Songs", ignoreCase = true) }?.items?.filterIsInstance<com.github.adriianh.innertube.models.SongItem>()
        val tracks = topSongs?.map { song ->
            Track(
                id = "piped:${song.id}",
                title = song.title,
                artist = song.artists.firstOrNull()?.name ?: result.artist.title,
                durationMs = song.duration?.times(1000L) ?: 0L,
                album = song.album?.name ?: "",
                genres = emptyList(),
                artworkUrl = song.thumbnail ?: result.artist.thumbnail,
                sourceId = song.id
            )
        }
        return SearchResult.Artist(
            id = result.artist.id,
            name = result.artist.title,
            artworkUrl = result.artist.thumbnail,
            description = result.description,
            subscriberCountText = result.subscriberCountText,
            topSongs = tracks
        )
    }

    override suspend fun getPlaylistDetails(id: String): SearchResult.Playlist? {
        val result = YouTube.playlist(id).getOrNull() ?: return fallback?.getPlaylistDetails(id)

        return SearchResult.Playlist(
            id = result.playlist.id,
            title = result.playlist.title,
            author = result.playlist.author?.name ?: "Unknown",
            trackCount = result.songs.size,
            artworkUrl = result.playlist.thumbnail,
            description = null
        )
    }
}