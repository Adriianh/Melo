package com.github.adriianh.data.provider.music

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.models.AlbumItem
import com.github.adriianh.innertube.models.ArtistItem
import com.github.adriianh.innertube.models.PlaylistItem
import com.github.adriianh.innertube.models.SongItem
import com.github.adriianh.innertube.models.YTItem
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

        return result?.items?.filterIsInstance<SongItem>()
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
                id = item.browseId,
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
        val result =
            YouTube.search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST).getOrNull()
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
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            ?: return fallback?.searchAll(query) ?: emptyList()
        val pages = mutableListOf<Track>()
        var continuation: String? = result.continuation

        fun mapItems(items: List<YTItem>) {
            pages.addAll(items.filterIsInstance<SongItem>().map { item ->
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
            })
        }

        mapItems(result.items)
        var count = 0
        while (continuation != null && count < 3) {
            val next = YouTube.searchContinuation(continuation).getOrNull() ?: break
            mapItems(next.items)
            continuation = next.continuation
            count++
        }
        return pages.ifEmpty { fallback?.searchAll(query) ?: emptyList() }
    }

    override suspend fun searchAllAlbums(query: String): List<SearchResult.Album> {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
            ?: return fallback?.searchAllAlbums(query) ?: emptyList()
        val pages = mutableListOf<SearchResult.Album>()
        var continuation: String? = result.continuation

        fun mapItems(items: List<YTItem>) {
            pages.addAll(items.filterIsInstance<AlbumItem>().map { item ->
                SearchResult.Album(
                    id = item.browseId,
                    title = item.title,
                    author = item.artists?.joinToString(", ") { it.name } ?: "Unknown",
                    year = item.year?.toString(),
                    artworkUrl = item.thumbnail
                )
            })
        }

        mapItems(result.items)
        var count = 0
        while (continuation != null && count < 3) {
            val next = YouTube.searchContinuation(continuation).getOrNull() ?: break
            mapItems(next.items)
            continuation = next.continuation
            count++
        }
        return pages.ifEmpty { fallback?.searchAllAlbums(query) ?: emptyList() }
    }

    override suspend fun searchAllArtists(query: String): List<SearchResult.Artist> {
        val result = YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
            ?: return fallback?.searchAllArtists(query) ?: emptyList()
        val pages = mutableListOf<SearchResult.Artist>()
        var continuation: String? = result.continuation

        fun mapItems(items: List<YTItem>) {
            pages.addAll(items.filterIsInstance<ArtistItem>().map { item ->
                SearchResult.Artist(
                    id = item.id,
                    name = item.title,
                    artworkUrl = item.thumbnail
                )
            })
        }

        mapItems(result.items)
        var count = 0
        while (continuation != null && count < 3) {
            val next = YouTube.searchContinuation(continuation).getOrNull() ?: break
            mapItems(next.items)
            continuation = next.continuation
            count++
        }
        return pages.ifEmpty { fallback?.searchAllArtists(query) ?: emptyList() }
    }

    override suspend fun searchAllPlaylists(query: String): List<SearchResult.Playlist> {
        val result =
            YouTube.search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST).getOrNull()
                ?: return fallback?.searchAllPlaylists(query) ?: emptyList()
        val pages = mutableListOf<SearchResult.Playlist>()
        var continuation: String? = result.continuation

        fun mapItems(items: List<YTItem>) {
            pages.addAll(items.filterIsInstance<PlaylistItem>().map { item ->
                SearchResult.Playlist(
                    id = item.id,
                    title = item.title,
                    author = item.author?.name ?: "Unknown",
                    trackCount = item.songCountText?.filter { it.isDigit() }?.toIntOrNull(),
                    artworkUrl = item.thumbnail
                )
            })
        }

        mapItems(result.items)
        var count = 0
        while (continuation != null && count < 3) {
            val next = YouTube.searchContinuation(continuation).getOrNull() ?: break
            mapItems(next.items)
            continuation = next.continuation
            count++
        }
        return pages.ifEmpty { fallback?.searchAllPlaylists(query) ?: emptyList() }
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
        val response = YouTube.album(id)
        val result = response.getOrNull() ?: return fallback?.getAlbumDetails(id)
        val albumItem = result.album
        val tracks = result.songs.map { song ->
            Track(
                id = "piped:${song.id}",
                title = song.title,
                artist = song.artists.firstOrNull()?.name ?: "Unknown",
                durationMs = song.duration?.times(1000L) ?: 0L,
                album = albumItem.title,
                genres = emptyList(),
                artworkUrl = song.thumbnail,
                sourceId = song.id
            )
        }
        val otherVersions = result.otherVersions.map {
            SearchResult.Album(
                id = it.browseId,
                title = it.title,
                author = it.artists?.joinToString(", ") { a -> a.name } ?: "Unknown",
                year = it.year?.toString(),
                artworkUrl = it.thumbnail
            )
        }

        return SearchResult.Album(
            id = albumItem.browseId,
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
        val sections = result.sections.map { section ->
            val mappedItems = section.items.map { item ->
                when (item) {
                    is SongItem -> SearchResult.Song(Track(
                        id = "piped:${item.id}",
                        title = item.title,
                        artist = item.artists.firstOrNull()?.name ?: result.artist.title,
                        durationMs = item.duration?.times(1000L) ?: 0L,
                        album = item.album?.name ?: "",
                        genres = emptyList(),
                        artworkUrl = item.thumbnail,
                        sourceId = item.id
                    ))
                    is AlbumItem -> SearchResult.Album(
                        id = item.browseId,
                        title = item.title,
                        author = item.artists?.joinToString(", ") { it.name } ?: result.artist.title,
                        year = item.year?.toString(),
                        artworkUrl = item.thumbnail
                    )
                    is ArtistItem -> SearchResult.Artist(
                        id = item.id,
                        name = item.title,
                        artworkUrl = item.thumbnail
                    )
                    is PlaylistItem -> SearchResult.Playlist(
                        id = item.id,
                        title = item.title,
                        author = item.author?.name ?: result.artist.title,
                        trackCount = item.songCountText?.filter { it.isDigit() }?.toIntOrNull(),
                        artworkUrl = item.thumbnail
                    )
                }
            }
            SearchResult.ArtistSection(section.title, mappedItems)
        }

        val topSongs = result.sections.find {
            it.title.equals("Songs", ignoreCase = true) || it.title.equals("Top songs", ignoreCase = true)
        }?.items?.filterIsInstance<SongItem>()
        val tracks = topSongs?.map { song ->
            Track(
                id = "piped:${song.id}",
                title = song.title,
                artist = song.artists.firstOrNull()?.name ?: result.artist.title,
                durationMs = song.duration?.times(1000L) ?: 0L,
                album = song.album?.name ?: "",
                genres = emptyList(),
                artworkUrl = song.thumbnail,
                sourceId = song.id
            )
        }

        return SearchResult.Artist(
            id = result.artist.id,
            name = result.artist.title,
            artworkUrl = result.artist.thumbnail,
            description = result.description,
            subscriberCountText = result.subscriberCountText,
            monthlyListenerCount = result.monthlyListenerCount,
            topSongs = tracks,
            sections = sections
        )
    }

    override suspend fun getPlaylistDetails(id: String): SearchResult.Playlist? {
        val result = YouTube.playlist(id).getOrNull() ?: return fallback?.getPlaylistDetails(id)

        val tracks = result.songs.map { song ->
            val albumName = song.album?.name
            Track(
                id = "piped:${song.id}",
                title = song.title,
                artist = song.artists.firstOrNull()?.name ?: "Unknown",
                durationMs = song.duration?.times(1000L) ?: 0L,
                album = albumName ?: "",
                genres = emptyList(),
                artworkUrl = song.thumbnail,
                sourceId = song.id
            )
        }

        return SearchResult.Playlist(
            id = result.playlist.id,
            title = result.playlist.title,
            author = result.playlist.author?.name ?: "Unknown",
            trackCount = result.songs.size,
            artworkUrl = result.playlist.thumbnail,
            songs = tracks,
            description = null
        )
    }
}