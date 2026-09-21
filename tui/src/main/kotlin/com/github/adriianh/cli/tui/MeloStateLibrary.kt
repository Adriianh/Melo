package com.github.adriianh.cli.tui

import com.github.adriianh.core.domain.model.FavoriteEntity
import com.github.adriianh.core.domain.model.FavoriteEntityType
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.PlaylistSortOrder
import com.github.adriianh.core.domain.model.SortDirection
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackSortOrder
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.model.toFavoriteEntity

/**
 * A track belonging to the favorites library, either local or remote.
 */
sealed interface LibraryFavoriteItem {
    val track: Track
    val isRemote: Boolean
    val icon: String

    data class Local(override val track: Track) : LibraryFavoriteItem {
        override val isRemote: Boolean get() = false
        override val icon: String get() = "⌂"
    }

    data class Remote(override val track: Track) : LibraryFavoriteItem {
        override val isRemote: Boolean get() = true
        override val icon: String get() = "☁"
    }
}

/**
 * A favorite entity (album, artist or playlist) from the library.
 */
sealed interface LibraryFavoriteEntityItem {
    val entity: FavoriteEntity
    val isRemote: Boolean
    val icon: String

    data class Local(override val entity: FavoriteEntity) : LibraryFavoriteEntityItem {
        override val isRemote: Boolean get() = false
        override val icon: String get() = "⌂"
    }

    data class Remote(override val entity: FavoriteEntity) : LibraryFavoriteEntityItem {
        override val isRemote: Boolean get() = true
        override val icon: String get() = "☁"
    }
}

/**
 * Merges local and remote favorites, de-duplicating remote tracks.
 */
fun MeloState.allLibraryFavorites(): List<LibraryFavoriteItem> {
    val remoteSongIds = collections.remoteFavorites.flatMap {
        listOf(
            it.id,
            it.id.removePrefix("piped:"),
            "piped:${it.id.removePrefix("piped:")}",
            it.sourceId.orEmpty()
        )
    }.filter { it.isNotBlank() }.toSet()

    val localTracks = collections.favorites.filter { track ->
        val rawId = track.sourceId?.takeIf { it.isNotBlank() } ?: track.id.removePrefix("piped:")
        track.id !in remoteSongIds && rawId !in remoteSongIds
    }

    val localItems = localTracks.map { LibraryFavoriteItem.Local(it) }
    val remoteItems = collections.remoteFavorites.map { LibraryFavoriteItem.Remote(it) }

    return localItems + remoteItems
}

/**
 * Checks if a track is favorited, matching local or remote favorites by id/sourceId.
 */
fun MeloState.isFavoriteTrack(track: Track): Boolean = isFavoriteTrack(track.id, track.sourceId)

fun MeloState.isFavoriteTrack(trackId: String, sourceId: String? = null): Boolean {
    val rawId = trackId.removePrefix("piped:")
    fun matches(id: String) = id == trackId || id == rawId || id.removePrefix("piped:") == rawId
    val inFavorites = collections.favorites.any {
        val sId = it.sourceId
        matches(it.id) || (sId != null && (matches(sId) || sId == sourceId))
    }
    if (inFavorites) return true
    return collections.remoteFavorites.any {
        val sId = it.sourceId
        matches(it.id) || (sId != null && (matches(sId) || sId == sourceId))
    }
}

/**
 * Checks whether the entity id is a favorite, local or remote.
 */
fun MeloState.isFavoriteEntity(entityId: String): Boolean {
    val rawId = entityId.removePrefix("piped:")
    fun matches(id: String) = id == entityId || id == rawId || id.removePrefix("piped:") == rawId
    if (collections.favoriteEntities.any { matches(it.id) }) return true
    if (collections.remoteAlbums.any { matches(it.id) }) return true
    if (collections.remoteArtists.any { matches(it.id) }) return true
    if (collections.remotePlaylists.any { matches(it.id) }) return true
    return false
}

/**
 * All favorite albums (local + remote), de-duplicated.
 */
fun MeloState.allFavoriteAlbums(): List<LibraryFavoriteEntityItem> {
    val remoteIds =
        collections.remoteAlbums.flatMap { listOf(it.id, it.id.removePrefix("piped:")) }.toSet()
    val localItems = collections.favoriteEntities
        .filter { it.type == FavoriteEntityType.ALBUM && it.id !in remoteIds && it.id.removePrefix("piped:") !in remoteIds }
        .map { LibraryFavoriteEntityItem.Local(it) }
    val remoteItems =
        collections.remoteAlbums.map { LibraryFavoriteEntityItem.Remote(it.toFavoriteEntity()) }
    return localItems + remoteItems
}

/**
 * All favorite artists (local + remote), de-duplicated.
 */
fun MeloState.allFavoriteArtists(): List<LibraryFavoriteEntityItem> {
    val remoteIds =
        collections.remoteArtists.flatMap { listOf(it.id, it.id.removePrefix("piped:")) }.toSet()
    val localItems = collections.favoriteEntities
        .filter {
            it.type == FavoriteEntityType.ARTIST && it.id !in remoteIds && it.id.removePrefix(
                "piped:"
            ) !in remoteIds
        }
        .map { LibraryFavoriteEntityItem.Local(it) }
    val remoteItems =
        collections.remoteArtists.map { LibraryFavoriteEntityItem.Remote(it.toFavoriteEntity()) }
    return localItems + remoteItems
}

