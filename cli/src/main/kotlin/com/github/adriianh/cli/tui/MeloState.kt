package com.github.adriianh.cli.tui

import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.SimilarTrack
import com.github.adriianh.core.domain.model.Track
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
    val similarTracks: List<SimilarTrack> = emptyList(),
    val artworkData: ImageData? = null,

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
)