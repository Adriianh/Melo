package com.github.adriianh.melo.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.OfflineFilterType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.SortDirection
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackSortOrder
import com.github.adriianh.core.domain.model.filterAndSortOfflineTracks
import com.github.adriianh.core.domain.model.filterAndSortTracks
import com.github.adriianh.core.domain.model.search.SearchResult

data class LibraryFilters(
    val customPlaylists: List<Playlist> = emptyList(),
    val remotePlaylists: List<SearchResult.Playlist> = emptyList(),
    val likedSongs: List<Track> = emptyList(),
    val downloadedTracks: List<OfflineTrack> = emptyList(),
    val localTracks: List<Track> = emptyList(),
    val albums: List<SearchResult.Album> = emptyList(),
    val artists: List<SearchResult.Artist> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
)

@Composable
fun rememberLibraryFilters(
    state: LibraryUiState,
    query: String,
    sortOrder: LibrarySortOrder,
): LibraryFilters = LibraryFilters(
    customPlaylists = remember(state.customPlaylists, query, sortOrder) {
        filterPlaylists(
            items = state.customPlaylists,
            query = query,
            sortOrder = sortOrder,
            queryMatches = { it.name },
            title = { it.name },
            count = { it.trackCount }
        )
    },
    remotePlaylists = remember(state.playlists, query, sortOrder) {
        filterRemotePlaylists(
            items = state.playlists,
            query = query,
            sortOrder = sortOrder
        )
    },
    likedSongs = remember(state.likedSongs, query, sortOrder) {
        filterTracks(
            items = state.likedSongs,
            query = query,
            sortOrder = sortOrder
        )
    },
    downloadedTracks = remember(state.downloadedTracks, query, sortOrder) {
        filterDownloadedTracks(
            items = state.downloadedTracks,
            query = query,
            sortOrder = sortOrder
        )
    },
    localTracks = remember(state.localTracks, query, sortOrder) {
        filterTracks(
            items = state.localTracks,
            query = query,
            sortOrder = sortOrder
        )
    },
    albums = remember(state.albums, query, sortOrder) {
        filterAlbums(
            items = state.albums,
            query = query,
            sortOrder = sortOrder
        )
    },
    artists = remember(state.artists, query, sortOrder) {
        val list = if (query.isEmpty()) state.artists else state.artists.filter {
            it.name.lowercase().contains(query)
        }
        when (sortOrder) {
            LibrarySortOrder.RECENTLY_ADDED, LibrarySortOrder.DURATION -> list
            LibrarySortOrder.TITLE_A_Z, LibrarySortOrder.ARTIST_A_Z ->
                list.sortedBy { it.name.lowercase() }
        }
    },
    history = remember(state.history, query, sortOrder) {
        filterHistory(
            items = state.history,
            query = query,
            sortOrder = sortOrder
        )
    }
)

private fun filterTracks(
    items: List<Track>,
    query: String,
    sortOrder: LibrarySortOrder,
): List<Track> {
    val (order, dir) = when (sortOrder) {
        LibrarySortOrder.RECENTLY_ADDED -> TrackSortOrder.DEFAULT to SortDirection.ASCENDING
        LibrarySortOrder.TITLE_A_Z -> TrackSortOrder.TITLE to SortDirection.ASCENDING
        LibrarySortOrder.ARTIST_A_Z -> TrackSortOrder.ARTIST to SortDirection.ASCENDING
        LibrarySortOrder.DURATION -> TrackSortOrder.DURATION to SortDirection.DESCENDING
    }
    return filterAndSortTracks(items, order, dir, query)
}

private fun filterDownloadedTracks(
    items: List<OfflineTrack>,
    query: String,
    sortOrder: LibrarySortOrder,
): List<OfflineTrack> {
    val (order, dir) = when (sortOrder) {
        LibrarySortOrder.RECENTLY_ADDED -> TrackSortOrder.DEFAULT to SortDirection.ASCENDING
        LibrarySortOrder.TITLE_A_Z -> TrackSortOrder.TITLE to SortDirection.ASCENDING
        LibrarySortOrder.ARTIST_A_Z -> TrackSortOrder.ARTIST to SortDirection.ASCENDING
        LibrarySortOrder.DURATION -> TrackSortOrder.DURATION to SortDirection.DESCENDING
    }
    return filterAndSortOfflineTracks(items, OfflineFilterType.ALL, order, dir, query)
}

private fun filterPlaylists(
    items: List<Playlist>,
    query: String,
    sortOrder: LibrarySortOrder,
    queryMatches: (Playlist) -> String,
    title: (Playlist) -> String,
    count: (Playlist) -> Int,
): List<Playlist> {
    val list = if (query.isEmpty()) items else items.filter {
        queryMatches(it).lowercase().contains(query)
    }
    return when (sortOrder) {
        LibrarySortOrder.RECENTLY_ADDED -> list
        LibrarySortOrder.TITLE_A_Z, LibrarySortOrder.ARTIST_A_Z ->
            list.sortedBy { title(it).lowercase() }

        LibrarySortOrder.DURATION -> list.sortedByDescending(count)
    }
}

private fun filterRemotePlaylists(
    items: List<SearchResult.Playlist>,
    query: String,
    sortOrder: LibrarySortOrder,
): List<SearchResult.Playlist> {
    val list = if (query.isEmpty()) items else items.filter {
        it.title.lowercase().contains(query) || it.author.lowercase().contains(query)
    }
    return when (sortOrder) {
        LibrarySortOrder.RECENTLY_ADDED -> list
        LibrarySortOrder.TITLE_A_Z -> list.sortedBy { it.title.lowercase() }
        LibrarySortOrder.ARTIST_A_Z -> list.sortedBy { it.author.lowercase() }
        LibrarySortOrder.DURATION -> list.sortedByDescending { it.trackCount ?: 0 }
    }
}

private fun filterAlbums(
    items: List<SearchResult.Album>,
    query: String,
    sortOrder: LibrarySortOrder,
): List<SearchResult.Album> {
    val list = if (query.isEmpty()) items else items.filter {
        it.title.lowercase().contains(query) || it.author.lowercase().contains(query)
    }
    return when (sortOrder) {
        LibrarySortOrder.RECENTLY_ADDED, LibrarySortOrder.DURATION -> list
        LibrarySortOrder.TITLE_A_Z -> list.sortedBy { it.title.lowercase() }
        LibrarySortOrder.ARTIST_A_Z -> list.sortedBy { it.author.lowercase() }
    }
}

private fun filterHistory(
    items: List<HistoryEntry>,
    query: String,
    sortOrder: LibrarySortOrder,
): List<HistoryEntry> {
    val list = if (query.isEmpty()) items else items.filter {
        it.track.title.lowercase().contains(query) || it.track.artist.lowercase()
            .contains(query)
    }
    return when (sortOrder) {
        LibrarySortOrder.RECENTLY_ADDED -> list
        LibrarySortOrder.TITLE_A_Z -> list.sortedBy { it.track.title.lowercase() }
        LibrarySortOrder.ARTIST_A_Z -> list.sortedBy { it.track.artist.lowercase() }
        LibrarySortOrder.DURATION -> list.sortedByDescending { it.track.durationMs }
    }
}