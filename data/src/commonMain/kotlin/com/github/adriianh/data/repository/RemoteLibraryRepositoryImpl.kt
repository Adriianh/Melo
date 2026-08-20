package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.repository.RemoteLibraryRepository
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.models.AlbumItem
import com.github.adriianh.innertube.models.ArtistItem
import com.github.adriianh.innertube.models.PlaylistItem

class RemoteLibraryRepositoryImpl : RemoteLibraryRepository {

    override suspend fun getAccountProfile(): Result<AccountProfile> = runCatching {
        val info = YouTube.accountInfo().getOrThrow()
        AccountProfile(
            name = info.name,
            email = info.email,
            channelHandle = info.channelHandle,
            avatarUrl = null,
        )
    }

    override suspend fun getUserPlaylists(): Result<List<SearchResult.Playlist>> = runCatching {
        val libraryPage = YouTube.userPlaylists().getOrThrow()
        libraryPage.items.filterIsInstance<PlaylistItem>()
            .filterNot { it.id == "SE" }
            .map { item ->
                SearchResult.Playlist(
                    id = item.id,
                    title = item.title,
                    author = item.author?.name ?: "You",
                    trackCount = item.songCountText?.filter { it.isDigit() }?.toIntOrNull(),
                    artworkUrl = item.thumbnail,
                )
            }
    }

    override suspend fun getLikedSongs(): Result<List<Track>> = runCatching {
        val playlistPage = YouTube.likedSongs().getOrThrow()
        playlistPage.songs.map { item ->
            Track(
                id = "piped:${item.id}",
                title = item.title,
                artist = item.artists.firstOrNull()?.name ?: "Unknown",
                durationMs = item.duration?.times(1000L) ?: 0L,
                album = item.album?.name ?: "",
                genres = emptyList(),
                artworkUrl = item.thumbnail,
                sourceId = item.id,
            )
        }
    }

    override suspend fun getUserArtists(): Result<List<SearchResult.Artist>> = runCatching {
        val libraryPage = YouTube.userArtists().getOrThrow()
        libraryPage.items.filterIsInstance<ArtistItem>().map { item ->
            SearchResult.Artist(
                id = item.id,
                name = item.title,
                artworkUrl = item.thumbnail,
            )
        }
    }

    override suspend fun getUserAlbums(): Result<List<SearchResult.Album>> = runCatching {
        val libraryPage = YouTube.userAlbums().getOrThrow()
        libraryPage.items.filterIsInstance<AlbumItem>().map { item ->
            SearchResult.Album(
                id = item.browseId,
                title = item.title,
                author = item.artists?.joinToString(", ") { it.name } ?: "Unknown",
                year = item.year?.toString(),
                artworkUrl = item.thumbnail,
            )
        }
    }

    override suspend fun getRemoteHistory(): Result<List<HistoryEntry>> = runCatching {
        val historyPage = YouTube.musicHistory().getOrThrow()
        historyPage.sections.orEmpty().flatMap { section ->
            section.songs.map { item ->
                HistoryEntry(
                    track = Track(
                        id = "piped:${item.id}",
                        title = item.title,
                        artist = item.artists.firstOrNull()?.name ?: "Unknown",
                        durationMs = item.duration?.times(1000L) ?: 0L,
                        album = item.album?.name ?: "",
                        genres = emptyList(),
                        artworkUrl = item.thumbnail,
                        sourceId = item.id,
                    ),
                    playedAt = com.github.adriianh.core.platform.currentTimeSeconds() * 1000L,
                )
            }
        }
    }

    override suspend fun toggleLike(videoId: String, isLiked: Boolean): Result<Unit> = runCatching {
        val rawId = videoId.removePrefix("piped:")
        YouTube.likeVideo(rawId, isLiked).getOrThrow()
    }

    override suspend fun toggleLikeAlbum(browseId: String, isLiked: Boolean): Result<Unit> =
        runCatching {
            YouTube.likeAlbum(browseId, isLiked).getOrThrow()
        }

    override suspend fun toggleLikePlaylist(playlistId: String, isLiked: Boolean): Result<Unit> =
        runCatching {
            YouTube.likePlaylist(playlistId, isLiked).getOrThrow()
        }

    override suspend fun subscribeChannel(channelId: String, isSubscribed: Boolean): Result<Unit> =
        runCatching {
            YouTube.subscribeChannel(channelId, isSubscribed).getOrThrow()
        }
}