/**
 * All favorite playlists (local + remote), de-duplicated.
 */
fun MeloState.allFavoritePlaylists(): List<LibraryFavoriteEntityItem> {
    val remoteIds =
        collections.remotePlaylists.flatMap { listOf(it.id, it.id.removePrefix("piped:")) }.toSet()
    val localItems = collections.favoriteEntities
        .filter {
            it.type == FavoriteEntityType.PLAYLIST && it.id !in remoteIds && it.id.removePrefix(
                "piped:"
            ) !in remoteIds
        }
        .map { LibraryFavoriteEntityItem.Local(it) }
    val remoteItems =
        collections.remotePlaylists.map { LibraryFavoriteEntityItem.Remote(it.toFavoriteEntity()) }
    return localItems + remoteItems
}

/**
 * All favorite entities for a sub-tab of the favorites panel.
 */
fun MeloState.allFavoriteEntities(subTab: FavoritesSubTab): List<LibraryFavoriteEntityItem> =
    when (subTab) {
        FavoritesSubTab.SONGS -> emptyList()
        FavoritesSubTab.ALBUMS -> allFavoriteAlbums()
        FavoritesSubTab.ARTISTS -> allFavoriteArtists()
        FavoritesSubTab.PLAYLISTS -> allFavoritePlaylists()
    }

/**
 * A playlist shown in the library, either local or remote.
 */
sealed interface LibraryPlaylistItem {
    val title: String
    val author: String
    val trackCountText: String
    val isRemote: Boolean
    val icon: String

    data class Local(val playlist: Playlist) : LibraryPlaylistItem {
        override val title: String get() = playlist.name
        override val author: String get() = "You"
        override val trackCountText: String get() = "${playlist.trackCount} track${if (playlist.trackCount != 1) "s" else ""}"
        override val isRemote: Boolean get() = false
        override val icon: String get() = "≡"
    }

    data class Remote(val playlist: SearchResult.Playlist) : LibraryPlaylistItem {
        override val title: String get() = playlist.title
        override val author: String get() = playlist.author.ifBlank { "YouTube Music" }
        override val trackCountText: String
            get() = playlist.trackCount?.let { "$it track${if (it != 1) "s" else ""}" }
                ?: "Playlist"
        override val isRemote: Boolean get() = true
        override val icon: String get() = "☁"
    }
}

/**
 * All playlists in the library (local + remote).
 */
fun MeloState.allLibraryPlaylists(): List<LibraryPlaylistItem> {
    val local = collections.playlists.map { LibraryPlaylistItem.Local(it) }
    val remote = collections.remotePlaylists.map { LibraryPlaylistItem.Remote(it) }
    return local + remote
}

/**
 * Applies source filter, text query and sort to the favorites list.
 */
fun MeloState.filteredAndSortedFavorites(
    sourceFilter: LibrarySourceFilter,
    sortOrder: TrackSortOrder,
    sortDirection: SortDirection = SortDirection.ASCENDING,
    query: String
): List<LibraryFavoriteItem> {
    var items = allLibraryFavorites()

    items = when (sourceFilter) {
        LibrarySourceFilter.ALL -> items
        LibrarySourceFilter.LOCAL -> items.filter { !it.isRemote }
        LibrarySourceFilter.REMOTE -> items.filter { it.isRemote }
    }

    if (query.isNotBlank()) {
        val q = query.lowercase().trim()
        items = items.filter { item ->
            item.track.title.lowercase().contains(q) ||
                    item.track.artist.lowercase().contains(q) ||
                    item.track.album.lowercase().contains(q)
        }
    }

    val sorted = when (sortOrder) {
        TrackSortOrder.DEFAULT -> items
        TrackSortOrder.TITLE -> items.sortedBy { it.track.title.lowercase() }
        TrackSortOrder.ARTIST -> items.sortedBy { it.track.artist.lowercase() }
        TrackSortOrder.DURATION -> items.sortedBy { it.track.durationMs }
    }

    return if (sortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
}

/**
 * Applies source filter, text query and sort to the playlists list.
 */
fun MeloState.filteredAndSortedPlaylists(
    sourceFilter: LibrarySourceFilter,
    sortOrder: PlaylistSortOrder,
    sortDirection: SortDirection = SortDirection.ASCENDING,
    query: String
): List<LibraryPlaylistItem> {
    var items = allLibraryPlaylists()

    items = when (sourceFilter) {
        LibrarySourceFilter.ALL -> items
        LibrarySourceFilter.LOCAL -> items.filter { !it.isRemote }
        LibrarySourceFilter.REMOTE -> items.filter { it.isRemote }
    }

    if (query.isNotBlank()) {
        val q = query.lowercase().trim()
        items = items.filter { item ->
            item.title.lowercase().contains(q) ||
                    item.author.lowercase().contains(q)
        }
    }

    val sorted = when (sortOrder) {
        PlaylistSortOrder.DEFAULT -> items
        PlaylistSortOrder.NAME -> items.sortedBy { it.title.lowercase() }
        PlaylistSortOrder.TRACKS -> items.sortedBy { item ->
            when (item) {
                is LibraryPlaylistItem.Local -> item.playlist.trackCount
                is LibraryPlaylistItem.Remote -> item.playlist.trackCount ?: 0
            }
        }

        PlaylistSortOrder.AUTHOR -> items.sortedBy { it.author.lowercase() }
    }

    return if (sortDirection == SortDirection.DESCENDING) sorted.reversed() else sorted
}