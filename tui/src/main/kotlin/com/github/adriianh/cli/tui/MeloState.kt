package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.util.LrcLine
import com.github.adriianh.cli.tui.util.ToastMessage
import com.github.adriianh.core.domain.model.ArtistStat
import com.github.adriianh.core.domain.model.DownloadStatus
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
    val equalizerTick: Long = 0,
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
    val toasts: List<ToastMessage> = emptyList(),
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