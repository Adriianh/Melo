package com.github.adriianh.melo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QueueViewModel(
    private val manager: PlaybackManager,
    private val searchTracksUseCase: SearchTracksUseCase? = null,
) : ViewModel() {
    val queueState = manager.queueState

    private val _suggestions = MutableStateFlow<List<Track>>(emptyList())
    val suggestions: StateFlow<List<Track>> = _suggestions.asStateFlow()

    init {
        viewModelScope.launch {
            queueState.collectLatest { state ->
                val track = state.currentTrack
                if (track != null && searchTracksUseCase != null) {
                    try {
                        val results = searchTracksUseCase(track.artist)
                        _suggestions.value = results.filter { it.id != track.id }.take(6)
                    } catch (_: Exception) {
                        _suggestions.value = emptyList()
                    }
                }
            }
        }
    }

    fun playTrack(track: Track) = manager.playTrack(track)

    fun addToQueue(track: Track) {
        manager.addToQueue(track)
        _suggestions.update { current -> current.filter { it.id != track.id } }
    }

    fun playTrackInQueue(track: Track) = manager.playTrackInQueue(track)

    fun toggleShuffle() = manager.toggleShuffle()

    fun toggleRepeat() = manager.toggleRepeat()
}