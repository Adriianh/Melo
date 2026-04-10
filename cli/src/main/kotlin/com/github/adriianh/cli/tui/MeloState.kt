package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.util.LrcLine
import com.github.adriianh.core.domain.model.ArtistStat
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.ListeningStats
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.StatsPeriod
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackStat
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.image.ImageData

/**
 * Repeat modes for queue playback.
 */
enum class RepeatMode {
    OFF,
    ONE,
    ALL,
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
    SETTINGS,
    OFFLINE,
}

/**
 * Tabs for the detail panel.
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

/**
 * Active section within the Home screen.
 */
enum class HomeSection {
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
 * Filter types for the offline screen.
 */
enum class OfflineFilterType(val label: String) {
    ALL("All"),
    MANUAL("Manual"),
    CACHE("Cache"),
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
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val isQueueVisible: Boolean = false,
    val volume: Int = 75,
    val isRadioMode: Boolean = false,
    val isLoadingMoreRadio: Boolean = false,
    val userQueueCount: Int = 0,
    val syncedLyrics: List<LrcLine> = emptyList(),
    val isLoadingSyncedLyrics: Boolean = false,
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
        val query: String = "",
        val tab: SearchTab = SearchTab.SONGS,
        val results: List<Track> = emptyList(),
        val albumResults: List<SearchResult.Album> = emptyList(),
        val artistResults: List<SearchResult.Artist> = emptyList(),
        val playlistResults: List<SearchResult.Playlist> = emptyList(),
        val selectedIndex: Int = 0,
        val isInEntityDetail: Boolean = false,
        val entityTitle: String? = null,
        val entityTracks: List<Track> = emptyList(),
        val artistDashboardItems: List<Any> = emptyList(),
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val errorMessage: String? = null,
    ) : ScreenState

    data class Home(
        val homeSection: HomeSection = HomeSection.RECENT,
        val homeRecentCursor: Int = 0,
        val homeFavoritesCursor: Int = 0,
    ) : ScreenState

    data class Library(
        val libraryTab: LibraryTab = LibraryTab.FAVORITES,
        val selectedPlaylist: Playlist? = null,
        val playlistTracks: List<Track> = emptyList(),
        val isInPlaylistDetail: Boolean = false,
        val localTracks: List<Track> = emptyList(),
        val searchQuery: String = "",
        val isTyping: Boolean = false,
        val localFilterIndex: Int = 0,
        val selectedIndex: Int = 0,
        val isLoading: Boolean = false
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
        val unused: Boolean = true
    ) : ScreenState

    data class Offline(
        val downloads: List<OfflineTrack> = emptyList(),
        val selectedIndex: Int = 0,
        val filterType: OfflineFilterType = OfflineFilterType.ALL,
        val searchQuery: String = "",
        val isTyping: Boolean = false,
        val isLoading: Boolean = false
    ) : ScreenState
}

/**
 * Persisted state for the detail side-panel.
 */
data class DetailState(
    val selectedTrack: Track? = null,
    val selectedEntity: SearchResult? = null,
    val detailTab: DetailTab = DetailTab.INFO,
    val lyrics: String? = null,
    val isLoadingLyrics: Boolean = false,
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
 * Global state for playlist interaction overlays.
 */
data class PlaylistInteractionState(
    val playlistInput: String = "",
    val playlistInputMode: PlaylistInputMode = PlaylistInputMode.NONE,
    val playlistPickerTrack: Track? = null,
    val playlistPickerCursor: Int = 0,
)

/**
 * Global state for track options menu (context menu).
 */
data class TrackOptionsMenuState(
    val track: Track? = null,
    val selectedIndex: Int = 0,
    val isVisible: Boolean = false,
)

/**
 * Global persistent collections.
 */
data class CollectionsState(
    val favorites: List<Track> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val recentTracks: List<HistoryEntry> = emptyList(),
    val offlineTracks: List<OfflineTrack> = emptyList()
)

/**
 * Unified application state for the Melo TUI.
 */
data class MeloState(
    val player: PlayerState = PlayerState(),
    val navigation: NavigationState = NavigationState(),

    // Current primary screen
    val screen: ScreenState = ScreenState.Home(),

    // Persistent detail side-panel
    val detail: DetailState = DetailState(),

    // Persistent collections
    val collections: CollectionsState = CollectionsState(),

    // Global Playlist interactions (overlays)
    val playlistInteraction: PlaylistInteractionState = PlaylistInteractionState(),

    // Global Track options (context menu)
    val trackOptions: TrackOptionsMenuState = TrackOptionsMenuState(),

    // Global UI/System flags
    val isSettingsVisible: Boolean = false,
    val isOfflineMode: Boolean = false,
    val isRestoringSession: Boolean = false,
    val needsGraphicsClear: Boolean = false,
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