package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.util.LrcLine
import com.github.adriianh.core.domain.model.ArtistStat
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.FavoriteEntity
import com.github.adriianh.core.domain.model.FavoriteEntityType
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.HomeFeedChip
import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.ListeningStats
import com.github.adriianh.core.domain.model.LyricsTranslationMode
import com.github.adriianh.core.domain.model.OfflineFilterType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.PlaylistSortOrder
import com.github.adriianh.core.domain.model.SortDirection
import com.github.adriianh.core.domain.model.StatsPeriod
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackSortOrder
import com.github.adriianh.core.domain.model.TrackStat
import com.github.adriianh.core.domain.model.mergeHistoryEntries
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.model.toFavoriteEntity
import com.github.adriianh.core.domain.player.RepeatMode
import dev.tamboui.image.ImageData

/**
 * Focus areas within the Home feed tab.
 */
enum class HomeFeedFocus {
    CHIPS,
    SECTIONS,
    ITEMS,
}

/**
 * Sections available in the sidebar navigation.
 */
enum class SidebarSection {
    HOME,
    SEARCH,
    LIBRARY,
    NOW_PLAYING,
    STATS,
    OFFLINE,
    SETTINGS,
}

/**
 * Detail tabs inside the detail side-panel.
 */
enum class DetailTab {
    INFO,
    LYRICS,
    SIMILAR,
}

/**
 * Tabs for the library panel.
 */
enum class LibraryTab {
    FAVORITES,
    PLAYLISTS,
    LOCAL,
}

enum class FavoritesSubTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists");

    fun next(): FavoritesSubTab = entries[(ordinal + 1) % entries.size]
    fun previous(): FavoritesSubTab = entries[(ordinal - 1 + entries.size) % entries.size]
}

enum class LibrarySourceFilter {
    ALL,
    LOCAL,
    REMOTE;

    fun next(): LibrarySourceFilter = entries[(ordinal + 1) % entries.size]
}

/**
 * Active tab within the Home screen.
 */
enum class HomeTab {
    FEED,
    RECENT,
    FAVORITES,
}

/**
 * Input mode for playlist create/rename/picker overlay.
 */
enum class PlaylistInputMode {
    NONE,
    CREATE,
    RENAME,
    PICKER,
}

/**
 * Unit used to display listening time in the statistics screen.
 */
enum class StatsTimeUnit(val label: String) {
    SECONDS("Secs"),
    MINUTES("Mins"),
    HOURS("Hours"),
}

/**
 * Player-specific state for Melo TUI.
 */
data class PlayerState(
    val nowPlaying: Track? = null,
    val isPlaying: Boolean = false,
    val isLoadingAudio: Boolean = false,
    val audioError: String? = null,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1,
    val queueCursor: Int = 0,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val shuffleEnabled: Boolean = false,
    val isQueueVisible: Boolean = false,
    val volume: Int = 75,
    val isRadioMode: Boolean = false,
    val syncedLyrics: List<LrcLine> = emptyList(),
    val isLoadingSyncedLyrics: Boolean = false,
    val isTranslatingLyrics: Boolean = false,
    val lyricsTranslationMode: LyricsTranslationMode = LyricsTranslationMode.ORIGINAL,
    val nowPlayingPositionMs: Long = 0L,
    val nowPlayingArtwork: ImageData? = null,
    val marqueeOffset: Int = 0,
    val progress: Double = 0.0,
    val isFavorite: Boolean = false,
)

/**
 * Global navigation state for the Melo TUI sidebar and focus.
 */
data class NavigationState(
    val activeSection: SidebarSection = SidebarSection.HOME,
    val pendingSection: SidebarSection? = null,
    val sidebarInUtil: Boolean = false,
)

enum class SearchTab { SONGS, ALBUMS, ARTISTS, PLAYLISTS }

/**
 * Represents the state of a specific screen.
 */
