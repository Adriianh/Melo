package com.github.adriianh.melo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.domain.usecase.library.ToggleLikeTrackUseCase
import com.github.adriianh.core.domain.usecase.playback.RecordPlayUseCase
import com.github.adriianh.melo.util.AccentColorExtractor
import com.github.adriianh.melo.util.AccentPalette
import com.github.adriianh.melo.util.PlayerUiState
import io.ktor.client.HttpClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModel(
    private val manager: PlaybackManager,
    private val httpClient: HttpClient,
    private val toggleLikeTrackUseCase: ToggleLikeTrackUseCase,
    private val recordPlayUseCase: RecordPlayUseCase,
    private val musicProvider: MusicProvider,
) : ViewModel() {
    val playbackState = manager.playbackState

    private val _accentPalette = MutableStateFlow(AccentColorExtractor.fallback)
    val accentPalette: StateFlow<AccentPalette> = _accentPalette.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)

    private val _artistDetails = MutableStateFlow<SearchResult.Artist?>(null)
    val artistDetails: StateFlow<SearchResult.Artist?> = _artistDetails.asStateFlow()

    val uiState: StateFlow<PlayerUiState> = combine(
        manager.playbackState,
        manager.queueState,
        _accentPalette,
        _isFavorite,
    ) { playback, queue, accent, isFavorite ->
        PlayerUiState.from(playback, queue, accent.dominant).copy(isFavorite = isFavorite)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerUiState())

    init {
        viewModelScope.launch {
            manager.playbackState
                .map { it.currentTrack?.artworkUrl }
                .distinctUntilChanged()
                .flatMapLatest { artworkUrl ->
                    if (artworkUrl.isNullOrBlank()) {
                        flowOf(AccentColorExtractor.fallback)
                    } else {
                        flow { emit(AccentColorExtractor.fromImageUrl(artworkUrl, httpClient)) }
                    }
                }
                .collectLatest { palette ->
                    _accentPalette.value = palette
                }
        }
        viewModelScope.launch {
            manager.playbackState
                .map { it.currentTrack?.sourceId }
                .distinctUntilChanged()
                .collectLatest {
                    _isFavorite.value = false
                }
        }
        viewModelScope.launch {
            manager.playbackState
                .map { it.currentTrack }
                .distinctUntilChanged()
                .collectLatest { track ->
                    if (track != null) {
                        recordPlayUseCase(track)
                    }
                }
        }
        viewModelScope.launch {
            manager.playbackState
                .map { it.currentTrack?.artist?.takeIf { a -> a.isNotBlank() } }
                .distinctUntilChanged()
                .collectLatest { artistName ->
                    if (artistName != null) {
                        loadArtistDetails(artistName)
                    } else {
                        _artistDetails.value = null
                    }
                }
        }
    }

    private suspend fun loadArtistDetails(artistName: String) {
        runCatching {
            val results = musicProvider.searchArtists(artistName)
            val first = results.firstOrNull() ?: return@runCatching null
            musicProvider.getArtistDetails(first.id)
        }.onSuccess { details ->
            _artistDetails.value = details
        }.onFailure {
            _artistDetails.value = null
        }
    }

    fun togglePlayPause() = manager.togglePlayPause()

    fun seekTo(positionMs: Long) = manager.seekTo(positionMs)

    fun playNext() = manager.playNext()

    fun playPrevious() = manager.playPrevious()

    fun toggleShuffle() = manager.toggleShuffle()

    fun toggleRepeat() = manager.toggleRepeat()

    fun toggleFavorite() {
        val track = manager.playbackState.value.currentTrack ?: return
        val videoId = track.sourceId ?: return
        val newFavorite = !_isFavorite.value
        _isFavorite.value = newFavorite
        viewModelScope.launch {
            val result = toggleLikeTrackUseCase(videoId, newFavorite)
            if (result.isFailure) {
                _isFavorite.value = !newFavorite
            }
        }
    }
}
