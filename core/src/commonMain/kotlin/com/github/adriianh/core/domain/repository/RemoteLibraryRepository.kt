package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import kotlinx.coroutines.flow.SharedFlow

sealed interface LibraryUpdateEvent {
    data class TrackLiked(val videoId: String, val isLiked: Boolean) : LibraryUpdateEvent
    data class AlbumSaved(val browseId: String, val isSaved: Boolean) : LibraryUpdateEvent
    data class PlaylistSaved(val playlistId: String, val isSaved: Boolean) : LibraryUpdateEvent
    data class ArtistSubscribed(val channelId: String, val isSubscribed: Boolean) :
        LibraryUpdateEvent
}

interface RemoteLibraryRepository {
    val libraryUpdates: SharedFlow<LibraryUpdateEvent>

    suspend fun getAccountProfile(): Result<AccountProfile>
    suspend fun getUserPlaylists(): Result<List<SearchResult.Playlist>>
    suspend fun getLikedSongs(): Result<List<Track>>
    suspend fun getUserArtists(): Result<List<SearchResult.Artist>>
    suspend fun getUserAlbums(): Result<List<SearchResult.Album>>
    suspend fun getRemoteHistory(): Result<List<HistoryEntry>>
    suspend fun toggleLike(videoId: String, isLiked: Boolean): Result<Unit>
    suspend fun toggleLikeAlbum(browseId: String, isLiked: Boolean): Result<Unit>
    suspend fun toggleLikePlaylist(playlistId: String, isLiked: Boolean): Result<Unit>
    suspend fun subscribeChannel(channelId: String, isSubscribed: Boolean): Result<Unit>
}