sealed interface ScreenState {
    data class Search(
        val tab: SearchTab = SearchTab.SONGS,
        val query: String = "",
        val searchSuggestions: List<String> = emptyList(),
        val selectedSuggestionIndex: Int? = null,
        val isShowingSuggestions: Boolean = false,
        val results: List<Track> = emptyList(),
        val albumResults: List<SearchResult.Album> = emptyList(),
        val artistResults: List<SearchResult.Artist> = emptyList(),
        val playlistResults: List<SearchResult.Playlist> = emptyList(),
        val cursor: Int = 0,
        val selectedIndex: Int = 0,
        val localCursor: Int = 0,
        val localResults: List<Track> = emptyList(),
        val isLoadingSearch: Boolean = false,
        val searchError: String? = null,
        val isInEntityDetail: Boolean = false,
        val entityTitle: String? = null,
        val entityTracks: List<Track> = emptyList(),
        val artistDashboardItems: List<Any> = emptyList(),
        val artistDashboardX: Int = 0,
        val artistDashboardY: Int = 0,
        val artistDashboardPositions: Map<String, Int> = emptyMap(),
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val errorMessage: String? = null
    ) : ScreenState

    data class Home(
        val homeTab: HomeTab = HomeTab.FEED,
        val feedSections: List<HomeSection> = emptyList(),
        val feedChips: List<HomeFeedChip> = emptyList(),
        val feedContinuation: String? = null,
        val isLoadingMoreSections: Boolean = false,
        val selectedChipIndex: Int = 0,
        val selectedSectionIndex: Int = 0,
        val selectedItemIndex: Int = 0,
        val feedFocus: HomeFeedFocus = HomeFeedFocus.ITEMS,
        val isLoadingFeed: Boolean = false,
        val feedError: String? = null,
        val homeRecentCursor: Int = 0,
        val homeFavoritesCursor: Int = 0,
    ) : ScreenState

    data class Library(
        val libraryTab: LibraryTab = LibraryTab.FAVORITES,
        val favoritesSubTab: FavoritesSubTab = FavoritesSubTab.SONGS,
        val favoriteEntitiesCursor: Int = 0,
        val selectedPlaylist: Playlist? = null,
        val playlistTracks: List<Track> = emptyList(),
        val isInPlaylistDetail: Boolean = false,
        val localTracks: List<Track> = emptyList(),
        val searchQuery: String = "",
        val isTyping: Boolean = false,
        val localFilterIndex: Int = 0,
        val localSortOrder: TrackSortOrder = TrackSortOrder.DEFAULT,
        val localSortDirection: SortDirection = SortDirection.ASCENDING,
        val localSearchQuery: String = searchQuery,
        val selectedIndex: Int = 0,
        val isLoading: Boolean = false,
        val favoritesSourceFilter: LibrarySourceFilter = LibrarySourceFilter.ALL,
        val favoritesSortOrder: TrackSortOrder = TrackSortOrder.DEFAULT,
        val favoritesSortDirection: SortDirection = SortDirection.ASCENDING,
        val favoritesSearchQuery: String = "",
        val playlistsSourceFilter: LibrarySourceFilter = LibrarySourceFilter.ALL,
        val playlistsSortOrder: PlaylistSortOrder = PlaylistSortOrder.DEFAULT,
        val playlistsSortDirection: SortDirection = SortDirection.ASCENDING,
        val playlistsSearchQuery: String = "",
        val playlistDetailSortOrder: TrackSortOrder = TrackSortOrder.DEFAULT,
        val playlistDetailSortDirection: SortDirection = SortDirection.ASCENDING,
        val playlistDetailSearchQuery: String = "",
    ) : ScreenState

    data class Stats(
        val statsPeriod: StatsPeriod = StatsPeriod.ALL_TIME,
        val statsTimeUnit: StatsTimeUnit = StatsTimeUnit.MINUTES,
        val statsTopTracks: List<TrackStat> = emptyList(),
        val statsTopArtists: List<ArtistStat> = emptyList(),
        val statsListening: ListeningStats? = null,
        val statsLoading: Boolean = false,
    ) : ScreenState

    data class NowPlaying(
        val lyricsScrollOffset: Int = 0,
        val isAutoScrollLyrics: Boolean = true,
    ) : ScreenState

    data class Offline(
        val downloads: List<OfflineTrack> = emptyList(),
        val selectedIndex: Int = 0,
        val filterType: OfflineFilterType = OfflineFilterType.ALL,
        val sortOrder: TrackSortOrder = TrackSortOrder.DEFAULT,
        val sortDirection: SortDirection = SortDirection.ASCENDING,
        val searchQuery: String = "",
        val isTyping: Boolean = false,
        val isLoading: Boolean = false
    ) : ScreenState

