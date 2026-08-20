package com.github.adriianh.core.domain.usecase.library

import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.repository.RemoteLibraryRepository

class GetAccountProfileUseCase(
    private val repository: RemoteLibraryRepository,
) {
    suspend operator fun invoke(): Result<AccountProfile> = repository.getAccountProfile()
}

class GetUserPlaylistsUseCase(
    private val repository: RemoteLibraryRepository,
) {
    suspend operator fun invoke(): Result<List<SearchResult.Playlist>> = repository.getUserPlaylists()
}

class GetLikedSongsUseCase(
    private val repository: RemoteLibraryRepository,
) {
    suspend operator fun invoke(): Result<List<Track>> = repository.getLikedSongs()
}

class GetUserArtistsUseCase(
    private val repository: RemoteLibraryRepository,
) {
    suspend operator fun invoke(): Result<List<SearchResult.Artist>> = repository.getUserArtists()
}

class GetUserAlbumsUseCase(
    private val repository: RemoteLibraryRepository,
) {
    suspend operator fun invoke(): Result<List<SearchResult.Album>> = repository.getUserAlbums()
}

class GetRemoteHistoryUseCase(
    private val repository: RemoteLibraryRepository,
) {
    suspend operator fun invoke(): Result<List<HistoryEntry>> = repository.getRemoteHistory()
}

class ToggleLikeTrackUseCase(
    private val repository: RemoteLibraryRepository,
) {
    suspend operator fun invoke(videoId: String, isLiked: Boolean): Result<Unit> =
        repository.toggleLike(videoId, isLiked)
}

class ToggleLikeAlbumUseCase(
    private val repository: RemoteLibraryRepository,
) {
    suspend operator fun invoke(browseId: String, isLiked: Boolean): Result<Unit> =
        repository.toggleLikeAlbum(browseId, isLiked)
}

class ToggleLikePlaylistUseCase(
    private val repository: RemoteLibraryRepository,
) {
    suspend operator fun invoke(playlistId: String, isLiked: Boolean): Result<Unit> =
        repository.toggleLikePlaylist(playlistId, isLiked)
}

class SubscribeChannelUseCase(
    private val repository: RemoteLibraryRepository,
) {
    suspend operator fun invoke(channelId: String, isSubscribed: Boolean): Result<Unit> =
        repository.subscribeChannel(channelId, isSubscribed)
}