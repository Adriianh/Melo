package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.util.LrcLine
import com.github.adriianh.core.domain.model.FavoriteEntity
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.LyricsTranslationMode
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.image.ImageData

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