    data class EntityDetail(
        val entity: SearchResult,
        val title: String,
        val subtitle: String? = null,
        val description: String? = null,
        val tracks: List<Track> = emptyList(),
        val artistDashboardItems: List<Any> = emptyList(),
        val artistDashboardX: Int = 0,
        val artistDashboardY: Int = 0,
        val artistDashboardPositions: Map<String, Int> = emptyMap(),
        val selectedIndex: Int = 0,
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val returnScreen: ScreenState,
        val returnSection: SidebarSection,
        val searchQuery: String = "",
        val isTyping: Boolean = false,
        val sortOrder: TrackSortOrder = TrackSortOrder.DEFAULT,
        val sortDirection: SortDirection = SortDirection.ASCENDING,
    ) : ScreenState
}

/**
 * Persisted state for the detail side-panel.
 */
data class DetailState(
    val isVisible: Boolean = true,
    val selectedTrack: Track? = null,
    val selectedEntity: SearchResult? = null,
    val detailTab: DetailTab = DetailTab.INFO,
    val lyrics: String? = null,
    val syncedLyrics: List<LrcLine> = emptyList(),
    val lyricsScrollOffset: Int = 0,
    val isAutoScrollLyrics: Boolean = true,
    val isLoadingLyrics: Boolean = false,
    val isTranslatingLyrics: Boolean = false,
    val lyricsTranslationMode: LyricsTranslationMode = LyricsTranslationMode.ORIGINAL,
    val plainLyricsTranslation: String? = null,
    val similarTracks: List<Track> = emptyList(),
    val isLoadingSimilar: Boolean = false,
    val isLoadingMoreSimilar: Boolean = false,
    val hasMoreSimilar: Boolean = true,
    val similarCursor: Int = 0,
    val artworkData: ImageData? = null,
    val entityGenres: List<String> = emptyList(),
    val isLoadingEntityMeta: Boolean = false,
)

/**
 * State for multi-track batch selection (Visual mode).
 */
data class BatchSelectionState(
    val isSelectionMode: Boolean = false,
    val selectedTracks: Map<String, Track> = emptyMap(),
) {
    val count: Int get() = selectedTracks.size
    val isNotEmpty: Boolean get() = selectedTracks.isNotEmpty()
    val isEmpty: Boolean get() = selectedTracks.isEmpty()
    fun isSelected(trackId: String): Boolean = selectedTracks.containsKey(trackId)
    fun tracks(): List<Track> = selectedTracks.values.toList()

    fun toggle(track: Track): BatchSelectionState {
        val updated = selectedTracks.toMutableMap()
        if (updated.containsKey(track.id)) {
            updated.remove(track.id)
        } else {
            updated[track.id] = track
        }
        return copy(
            isSelectionMode = updated.isNotEmpty(),
            selectedTracks = updated
        )
    }

    fun selectAll(tracks: List<Track>): BatchSelectionState {
        val updated = selectedTracks.toMutableMap()
        tracks.forEach { updated[it.id] = it }
        return copy(isSelectionMode = updated.isNotEmpty(), selectedTracks = updated)
    }

    fun clear(): BatchSelectionState = copy(isSelectionMode = false, selectedTracks = emptyMap())
}

/**
 * Global state for playlist interaction overlays.
 */
data class PlaylistInteractionState(
    val playlistInput: String = "",
    val playlistInputMode: PlaylistInputMode = PlaylistInputMode.NONE,
    val playlistPickerTrack: Track? = null,
    val playlistPickerTracks: List<Track> = emptyList(),
    val playlistPickerCursor: Int = 0,
)

/**
 * Global state for track options menu (context menu).
 */
data class TrackOptionsMenuState(
    val track: Track? = null,
    val batchTracks: List<Track> = emptyList(),
    val selectedIndex: Int = 0,
    val isVisible: Boolean = false,
) {
    val isBatch: Boolean get() = batchTracks.isNotEmpty()
}

/**
 * Global state for the lyrics language selector modal.
 */
data class LanguagePickerState(
    val isVisible: Boolean = false,
    val selectedIndex: Int = 0,
    val currentLanguage: String = "es",
    val languages: List<Pair<String, String>> = listOf(
        "es" to "Español",
        "en" to "English",
        "pt" to "Português",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "ja" to "日本語"
    )
)

