package com.github.adriianh.melo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.network.NetworkMonitor
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.usecase.offline.GetOfflineSuggestionsUseCase
import com.github.adriianh.core.domain.usecase.playback.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.search.GetArtistRadioUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
import com.github.adriianh.core.domain.usecase.search.GetRelatedTracksUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QueueViewModel(
    private val manager: PlaybackManager,
    private val searchTracksUseCase: SearchTracksUseCase? = null,
    private val getRadioUseCase: GetRadioUseCase? = null,
    private val getArtistRadioUseCase: GetArtistRadioUseCase? = null,
    private val getRelatedTracksUseCase: GetRelatedTracksUseCase? = null,
    private val getRecentTracksUseCase: GetRecentTracksUseCase? = null,
    private val getOfflineSuggestionsUseCase: GetOfflineSuggestionsUseCase? = null,
    private val getSettingsUseCase: GetSettingsUseCase? = null,
    private val networkMonitor: NetworkMonitor? = null,
) : ViewModel() {
    val queueState = manager.queueState

    private val _suggestions = MutableStateFlow<List<Track>>(emptyList())
    val suggestions: StateFlow<List<Track>> = _suggestions.asStateFlow()

    private val _isLoadingSuggestions = MutableStateFlow(false)
    val isLoadingSuggestions: StateFlow<Boolean> = _isLoadingSuggestions.asStateFlow()

    private var loadSuggestionsJob: Job? = null

    init {
        viewModelScope.launch {
            var lastTrackId: String? = null
            queueState.collectLatest { state ->
                val track = state.currentTrack
                if (track != null) {
                    if (track.id != lastTrackId) {
                        lastTrackId = track.id
                        reloadSuggestions(track)
                    }
                } else {
                    lastTrackId = null
                    loadSuggestionsJob?.cancel()
                    _suggestions.value = emptyList()
                    _isLoadingSuggestions.value = false
                }
            }
        }

        if (getSettingsUseCase != null || networkMonitor != null) {
            viewModelScope.launch {
                val offlineSettingsFlow =
                    getSettingsUseCase?.invoke()?.map { it.offlineMode } ?: MutableStateFlow(false)
                val isOnlineFlow = networkMonitor?.isOnline ?: MutableStateFlow(true)
                combine(offlineSettingsFlow, isOnlineFlow) { offlineSetting, isOnline ->
                    offlineSetting || !isOnline
                }.distinctUntilChanged()
                    .drop(1)
                    .collectLatest { _ ->
                        val track = queueState.value.currentTrack
                        if (track != null) {
                            reloadSuggestions(track)
                        }
                    }
            }
        }
    }

    private fun isEffectivelyOffline(): Boolean {
        val offlineSetting = getSettingsUseCase?.invoke()?.value?.offlineMode ?: false
        val networkOffline = networkMonitor?.isOnline?.value == false
        return offlineSetting || networkOffline
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> safeCall(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    private fun reloadSuggestions(track: Track) {
        loadSuggestionsJob?.cancel()
        loadSuggestionsJob =
            viewModelScope.launch {
                _suggestions.value = emptyList()
                loadSuggestionsForTrack(track)
            }
    }

    private suspend fun loadSuggestionsForTrack(track: Track) {
        _isLoadingSuggestions.value = true
        val queueIds =
            queueState.value.tracks
                .map { it.id }
                .toSet()
        try {
            if (isEffectivelyOffline()) {
                _suggestions.value =
                    safeCall { getOfflineSuggestionsUseCase?.invoke(track, queueIds, 12) }.orEmpty()
                return
            }

            val videoId = track.sourceId ?: track.id.removePrefix("piped:")
            val recentIds: Set<String> =
                safeCall {
                    getRecentTracksUseCase
                        ?.invoke(30)
                        ?.firstOrNull()
                        ?.map { it.track.id }
                        ?.toSet()
                }.orEmpty()

            val related = safeCall { getRelatedTracksUseCase?.invoke(videoId) }.orEmpty()
            val radio = safeCall { getRadioUseCase?.invoke(videoId) }.orEmpty()
            val search =
                if (related.isEmpty() && radio.isEmpty() && searchTracksUseCase != null) {
                    safeCall { searchTracksUseCase.invoke(track.artist) }.orEmpty()
                } else {
                    emptyList()
                }

            val combined =
                (related + radio + search)
                    .distinctBy { it.id }
                    .filter { it.id != track.id && it.id !in queueIds }

            val finalSuggestions =
                if (combined.isEmpty() && getOfflineSuggestionsUseCase != null) {
                    safeCall { getOfflineSuggestionsUseCase(track, queueIds, 12) }.orEmpty()
                } else {
                    val fresh = combined.filter { it.id !in recentIds }
                    if (fresh.size >= 5) fresh else combined
                }

            _suggestions.value = finalSuggestions.take(12)
        } finally {
            _isLoadingSuggestions.value = false
        }
    }

    fun refreshSuggestions() {
        val track = queueState.value.currentTrack ?: return
        reloadSuggestions(track)
    }

    fun playTrack(track: Track) = manager.playTrack(track)

    fun playTracks(
        tracks: List<Track>,
        startIndex: Int = 0,
    ) {
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
            if (isEffectivelyOffline()) {
                val offlineTracks =
                    safeCall { getOfflineSuggestionsUseCase?.invoke(track, setOf(track.id), 25) }.orEmpty()
                if (offlineTracks.isNotEmpty()) {
                    manager.setQueue(listOf(track) + offlineTracks, 0)
                }
                return@launch
            }

            val videoId = track.sourceId ?: track.id.removePrefix("piped:")
            val radioTracks =
                safeCall { getRadioUseCase?.invoke(videoId)?.filter { it.id != track.id } }.orEmpty()
            val relatedTracks =
                safeCall { getRelatedTracksUseCase?.invoke(videoId)?.filter { it.id != track.id } }.orEmpty()

            val blended =
                if (relatedTracks.isNotEmpty()) {
                    (radioTracks.take(15) + relatedTracks.take(15))
                        .distinctBy { it.id }
                        .shuffled()
                } else {
                    radioTracks
                }

            val finalTracks =
                if (blended.isEmpty() && getOfflineSuggestionsUseCase != null) {
                    safeCall { getOfflineSuggestionsUseCase.invoke(track, setOf(track.id), 25) }.orEmpty()
                } else {
                    blended
                }

            if (finalTracks.isNotEmpty()) {
                manager.setQueue(listOf(track) + finalTracks, 0)
            }
        }
    }

    fun startArtistRadio(
        artistId: String,
        fallbackTracks: List<Track> = emptyList(),
    ) {
        viewModelScope.launch {
            val radioTracks =
                if (isEffectivelyOffline()) {
                    emptyList()
                } else {
                    safeCall { getArtistRadioUseCase?.invoke(artistId) }.orEmpty()
                }
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

    fun insertTrackNext(track: Track) {
        val index = queueState.value.currentIndex + 1
        manager.insertToQueue(track, index)
        _suggestions.update { current -> current.filter { it.id != track.id } }
    }

    fun playTrackInQueue(track: Track) = manager.playTrackInQueue(track)

    fun insertTrackAt(
        track: Track,
        index: Int,
    ) {
        manager.insertToQueue(track, index)
    }

    fun removeTrackAt(index: Int): Track? {
        val currentTracks = queueState.value.tracks.toMutableList()
        if (index < 0 || index >= currentTracks.size) return null
        val track = currentTracks[index]
        manager.removeFromQueue(index)
        return track
    }

    fun removeTrackFromQueue(track: Track): Int {
        val currentTracks = queueState.value.tracks.toMutableList()
        val idx = currentTracks.indexOfFirst { it.id == track.id }
        if (idx != -1) {
            manager.removeFromQueue(idx)
        }
        return idx
    }

    fun moveTrack(
        fromIndex: Int,
        toIndex: Int,
    ) {
        manager.moveQueueItem(fromIndex, toIndex)
    }

    fun toggleShuffle() = manager.toggleShuffle()

    fun toggleRepeat() = manager.toggleRepeat()
}
