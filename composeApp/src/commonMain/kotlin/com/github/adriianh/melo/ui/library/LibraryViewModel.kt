package com.github.adriianh.melo.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.usecase.library.GetAccountProfileUseCase
import com.github.adriianh.core.domain.usecase.library.GetLikedSongsUseCase
import com.github.adriianh.core.domain.usecase.library.GetRemoteHistoryUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserAlbumsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserArtistsUseCase
import com.github.adriianh.core.domain.usecase.library.GetUserPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.library.ToggleLikeTrackUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class LibraryTab(val label: String) {
    PLAYLISTS("Playlists"),
    LIKED("Canciones que me gustan"),
    ARTISTS("Artistas"),
    ALBUMS("Álbumes"),
    HISTORY("Historial")
}

data class LibraryUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val profile: AccountProfile? = null,
    val selectedTab: LibraryTab = LibraryTab.PLAYLISTS,
    val playlists: List<SearchResult.Playlist> = emptyList(),
    val likedSongs: List<Track> = emptyList(),
    val artists: List<SearchResult.Artist> = emptyList(),
    val albums: List<SearchResult.Album> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
    val error: String? = null,
)

class LibraryViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val getAccountProfileUseCase: GetAccountProfileUseCase,
    private val getUserPlaylistsUseCase: GetUserPlaylistsUseCase,
    private val getLikedSongsUseCase: GetLikedSongsUseCase,
    private val getUserArtistsUseCase: GetUserArtistsUseCase,
    private val getUserAlbumsUseCase: GetUserAlbumsUseCase,
    private val getRemoteHistoryUseCase: GetRemoteHistoryUseCase,
    private val toggleLikeTrackUseCase: ToggleLikeTrackUseCase,
    private val playbackManager: PlaybackManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSettingsUseCase().collectLatest { settings ->
                val loggedIn = !settings.sessionCookies.isNullOrBlank()
                _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
                if (loggedIn) {
                    refreshAll()
                }
            }
        }
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun refreshAll() {
        if (!_uiState.value.isLoggedIn) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val profileResult = getAccountProfileUseCase()
            val playlistsResult = getUserPlaylistsUseCase()
            val likedSongsResult = getLikedSongsUseCase()
            val artistsResult = getUserArtistsUseCase()
            val albumsResult = getUserAlbumsUseCase()
            val historyResult = getRemoteHistoryUseCase()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                profile = profileResult.getOrNull() ?: _uiState.value.profile,
                playlists = playlistsResult.getOrDefault(emptyList()),
                likedSongs = likedSongsResult.getOrDefault(emptyList()),
                artists = artistsResult.getOrDefault(emptyList()),
                albums = albumsResult.getOrDefault(emptyList()),
                history = historyResult.getOrDefault(emptyList()),
                error = if (playlistsResult.isFailure && likedSongsResult.isFailure) {
                    "No se pudo cargar la biblioteca. Verifica tu conexión o sesión."
                } else null
            )
        }
    }

    fun playTrack(track: Track, queue: List<Track> = emptyList()) {
        if (queue.isNotEmpty()) {
            val index = queue.indexOf(track).coerceAtLeast(0)
            playbackManager.setQueue(queue, index)
        } else {
            playbackManager.playTrack(track)
        }
    }

    fun toggleLike(trackId: String, isLiked: Boolean) {
        viewModelScope.launch {
            toggleLikeTrackUseCase(trackId, isLiked)
            // Refresh liked songs in background
            val updatedLiked = getLikedSongsUseCase().getOrNull()
            if (updatedLiked != null) {
                _uiState.value = _uiState.value.copy(likedSongs = updatedLiked)
            }
        }
    }
}