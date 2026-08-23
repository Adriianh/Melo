package com.github.adriianh.melo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.usecase.playback.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.search.GetArtistRadioUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
import com.github.adriianh.core.domain.usecase.search.GetRelatedTracksUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QueueViewModel(
    private val manager: PlaybackManager,
    private val searchTracksUseCase: SearchTracksUseCase? = null,
    private val getRadioUseCase: GetRadioUseCase? = null,
    private val getArtistRadioUseCase: GetArtistRadioUseCase? = null,
    private val getRelatedTracksUseCase: GetRelatedTracksUseCase? = null,
    private val getRecentTracksUseCase: GetRecentTracksUseCase? = null,
) : ViewModel() {
    val queueState = manager.queueState

    private val _suggestions = MutableStateFlow<List<Track>>(emptyList())
    val suggestions: StateFlow<List<Track>> = _suggestions.asStateFlow()

    private val _isLoadingSuggestions = MutableStateFlow(false)
    val isLoadingSuggestions: StateFlow<Boolean> = _isLoadingSuggestions.asStateFlow()

    init {
        viewModelScope.launch {
            queueState.collectLatest { state ->
                val track = state.currentTrack
                if (track != null) {
                    loadSuggestionsForTrack(track)
                }
            }
        }
    }

    private suspend fun loadSuggestionsForTrack(track: Track) {
        _isLoadingSuggestions.value = true
        try {
            val videoId = track.sourceId ?: track.id.removePrefix("piped:")
            val queueIds = queueState.value.tracks.map { it.id }.toSet()
            val recentIds: Set<String> = try {
                getRecentTracksUseCase?.invoke(30)?.firstOrNull()?.map { it.track.id }?.toSet()
                    .orEmpty()
            } catch (_: Exception) {
                emptySet()
            }

            val related = getRelatedTracksUseCase?.invoke(videoId).orEmpty()
            val radio = getRadioUseCase?.invoke(videoId).orEmpty()
            val search = if (related.isEmpty() && radio.isEmpty() && searchTracksUseCase != null) {
                searchTracksUseCase.invoke(track.artist)
            } else emptyList()

            val combined = (related + radio + search)
                .distinctBy { it.id }
                .filter { it.id != track.id && it.id !in queueIds }

            val fresh = combined.filter { it.id !in recentIds }
            val finalSuggestions = if (fresh.size >= 5) fresh else combined

            _suggestions.value = finalSuggestions.take(12)
        } catch (_: Exception) {
            _suggestions.value = emptyList()
        } finally {
            _isLoadingSuggestions.value = false
        }
    }

    fun refreshSuggestions() {
        val track = queueState.value.currentTrack ?: return
        viewModelScope.launch {
            loadSuggestionsForTrack(track)
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
            val relatedTracks =
                getRelatedTracksUseCase?.invoke(videoId)?.filter { it.id != track.id }.orEmpty()

            val blended = if (relatedTracks.isNotEmpty()) {
                (radioTracks.take(15) + relatedTracks.take(15))
                    .distinctBy { it.id }
                    .shuffled()
            } else {
                radioTracks
            }

            if (blended.isNotEmpty()) {
                manager.setQueue(listOf(track) + blended, 0)
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

    fun removeTrackFromQueue(track: Track) {
        val currentTracks = queueState.value.tracks.toMutableList()
        val idx = currentTracks.indexOfFirst { it.id == track.id }
        if (idx != -1) {
            currentTracks.removeAt(idx)
            val newCurrentIdx = when {
                idx < queueState.value.currentIndex -> queueState.value.currentIndex - 1
                idx == queueState.value.currentIndex -> queueState.value.currentIndex.coerceAtMost(
                    currentTracks.lastIndex
                )

                else -> queueState.value.currentIndex
            }
            manager.setQueue(currentTracks, newCurrentIdx)
        }
    }

    fun toggleShuffle() = manager.toggleShuffle()

    fun toggleRepeat() = manager.toggleRepeat()
}