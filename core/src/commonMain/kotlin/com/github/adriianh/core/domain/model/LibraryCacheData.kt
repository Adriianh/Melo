package com.github.adriianh.core.domain.model

import com.github.adriianh.core.domain.model.search.SearchResult
import kotlinx.serialization.Serializable

@Serializable
data class LibraryCacheData(
    val playlists: List<SearchResult.Playlist> = emptyList(),
    val likedSongs: List<Track> = emptyList(),
    val albums: List<SearchResult.Album> = emptyList(),
    val artists: List<SearchResult.Artist> = emptyList(),
    val profile: AccountProfile? = null,
    val remoteHistory: List<HistoryEntry> = emptyList(),
    val lastSyncedAt: Long = 0L,
)
