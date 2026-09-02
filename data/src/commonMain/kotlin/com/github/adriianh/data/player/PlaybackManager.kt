package com.github.adriianh.data.player

import com.github.adriianh.core.domain.manager.DownloadManager
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.player.PlaybackState
import com.github.adriianh.core.domain.player.QueueState
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaybackManagerImpl(
    private val meloPlayer: MeloPlayer,
    private val getStreamUseCase: GetStreamUseCase,
    private val scope: CoroutineScope,
    private val getRadioUseCase: GetRadioUseCase? = null,
    private val getSettingsUseCase: GetSettingsUseCase? = null,
    private val downloadManager: DownloadManager? = null,
    private val offlineRepository: OfflineRepository? = null,
) : PlaybackManager {

    override val playbackState: StateFlow<PlaybackState> = meloPlayer.state

    private val _queueState = MutableStateFlow(QueueState())
    override val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    private val prefetchCache = mutableMapOf<String, String>()
    private val cacheMutex = Mutex()
    private var prefetchJob: Job? = null
    private var isAutoplayFetching = false
    private var lastHandledFinishedTrackId: String? = null

    init {
        scope.launch {
            meloPlayer.state.collect { state ->
                val trackId = state.currentTrack?.id
                if (state.isFinished && state.error == null) {
                    if (trackId != null && trackId != lastHandledFinishedTrackId) {
                        lastHandledFinishedTrackId = trackId
                        handleTrackFinished()
                    }
                } else if (state.isPlaying) {
                    lastHandledFinishedTrackId = null
                }
            }
        }
    }

    override fun playTrack(track: Track) {
        setQueue(listOf(track), startIndex = 0)
    }

    override fun playTrackInQueue(track: Track) {
        val index = _queueState.value.tracks.indexOfFirst { it.id == track.id }
        if (index != -1) {
            _queueState.update { it.copy(currentIndex = index) }
            playCurrentQueueTrack()
        } else {
            addToQueue(track)
        }
    }

    override fun togglePlayPause() {
        if (playbackState.value.isPlaying) {
            meloPlayer.pause()
        } else if (playbackState.value.currentTrack != null) {
            meloPlayer.play()
        }
    }

    override fun seekTo(positionMs: Long) {
        meloPlayer.seekTo(positionMs)
    }

    override fun release() {
        prefetchJob?.cancel()
        meloPlayer.release()
    }

    override fun setQueue(tracks: List<Track>, startIndex: Int) {
        scope.launch { cacheMutex.withLock { prefetchCache.clear() } }
        val validIndex = if (tracks.isEmpty()) -1 else startIndex.coerceIn(0, tracks.lastIndex)
        _queueState.update { it.copy(tracks = tracks, currentIndex = validIndex) }
        if (validIndex >= 0) {
            playCurrentQueueTrack()
        }
    }

    override fun addToQueue(track: Track) {
        _queueState.update { it.copy(tracks = it.tracks + track) }
        if (_queueState.value.currentIndex == -1) {
            _queueState.update { it.copy(currentIndex = 0) }
            playCurrentQueueTrack()
        }
    }

    override fun insertToQueue(track: Track, index: Int) {
        _queueState.update {
            val safeIndex = index.coerceIn(0, it.tracks.size)
            val newTracks = it.tracks.toMutableList().apply { add(safeIndex, track) }
            val newIndex =
                if (safeIndex <= it.currentIndex) it.currentIndex + 1 else it.currentIndex
            it.copy(tracks = newTracks, currentIndex = newIndex)
        }
    }

    override fun removeFromQueue(index: Int) {
        val q = _queueState.value
        if (index < 0 || index >= q.tracks.size) return
        val wasCurrent = index == q.currentIndex
        _queueState.update {
            val newTracks = it.tracks.toMutableList()
            newTracks.removeAt(index)
            val newIndex = when {
                index < it.currentIndex -> it.currentIndex - 1
                index == it.currentIndex -> it.currentIndex.coerceAtMost(newTracks.lastIndex)
                else -> it.currentIndex
            }
            it.copy(tracks = newTracks, currentIndex = newIndex)
        }
        if (wasCurrent) {
            if (_queueState.value.tracks.isEmpty()) {
                meloPlayer.stop()
            } else {
                playCurrentQueueTrack()
            }
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val q = _queueState.value
        if (fromIndex < 0 || fromIndex >= q.tracks.size || toIndex < 0 || toIndex >= q.tracks.size || fromIndex == toIndex) return
        _queueState.update { current ->
            val newTracks = current.tracks.toMutableList()
            val item = newTracks.removeAt(fromIndex)
            newTracks.add(toIndex, item)

            val newCurrentIndex = when {
                current.currentIndex == fromIndex -> toIndex
                fromIndex < current.currentIndex && toIndex >= current.currentIndex -> current.currentIndex - 1
                fromIndex > current.currentIndex && toIndex <= current.currentIndex -> current.currentIndex + 1
                else -> current.currentIndex
            }
            current.copy(tracks = newTracks, currentIndex = newCurrentIndex)
        }
    }

    override fun playNext() {
        val q = _queueState.value
        if (!q.hasNext) {
            handleAutoplay(forcePlayNext = true)
            return
        }
        val nextIndex = when (q.repeatMode) {
            RepeatMode.ONE -> q.currentIndex
            RepeatMode.ALL -> (q.currentIndex + 1) % q.tracks.size
            RepeatMode.NONE -> q.currentIndex + 1
        }
        _queueState.update { it.copy(currentIndex = nextIndex) }
        playCurrentQueueTrack()
    }

    override fun playPrevious() {
        if (playbackState.value.progressMs > 3000) {
            meloPlayer.seekTo(0)
            return
        }
        val q = _queueState.value
        if (!q.hasPrevious) return
        _queueState.update { it.copy(currentIndex = q.currentIndex - 1) }
        playCurrentQueueTrack()
    }

    override fun toggleShuffle() {
        _queueState.update { q ->
            if (q.shuffleEnabled) {
                q.copy(shuffleEnabled = false)
            } else {
                val shuffled = q.tracks.toMutableList()
                val current = q.currentTrack
                shuffled.shuffle()
                if (current != null) {
                    shuffled.remove(current)
                    shuffled.add(0, current)
                }
                q.copy(tracks = shuffled, currentIndex = 0, shuffleEnabled = true)
            }
        }
    }

    override fun toggleRepeat() {
        _queueState.update {
            it.copy(
                repeatMode = when (it.repeatMode) {
                    RepeatMode.NONE -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.NONE
                }
            )
        }
    }

    private fun playCurrentQueueTrack() {
        val track = _queueState.value.currentTrack ?: return
        scope.launch {
            val settings = getSettingsUseCase?.invoke()?.firstOrNull()
            val isOfflineMode = settings?.offlineMode == true
            val isTrackAvailableOffline = track.id.startsWith("local:") ||
                (offlineRepository?.getOfflineTrack(track.id)?.downloadStatus == DownloadStatus.COMPLETED)

            if (isOfflineMode && !isTrackAvailableOffline) {
                val nextOfflineIndex = findNextOfflineTrackIndex(_queueState.value.currentIndex)
                if (nextOfflineIndex != null) {
                    _queueState.update { it.copy(currentIndex = nextOfflineIndex) }
                    playCurrentQueueTrack()
                    return@launch
                }
            }

            val cachedUrl = cacheMutex.withLock { prefetchCache.remove(track.id) }
            val url = cachedUrl
                ?: getStreamUseCase(track)
                ?: run {
                    val nextOfflineIndex = findNextOfflineTrackIndex(_queueState.value.currentIndex)
                    if (nextOfflineIndex != null) {
                        _queueState.update { it.copy(currentIndex = nextOfflineIndex) }
                        playCurrentQueueTrack()
                    } else if (_queueState.value.hasNext) {
                        playNext()
                    } else {
                        handleAutoplay(forcePlayNext = true)
                    }
                    return@launch
                }
            meloPlayer.load(url, track)

            if (!track.id.startsWith("local:") && !url.startsWith("file:")) {
                launch {
                    downloadManager?.cacheTrack(track)
                }
            }

            schedulePrefetch()
        }
    }

    private fun schedulePrefetch() {
        prefetchJob?.cancel()
        val q = _queueState.value
        if (q.currentIndex >= 0 && q.currentIndex == q.tracks.lastIndex && getRadioUseCase != null) {
            handleAutoplay(forcePlayNext = false)
        }

        val queueTracks = q.tracks
        val nextIndices = (q.currentIndex + 1 until minOf(queueTracks.size, q.currentIndex + 4))
        if (nextIndices.isEmpty()) return

        prefetchJob = scope.launch {
            for (idx in nextIndices) {
                val nextTrack = queueTracks.getOrNull(idx) ?: continue
                val alreadyCached = cacheMutex.withLock { nextTrack.id in prefetchCache }
                if (!alreadyCached) {
                    val url = getStreamUseCase(nextTrack) ?: continue
                    cacheMutex.withLock { prefetchCache[nextTrack.id] = url }
                }
                if (!nextTrack.id.startsWith("local:")) {
                    downloadManager?.cacheTrack(nextTrack)
                }
            }
        }
    }

    private suspend fun findNextOfflineTrackIndex(startIndex: Int): Int? {
        val q = _queueState.value
        for (i in (startIndex + 1 until q.tracks.size)) {
            val t = q.tracks[i]
            if (t.id.startsWith("local:")) return i
            if (offlineRepository?.getOfflineTrack(t.id)?.downloadStatus == DownloadStatus.COMPLETED) {
                return i
            }
        }
        return null
    }

    private fun handleAutoplay(forcePlayNext: Boolean) {
        val currentTrack = _queueState.value.currentTrack ?: return
        if (isAutoplayFetching || getRadioUseCase == null) {
            if (forcePlayNext) meloPlayer.stop()
            return
        }

        isAutoplayFetching = true
        scope.launch {
            try {
                val autoplayEnabled = getSettingsUseCase?.invoke()?.firstOrNull()?.autoplay ?: true
                if (!autoplayEnabled) {
                    if (forcePlayNext) meloPlayer.stop()
                    return@launch
                }

                val videoId = currentTrack.sourceId ?: currentTrack.id.removePrefix("piped:")
                val radioTracks = getRadioUseCase(videoId).filter { rec ->
                    _queueState.value.tracks.none { it.id == rec.id }
                }

                if (radioTracks.isNotEmpty()) {
                    _queueState.update { it.copy(tracks = it.tracks + radioTracks) }
                    if (forcePlayNext) {
                        val nextIdx = _queueState.value.currentIndex + 1
                        if (nextIdx in _queueState.value.tracks.indices) {
                            _queueState.update { it.copy(currentIndex = nextIdx) }
                            playCurrentQueueTrack()
                        }
                    }
                } else if (forcePlayNext) {
                    meloPlayer.stop()
                }
            } catch (_: Exception) {
                if (forcePlayNext) meloPlayer.stop()
            } finally {
                isAutoplayFetching = false
            }
        }
    }

    private fun nextTrackForPrefetch(): Track? {
        val q = _queueState.value
        return when (q.repeatMode) {
            RepeatMode.ONE -> q.currentTrack
            RepeatMode.ALL -> q.tracks.getOrNull((q.currentIndex + 1) % q.tracks.size)
            RepeatMode.NONE -> q.tracks.getOrNull(q.currentIndex + 1)
        }
    }

    private fun handleTrackFinished() {
        if (_queueState.value.hasNext) {
            playNext()
        } else {
            handleAutoplay(forcePlayNext = true)
        }
    }
}