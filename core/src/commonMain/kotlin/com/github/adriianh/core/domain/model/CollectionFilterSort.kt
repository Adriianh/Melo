package com.github.adriianh.core.domain.model

/**
 * Filters and sorts a list of [Track]s based on a text query and sort criteria.
 */
fun filterAndSortTracks(
    tracks: List<Track>,
    sortOrder: TrackSortOrder = TrackSortOrder.DEFAULT,
    sortDirection: SortDirection = SortDirection.ASCENDING,
    query: String = ""
): List<Track> {
    var result = tracks
    if (query.isNotBlank()) {
        val q = query.lowercase().trim()
        result = result.filter { track ->
            track.title.lowercase().contains(q) ||
                    track.artist.lowercase().contains(q) ||
                    track.album.lowercase().contains(q)
        }
    }
    val sorted = when (sortOrder) {
        TrackSortOrder.DEFAULT -> result
        TrackSortOrder.TITLE -> result.sortedBy { it.title.lowercase() }
        TrackSortOrder.ARTIST -> result.sortedBy { it.artist.lowercase() }
        TrackSortOrder.DURATION -> result.sortedBy { it.durationMs }
    }
    return if (sortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
}

/**
 * Filters and sorts a list of [OfflineTrack]s based on download type, download status,
 * text query, and sort criteria.
 */
fun filterAndSortOfflineTracks(
    downloads: List<OfflineTrack>,
    filterType: OfflineFilterType = OfflineFilterType.ALL,
    sortOrder: TrackSortOrder = TrackSortOrder.DEFAULT,
    sortDirection: SortDirection = SortDirection.ASCENDING,
    query: String = ""
): List<OfflineTrack> {
    var result = downloads.filter { offlineTrack ->
        val matchesType = when (filterType) {
            OfflineFilterType.ALL -> true
            OfflineFilterType.MANUAL -> offlineTrack.downloadType == DownloadType.MANUAL
            OfflineFilterType.CACHE -> offlineTrack.downloadType == DownloadType.PREFETCH
        }
        matchesType && offlineTrack.downloadStatus != DownloadStatus.PENDING
    }
    if (query.isNotBlank()) {
        val q = query.lowercase().trim()
        result = result.filter { item ->
            item.track.title.lowercase().contains(q) ||
                    item.track.artist.lowercase().contains(q) ||
                    item.track.album.lowercase().contains(q)
        }
    }
    val sorted = when (sortOrder) {
        TrackSortOrder.DEFAULT -> result
        TrackSortOrder.TITLE -> result.sortedBy { it.track.title.lowercase() }
        TrackSortOrder.ARTIST -> result.sortedBy { it.track.artist.lowercase() }
        TrackSortOrder.DURATION -> result.sortedBy { it.track.durationMs }
    }
    return if (sortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
}

/**
 * Filters and sorts a list of [Playlist]s based on a text query and sort criteria.
 */
fun filterAndSortPlaylists(
    playlists: List<Playlist>,
    sortOrder: PlaylistSortOrder = PlaylistSortOrder.DEFAULT,
    sortDirection: SortDirection = SortDirection.ASCENDING,
    query: String = ""
): List<Playlist> {
    var result = playlists
    if (query.isNotBlank()) {
        val q = query.lowercase().trim()
        result = result.filter { playlist ->
            playlist.name.lowercase().contains(q)
        }
    }
    val sorted = when (sortOrder) {
        PlaylistSortOrder.DEFAULT -> result
        PlaylistSortOrder.NAME -> result.sortedBy { it.name.lowercase() }
        PlaylistSortOrder.TRACKS -> result.sortedBy { it.trackCount }
        PlaylistSortOrder.AUTHOR -> result
    }
    return if (sortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
}