package com.github.adriianh.melo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.usecase.search.GetArtistRadioUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
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
    private val getRadioUseCase: GetRadioUseCase? = null,
    private val getArtistRadioUseCase: GetArtistRadioUseCase? = null,
) : ViewModel() {
    val queueState = manager.queueState

    private val _suggestions = MutableStateFlow<List<Track>>(emptyList())
    val suggestions: StateFlow<List<Track>> = _suggestions.asStateFlow()

    init {
        viewModelScope.launch {
            queueState.collectLatest { state ->
                val track = state.currentTrack
                if (track != null) {
                    try {
                        val videoId = track.sourceId ?: track.id.removePrefix("piped:")
                        val radioTracks = getRadioUseCase?.invoke(videoId)
                        if (!radioTracks.isNullOrEmpty()) {
                            val currentQueueIds = state.tracks.map { it.id }.toSet()
                            _suggestions.value =
                                radioTracks.filter { it.id !in currentQueueIds }.take(10)
                        } else if (searchTracksUseCase != null) {
                            val results = searchTracksUseCase(track.artist)
                            _suggestions.value = results.filter { it.id != track.id }.take(6)
                        }
                    } catch (_: Exception) {
                        _suggestions.value = emptyList()
                    }
                }
            }
        }
    }

    fun playTrack(track: Track) = manager.playTrack(track)

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val validIndex = startIndex.coerceIn(0, tracks.lastIndex)
        manager.setQueue(tracks, validIndex)
    }

    fun playShuffled(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val shuffled = tracks.shuffled()
        manager.setQueue(shuffled, 0)
    }

    fun startRadio(track: Track) {
        viewModelScope.launch {
            manager.playTrack(track)
            val videoId = track.sourceId ?: track.id.removePrefix("piped:")
            val radioTracks =
                getRadioUseCase?.invoke(videoId)?.filter { it.id != track.id }.orEmpty()
            if (radioTracks.isNotEmpty()) {
                manager.setQueue(listOf(track) + radioTracks, 0)
            }
        }
    }

    fun startArtistRadio(
        artistId: String,
        fallbackTracks: List<Track> = emptyList()
    ) {
        viewModelScope.launch {
            val radioTracks = getArtistRadioUseCase?.invoke(artistId).orEmpty()
            val queue = radioTracks.ifEmpty { fallbackTracks }
            if (queue.isNotEmpty()) {
                manager.setQueue(queue, 0)
            }
        }
    }

    fun addToQueue(track: Track) {
        manager.addToQueue(track)
        _suggestions.update { current -> current.filter { it.id != track.id } }
    }

    fun addAllToQueue(tracks: List<Track>) {
        tracks.forEach { manager.addToQueue(it) }
    }

    fun playTrackInQueue(track: Track) = manager.playTrackInQueue(track)

    fun toggleShuffle() = manager.toggleShuffle()

    fun toggleRepeat() = manager.toggleRepeat()
}