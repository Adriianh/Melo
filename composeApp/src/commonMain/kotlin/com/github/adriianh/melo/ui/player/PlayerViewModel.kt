package com.github.adriianh.melo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.melo.util.PlayerUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PlayerViewModel(private val manager: PlaybackManager) : ViewModel() {
    val playbackState = manager.playbackState

    val uiState: StateFlow<PlayerUiState> = combine(
        manager.playbackState,
        manager.queueState
    ) { playback, queue ->
        PlayerUiState.from(playback, queue)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerUiState())

    fun togglePlayPause() = manager.togglePlayPause()

    fun seekTo(positionMs: Long) = manager.seekTo(positionMs)

    fun playNext() = manager.playNext()

    fun playPrevious() = manager.playPrevious()

    fun toggleShuffle() = manager.toggleShuffle()

    fun toggleRepeat() = manager.toggleRepeat()
}