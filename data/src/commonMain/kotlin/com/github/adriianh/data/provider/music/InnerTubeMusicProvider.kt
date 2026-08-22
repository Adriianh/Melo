package com.github.adriianh.data.provider.music

import com.github.adriianh.core.domain.model.BrowseCategoryResult
import com.github.adriianh.core.domain.model.BrowseCategorySection
import com.github.adriianh.core.domain.model.HomeFeed
import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.MoodAndGenreCategory
import com.github.adriianh.core.domain.model.MoodAndGenreGroup
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.models.AlbumItem
import com.github.adriianh.innertube.models.ArtistItem
import com.github.adriianh.innertube.models.PlaylistItem
import com.github.adriianh.innertube.models.SongItem
import com.github.adriianh.innertube.models.WatchEndpoint
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

        val albumArtwork =
            albumItem.thumbnail.takeIf { it.isNotBlank() } ?: tracks.firstOrNull()?.artworkUrl
        val albumTitle = albumItem.title.takeIf { it.isNotBlank() }
            ?: tracks.firstOrNull()?.album?.takeIf { it.isNotBlank() }
            ?: "Álbum"

        return SearchResult.Album(
            id = albumItem.browseId,
            title = albumTitle,
            author = albumItem.artists?.joinToString(", ") { it.name } ?: "Unknown",
            year = albumItem.year?.toString(),
            artworkUrl = albumArtwork,
            songs = tracks,
            otherVersions = otherVersions
        )
    }

    override suspend fun getArtistDetails(id: String): SearchResult.Artist? {
        val result = YouTube.artist(id).getOrNull() ?: return fallback?.getArtistDetails(id)
        val sections = result.sections.map { section ->
            val mappedItems = section.items.map { item ->
                when (item) {
                    is SongItem -> SearchResult.Song(
                        Track(
                            id = "piped:${item.id}",
                            title = item.title,
                            artist = item.artists.firstOrNull()?.name ?: result.artist.title,
                            durationMs = item.duration?.times(1000L) ?: 0L,
                            album = item.album?.name ?: "",
                            genres = emptyList(),
                            artworkUrl = item.thumbnail,
                            sourceId = item.id
                        )
                    )

                    is AlbumItem -> SearchResult.Album(
                        id = item.browseId,
                        title = item.title,
                        author = item.artists?.joinToString(", ") { it.name }
                            ?: result.artist.title,
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
            it.title.equals("Songs", ignoreCase = true) || it.title.equals(
                "Top songs",
                ignoreCase = true
            )
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

        val artistArtwork =
            result.artist.thumbnail.takeIf { it.isNotBlank() } ?: tracks?.firstOrNull()?.artworkUrl

        return SearchResult.Artist(
            id = result.artist.id,
            name = result.artist.title,
            artworkUrl = artistArtwork,
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

        val playlistArtwork = result.playlist.thumbnail.takeIf { it.isNotBlank() }
            ?: tracks.firstOrNull()?.artworkUrl
        val playlistTitle = result.playlist.title.takeIf { it.isNotBlank() } ?: "Playlist"

        return SearchResult.Playlist(
            id = result.playlist.id,
            title = playlistTitle,
            author = result.playlist.author?.name ?: "Unknown",
            trackCount = result.songs.size,
            artworkUrl = playlistArtwork,
            songs = tracks,
            description = null
        )
    }

    override suspend fun getSearchSuggestions(query: String): List<String> {
        val remoteSuggestions = YouTube.searchSuggestions(query).getOrNull()?.queries
            ?: return fallback?.getSearchSuggestions(query) ?: emptyList()
        return remoteSuggestions
    }

    override suspend fun getHome(): List<HomeSection> = getHomeFeed().sections

    override suspend fun getHomeFeed(
        params: String?,
        continuation: String?
    ): HomeFeed {
        val homePage = YouTube.home(continuation = continuation, params = params).getOrNull()
            ?: return HomeFeed()

        val chips = homePage.chips.orEmpty().map { chip ->
            val title = chip.title
            com.github.adriianh.core.domain.model.HomeFeedChip(
                title = title,
                params = chip.endpoint?.params
            )
        }

        val sections = homePage.sections.map { section ->
            HomeSection(
                title = section.title,
                type = section.items.toHomeSectionType(),
                items = section.items.mapNotNull { mapYTItem(it) }
            )
        }

        return HomeFeed(
            chips = chips,
            sections = sections,
            continuation = homePage.continuation
        )
    }

    override suspend fun getExplore(): List<HomeSection> {
        val explorePage = YouTube.explore().getOrNull() ?: return emptyList()

        return explorePage.sections.map { section ->
            HomeSection(
                title = section.title,
                type = section.items.toHomeSectionType(),
                items = section.items.mapNotNull { mapYTItem(it) }
            )
        }
    }

    override suspend fun getCharts(): List<HomeSection> {
        return YouTube.charts().getOrNull().orEmpty().map { section ->
            HomeSection(
                title = section.title,
                type = section.items.toHomeSectionType(),
                items = section.items.mapNotNull { mapYTItem(it) }
            )
        }
    }

    override suspend fun getTrending(): List<Track> {
        val home = getHome()
        return home.find { it.title.contains("Trending", ignoreCase = true) }
            ?.items?.filterIsInstance<SearchResult.Song>()?.map { it.track }
            ?: emptyList()
    }

    override suspend fun getMoodAndGenres(): List<MoodAndGenreGroup> {
        return YouTube.moodAndGenres().getOrNull().orEmpty().map { group ->
            MoodAndGenreGroup(
                title = group.title,
                items = group.items.map { item ->
                    MoodAndGenreCategory(
                        title = item.title,
                        stripeColor = item.stripeColor,
                        browseId = item.endpoint.browseId,
                        params = item.endpoint.params,
                    )
                }
            )
        }
    }

    override suspend fun getRadio(videoId: String): List<Track> {
        val cleanId = videoId.removePrefix("piped:")
        if (cleanId.isBlank()) return emptyList()
        val endpoint = WatchEndpoint(
            videoId = cleanId,
            playlistId = "RDAMVM$cleanId"
        )
        val result = YouTube.next(endpoint).getOrNull()
            ?: YouTube.next(WatchEndpoint(videoId = cleanId)).getOrNull()
            ?: return fallback?.getRadio(videoId) ?: emptyList()
        return result.items.map { mapSongItem(it) }
    }

    override suspend fun getArtistRadio(artistId: String): List<Track> {
        val cleanId = artistId.removePrefix("piped:")
        if (cleanId.isBlank()) return emptyList()

        val endpoint = WatchEndpoint(
            playlistId = if (cleanId.startsWith("RDAMEA")) cleanId else "RDAMEA$cleanId"
        )
        val result = YouTube.next(endpoint).getOrNull()
        if (result != null && result.items.isNotEmpty()) {
            return result.items.map { mapSongItem(it) }
        }

        val artistDetails = getArtistDetails(cleanId)
        val firstTopSongId = artistDetails?.topSongs?.firstOrNull()?.sourceId
            ?: artistDetails?.topSongs?.firstOrNull()?.id?.removePrefix("piped:")
        if (!firstTopSongId.isNullOrBlank()) {
            return getRadio(firstTopSongId)
        }

        return emptyList()
    }

    override suspend fun browseCategory(browseId: String, params: String?): BrowseCategoryResult? {
        val browseResult = YouTube.browse(browseId, params).getOrNull() ?: return null
        val sections = browseResult.items.map { item ->
            BrowseCategorySection(
                title = item.title,
                items = item.items.mapNotNull { mapYTItem(it) }
            )
        }
        return BrowseCategoryResult(
            title = browseResult.title,
            sections = sections
        )
    }

    private fun mapYTItem(item: YTItem): SearchResult? {
        return when (item) {
            is SongItem -> SearchResult.Song(mapSongItem(item))
            is AlbumItem -> mapAlbumItem(item)
            is ArtistItem -> SearchResult.Artist(
                id = item.id,
                name = item.title,
                artworkUrl = item.thumbnail
            )

            is PlaylistItem -> SearchResult.Playlist(
                id = item.id,
                title = item.title,
                author = item.author?.name ?: "Unknown",
                trackCount = item.songCountText?.filter { it.isDigit() }?.toIntOrNull(),
                artworkUrl = item.thumbnail
            )
        }
    }

    private fun mapSongItem(item: SongItem): Track {
        return Track(
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

    private fun mapAlbumItem(item: AlbumItem): SearchResult.Album {
        return SearchResult.Album(
            id = item.browseId,
            title = item.title,
            author = item.artists?.joinToString(", ") { it.name } ?: "Unknown",
            year = item.year?.toString(),
            artworkUrl = item.thumbnail
        )
    }
}

private fun List<YTItem>.toHomeSectionType(): HomeSectionType = when {
    isEmpty() -> HomeSectionType.MIXED
    all { it is SongItem && it.isVideoSong } -> HomeSectionType.VIDEOS
    all { it is SongItem } -> HomeSectionType.SONGS
    all { it is AlbumItem } -> HomeSectionType.ALBUMS
    all { it is PlaylistItem } -> HomeSectionType.PLAYLISTS
    all { it is ArtistItem } -> HomeSectionType.ARTISTS
    else -> HomeSectionType.MIXED
}