/**
 * Global UI/System flags
 */
data class CommandBarState(
    val isVisible: Boolean = false,
    val input: String = "",
    val errorMessage: String? = null,
    val history: List<String> = emptyList(),
    val historyIndex: Int = -1,
    val cursorPosition: Int = 0,
    val previousFocusId: String? = null,
    val suggestions: List<String> = emptyList(),
    val selectedSuggestionIndex: Int? = null
)

/**
 * Global persistent collections.
 */
data class CollectionsState(
    val favorites: List<Track> = emptyList(),
    val remoteFavorites: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val remotePlaylists: List<SearchResult.Playlist> = emptyList(),
    val remoteAlbums: List<SearchResult.Album> = emptyList(),
    val remoteArtists: List<SearchResult.Artist> = emptyList(),
    val recentTracks: List<HistoryEntry> = emptyList(),
    val remoteRecentTracks: List<HistoryEntry> = emptyList(),
    val offlineTracks: List<OfflineTrack> = emptyList(),
    val favoriteEntities: List<FavoriteEntity> = emptyList(),
)

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

fun MeloState.isFavoriteEntity(entityId: String): Boolean {
    val rawId = entityId.removePrefix("piped:")
    fun matches(id: String) = id == entityId || id == rawId || id.removePrefix("piped:") == rawId
    if (collections.favoriteEntities.any { matches(it.id) }) return true
    if (collections.remoteAlbums.any { matches(it.id) }) return true
    if (collections.remoteArtists.any { matches(it.id) }) return true
    if (collections.remotePlaylists.any { matches(it.id) }) return true
    return false
}

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

fun MeloState.allFavoriteEntities(subTab: FavoritesSubTab): List<LibraryFavoriteEntityItem> =
    when (subTab) {
        FavoritesSubTab.SONGS -> emptyList()
        FavoritesSubTab.ALBUMS -> allFavoriteAlbums()
        FavoritesSubTab.ARTISTS -> allFavoriteArtists()
        FavoritesSubTab.PLAYLISTS -> allFavoritePlaylists()
    }

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

fun MeloState.allLibraryPlaylists(): List<LibraryPlaylistItem> {
    val local = collections.playlists.map { LibraryPlaylistItem.Local(it) }
    val remote = collections.remotePlaylists.map { LibraryPlaylistItem.Remote(it) }
    return local + remote
}

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

/**
 * Unified application state for the Melo TUI.
 */
data class MeloState(
    val player: PlayerState = PlayerState(),
    val navigation: NavigationState = NavigationState(),
    val screen: ScreenState = ScreenState.Home(),
    val detail: DetailState = DetailState(),
    val collections: CollectionsState = CollectionsState(),
    val playlistInteraction: PlaylistInteractionState = PlaylistInteractionState(),
    val trackOptions: TrackOptionsMenuState = TrackOptionsMenuState(),
    val selection: BatchSelectionState = BatchSelectionState(),
    val commandBar: CommandBarState = CommandBarState(),
    val languagePicker: LanguagePickerState = LanguagePickerState(),
    val isSettingsVisible: Boolean = false,
    val isOfflineMode: Boolean = false,
    val isRestoringSession: Boolean = false,
    val needsGraphicsClear: Boolean = false,
    val youtubeAccountName: String? = null,
)

/**
 * Checks if a track can be played given the current offline mode state.
 */
fun MeloState.isPlayable(track: Track): Boolean {
    if (!isOfflineMode) return true
    if (track.id.startsWith("local:")) return true

    val byId = collections.offlineTracks.any {
        it.track.id == track.id && it.downloadStatus == DownloadStatus.COMPLETED
    }
    if (byId) return true

    val sourceId = track.sourceId
    if (sourceId != null) {
        return collections.offlineTracks.any {
            it.track.sourceId == sourceId && it.downloadStatus == DownloadStatus.COMPLETED
        }
    }

    return false
}

/**
 * Returns merged local and remote playback history, with deduplication.
 */
fun MeloState.allRecentTracks(): List<HistoryEntry> =
    mergeHistoryEntries(collections.recentTracks, collections.remoteRecentTracks)