package com.github.adriianh.melo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.player.PlaybackManager
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
) : ViewModel() {
    val playbackState = manager.playbackState

    private val _accentPalette = MutableStateFlow<AccentPalette>(AccentColorExtractor.fallback)
    val accentPalette: StateFlow<AccentPalette> = _accentPalette.asStateFlow()

    val uiState: StateFlow<PlayerUiState> = combine(
        manager.playbackState,
        manager.queueState,
        _accentPalette
    ) { playback, queue, accent ->
        PlayerUiState.from(playback, queue, accent.dominant)
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
    }

    fun togglePlayPause() = manager.togglePlayPause()

    fun seekTo(positionMs: Long) = manager.seekTo(positionMs)

    fun playNext() = manager.playNext()

    fun playPrevious() = manager.playPrevious()

    fun toggleShuffle() = manager.toggleShuffle()

    fun toggleRepeat() = manager.toggleRepeat()
}
