package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.util.LrcLine
import com.github.adriianh.core.domain.model.ArtistStat
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.ListeningStats
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.StatsPeriod
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackStat
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
 * Unit used to display listening time in the statistics screen.
 */
enum class StatsTimeUnit(val label: String) {
    SECONDS("Secs"),
    MINUTES("Mins"),
    HOURS("Hours"),
}

/**
 * Unified application state for the Melo TUI.
 */
data class MeloState(
    // Sidebar
    val activeSection: SidebarSection = SidebarSection.HOME,

    // Search
    val query: String = "",
    val results: List<Track> = emptyList(),
    val selectedIndex: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val errorMessage: String? = null,

    // Selected track details
    val selectedTrack: Track? = null,
    val detailTab: DetailTab = DetailTab.INFO,
    val lyrics: String? = null,
    val isLoadingLyrics: Boolean = false,
    val similarTracks: List<Track> = emptyList(),
    val isLoadingSimilar: Boolean = false,
    val isLoadingMoreSimilar: Boolean = false,
    val hasMoreSimilar: Boolean = true,
    val similarCursor: Int = 0,
    val artworkData: ImageData? = null,
    val nowPlayingArtwork: ImageData? = null,

    // Library
    val favorites: List<Track> = emptyList(),
    val isFavorite: Boolean = false,
    val libraryTab: LibraryTab = LibraryTab.FAVORITES,

    // Playlists
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylist: Playlist? = null,
    val playlistTracks: List<Track> = emptyList(),
    val isInPlaylistDetail: Boolean = false,
    val playlistInput: String = "",
    val playlistInputMode: PlaylistInputMode = PlaylistInputMode.NONE,
    val playlistPickerTrack: Track? = null,
    val playlistPickerCursor: Int = 0,

    // Home
    val recentTracks: List<HistoryEntry> = emptyList(),
    val homeSection: HomeSection = HomeSection.RECENT,
    val homeRecentCursor: Int = 0,
    val homeFavoritesCursor: Int = 0,

    // Now playing (player bar)
    val nowPlaying: Track? = null,
    val isPlaying: Boolean = false,
    val isLoadingAudio: Boolean = false,
    val audioError: String? = null,

    // Queue
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = -1,
    val queueCursor: Int = 0,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val isQueueVisible: Boolean = false,

    // Marquee scroll animation
    val marqueeOffset: Int = 0,
    val progress: Double = 0.0,
    val volume: Int = 75,

    // Radio / auto-play
    val isRadioMode: Boolean = false,
    val isLoadingMoreRadio: Boolean = false,

    // Session restore
    val isRestoringSession: Boolean = false,

    // Now Playing screen — synced lyrics
    val syncedLyrics: List<LrcLine> = emptyList(),
    val isLoadingSyncedLyrics: Boolean = false,
    val nowPlayingPositionMs: Long = 0L,

    // Graphics
    val needsGraphicsClear: Boolean = false,
    val pendingSection: SidebarSection? = null,

    // Statistics
    val statsPeriod: StatsPeriod = StatsPeriod.ALL_TIME,
    val statsTimeUnit: StatsTimeUnit = StatsTimeUnit.MINUTES,
    val statsTopTracks: List<TrackStat> = emptyList(),
    val statsTopArtists: List<ArtistStat> = emptyList(),
    val statsListening: ListeningStats? = null,
    val statsLoading: Boolean = false,

    // Sidebar cursor — tracks which list the navigation highlight is in
    val sidebarInUtil: Boolean = false,
)