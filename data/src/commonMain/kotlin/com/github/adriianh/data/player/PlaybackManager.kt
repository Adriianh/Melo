package com.github.adriianh.data.player

import com.github.adriianh.core.domain.manager.DownloadManager
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.player.PlaybackEvent
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.player.PlaybackState
import com.github.adriianh.core.domain.player.QueueState
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.core.domain.provider.AgeRestrictedException
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.repository.SavedSession
import com.github.adriianh.core.domain.repository.StreamCacheRepository
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.playback.RecordPlayUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
import com.github.adriianh.core.domain.usecase.session.ClearSessionUseCase
import com.github.adriianh.core.domain.usecase.session.RestoreSessionUseCase
import com.github.adriianh.core.domain.usecase.session.SaveSessionUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.data.player.PlaybackManagerImpl.Companion.MAX_STREAM_RESOLVE_ATTEMPTS
import com.github.adriianh.data.player.PlaybackManagerImpl.Companion.STREAM_RETRY_DELAY_MS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.ContinuationInterceptor
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

class PlaybackManagerImpl(
    private val meloPlayer: MeloPlayer,
    private val getStreamUseCase: GetStreamUseCase,
    private val scope: CoroutineScope,
    private val getRadioUseCase: GetRadioUseCase? = null,
    private val getSettingsUseCase: GetSettingsUseCase? = null,
    private val downloadManager: DownloadManager? = null,
    private val offlineRepository: OfflineRepository? = null,
    private val recordPlayUseCase: RecordPlayUseCase? = null,
    private val updateSettingsUseCase: UpdateSettingsUseCase? = null,
    private val saveSessionUseCase: SaveSessionUseCase? = null,
    private val restoreSessionUseCase: RestoreSessionUseCase? = null,
    private val clearSessionUseCase: ClearSessionUseCase? = null,
    private val streamCacheRepository: StreamCacheRepository? = null,
    ioDispatcher: CoroutineDispatcher? = null,
) : PlaybackManager {

    private val dispatcher: CoroutineDispatcher =
        ioDispatcher
            ?: (scope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher)
            ?: MeloDispatchers.IO

    override val playbackState: StateFlow<PlaybackState> = meloPlayer.state

    private val _queueState = MutableStateFlow(QueueState())
    override val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    private val _volume = MutableStateFlow(0.75f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()
    private var lastUnmutedVolume: Float = 0.75f

    private val _events = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<PlaybackEvent> = _events.asSharedFlow()

    private val prefetchCache = mutableMapOf<String, String>()
    private val cacheMutex = Mutex()
    private var playJob: Job? = null
    private var prefetchJob: Job? = null
    private var saveVolumeJob: Job? = null
    private var saveSessionJob: Job? = null
    private var isAutoplayFetching = false
    private var lastHandledFinishedTrackId: String? = null
    private var lastHandledErrorTrackId: String? = null
    private var isTrackLoaded = false
    private var pendingRestorePositionMs: Long = 0L
    private var lastSavedPositionMs: Long = 0L

    init {
        meloPlayer.setVolume(_volume.value)
        scope.launch(dispatcher) {
            getSettingsUseCase?.invoke()?.collectLatest { s ->
                val newVol = (s.volume.toFloat() / 100f).coerceIn(0f, 1f)
                if (newVol > 0.05f) {
                    lastUnmutedVolume = newVol
                }
                if (saveVolumeJob?.isActive != true && abs(_volume.value - newVol) > 0.01f) {
                    _volume.value = newVol
                    meloPlayer.setVolume(newVol)
                }
            }
        }
        scope.launch(dispatcher) {
            val session = restoreSessionUseCase?.invoke()
            if (session != null && session.queue.isNotEmpty()) {
                val validIndex = session.queueIndex.coerceIn(0, session.queue.lastIndex)
                _queueState.update {
                    it.copy(tracks = session.queue, currentIndex = validIndex)
                }
                val track = session.queue.getOrNull(validIndex)
                if (track != null) {
                    pendingRestorePositionMs = session.positionMs
                    lastSavedPositionMs = session.positionMs
                    meloPlayer.setIdleTrack(track, session.positionMs)
                }
            }
        }
        scope.launch(dispatcher) {
            meloPlayer.state.collect { state ->
                val trackId = state.currentTrack?.id
                val loadError = state.error
                if (loadError != null && (trackId == null || trackId != lastHandledErrorTrackId)) {
                    lastHandledErrorTrackId = trackId
                    if (trackId != null) {
                        streamCacheRepository?.invalidate(trackId)
                    }
                    emitPlaybackError(loadError)
                } else if (state.isFinished && state.error == null) {
                    if (trackId != null && trackId != lastHandledFinishedTrackId) {
                        lastHandledFinishedTrackId = trackId
                        handleTrackFinished()
                    }
                } else if (state.isPlaying) {
                    lastHandledFinishedTrackId = null
                    lastHandledErrorTrackId = null
                    if (abs(state.progressMs - lastSavedPositionMs) >= 5000) {
                        persistCurrentSession(immediate = true)
                    }
                } else if (isTrackLoaded && !state.isPlaying && !state.isBuffering && state.currentTrack != null) {
                    persistCurrentSession(immediate = true)
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
            persistCurrentSession(immediate = true)
        } else if (playbackState.value.isFinished) {
            meloPlayer.seekTo(0)
            meloPlayer.play()
        } else if (playbackState.value.error != null && _queueState.value.currentTrack != null) {
            playCurrentQueueTrack(initialSeekMs = playbackState.value.progressMs)
        } else if (isTrackLoaded) {
            meloPlayer.play()
        } else if (_queueState.value.currentTrack != null) {
            playCurrentQueueTrack(initialSeekMs = pendingRestorePositionMs)
        }
    }

    override fun seekTo(positionMs: Long) {
        if (!isTrackLoaded) {
            pendingRestorePositionMs = positionMs
            lastSavedPositionMs = positionMs
            val track = _queueState.value.currentTrack
            if (track != null) {
                meloPlayer.setIdleTrack(track, positionMs)
            }
            persistCurrentSession(immediate = false)
        } else {
            meloPlayer.seekTo(positionMs)
            persistCurrentSession(immediate = false)
        }
    }

    override fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _volume.value = clamped
        if (clamped > 0.05f) {
            lastUnmutedVolume = clamped
        }
        meloPlayer.setVolume(clamped)
        getSettingsUseCase?.let { getSettings ->
            updateSettingsUseCase?.let { updateSettings ->
                saveVolumeJob?.cancel()
                saveVolumeJob = scope.launch(dispatcher) {
                    delay(300.milliseconds)
                    val currentSettings = getSettings.getSnapshot()
                    val volInt = (clamped * 100).toInt()
                    if (currentSettings.volume != volInt) {
                        updateSettings(currentSettings.copy(volume = volInt))
                    }
                }
            }
        }
    }

    override fun toggleMute() {
        if (_volume.value > 0.01f) {
            lastUnmutedVolume = _volume.value
            setVolume(0f)
        } else {
            setVolume(lastUnmutedVolume)
        }
    }

    override fun release() {
        persistCurrentSession(immediate = true)
        playJob?.cancel()
        prefetchJob?.cancel()
        saveVolumeJob?.cancel()
        saveSessionJob?.cancel()
        meloPlayer.release()
    }

    override fun setQueue(tracks: List<Track>, startIndex: Int) {
        scope.launch(dispatcher) { cacheMutex.withLock { prefetchCache.clear() } }
        val validIndex = if (tracks.isEmpty()) -1 else startIndex.coerceIn(0, tracks.lastIndex)
        _queueState.update {
            it.copy(tracks = tracks, currentIndex = validIndex, userQueueCount = 0)
        }
        if (validIndex >= 0) {
            playCurrentQueueTrack()
        } else {
            meloPlayer.stop()
            persistCurrentSession(immediate = true)
        }
    }

    override fun addToQueue(track: Track) {
        val shouldAutoStart =
            _queueState.value.currentIndex < 0 || _queueState.value.tracks.isEmpty()
        _queueState.update { current ->
            if (shouldAutoStart) {
                current.copy(tracks = listOf(track), currentIndex = 0, userQueueCount = 0)
            } else {
                val insertIndex =
                    (current.currentIndex + current.userQueueCount + 1).coerceAtMost(current.tracks.size)
                val newTracks = current.tracks.toMutableList().apply { add(insertIndex, track) }
                current.copy(tracks = newTracks, userQueueCount = current.userQueueCount + 1)
            }
        }
        if (shouldAutoStart) {
            playCurrentQueueTrack()
        } else {
            persistCurrentSession(immediate = false)
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
        persistCurrentSession(immediate = false)
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
            val manualStart = it.currentIndex + 1
            val manualEnd = it.currentIndex + it.userQueueCount
            val newCount = if (index in manualStart..manualEnd && it.userQueueCount > 0) {
                it.userQueueCount - 1
            } else {
                it.userQueueCount
            }
            it.copy(tracks = newTracks, currentIndex = newIndex, userQueueCount = newCount)
        }
        if (wasCurrent) {
            if (_queueState.value.tracks.isEmpty()) {
                meloPlayer.stop()
                persistCurrentSession(immediate = true)
            } else {
                playCurrentQueueTrack()
            }
        } else {
            persistCurrentSession(immediate = true)
        }
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val q = _queueState.value
        if (fromIndex < 0 || fromIndex >= q.tracks.size || toIndex < 0 || toIndex >= q.tracks.size || fromIndex == toIndex) return

        val manualStart = q.currentIndex + 1
        val manualEnd = q.currentIndex + q.userQueueCount
        val fromInBlock = fromIndex in manualStart..manualEnd
        val toInBlock = toIndex in manualStart..manualEnd
        val newCount = when {
            fromInBlock && toInBlock -> q.userQueueCount
            fromInBlock -> (q.userQueueCount - 1).coerceAtLeast(0)
            toInBlock -> q.userQueueCount + 1
            else -> q.userQueueCount
        }

        _queueState.update { current ->
            val newTracks = current.tracks.toMutableList()
            val item = newTracks.removeAt(fromIndex)
            newTracks.add(toIndex, item)

            val newCurrentIndex = when (current.currentIndex) {
                fromIndex -> toIndex
                in (fromIndex + 1)..toIndex -> current.currentIndex - 1
                in toIndex..<fromIndex -> current.currentIndex + 1
                else -> current.currentIndex
            }
            current.copy(
                tracks = newTracks,
                currentIndex = newCurrentIndex,
                userQueueCount = newCount
            )
        }
        persistCurrentSession(immediate = false)
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
        _queueState.update { current ->
            val diff = nextIndex - current.currentIndex
            val newCount = when {
                diff == 1 && current.userQueueCount > 0 -> current.userQueueCount - 1
                diff != 0 -> 0
                else -> current.userQueueCount
            }
            current.copy(currentIndex = nextIndex, userQueueCount = newCount)
        }
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
                q.copy(
                    tracks = shuffled,
                    currentIndex = 0,
                    shuffleEnabled = true,
                    userQueueCount = 0
                )
            }
        }
        persistCurrentSession(immediate = false)
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

    private fun playCurrentQueueTrack(initialSeekMs: Long? = null) {
        val track = _queueState.value.currentTrack ?: return
        val targetSeek = initialSeekMs
            ?: if (!isTrackLoaded && pendingRestorePositionMs > 0) pendingRestorePositionMs else 0L
        val replayingLoadedTrack =
            isTrackLoaded && playbackState.value.currentTrack?.id == track.id
        if (!replayingLoadedTrack) {
            meloPlayer.stop()
        }
        isTrackLoaded = true
        lastHandledFinishedTrackId = null
        lastHandledErrorTrackId = null
        pendingRestorePositionMs = 0L
        playJob?.cancel()
        playJob = scope.launch(dispatcher) {
            val isActivelyDownloading = downloadManager?.activeDownloads?.value?.let { activeMap ->
                activeMap.isNotEmpty() && (activeMap.containsKey(track.id) || activeMap.containsKey(
                    track.id.removePrefix("piped:")
                ))
            } == true
            if (isActivelyDownloading) {
                if (_queueState.value.hasNext) {
                    playNext()
                } else {
                    meloPlayer.stop()
                }
                return@launch
            }

            val settings = getSettingsUseCase?.invoke()?.firstOrNull()
            val isOfflineMode = settings?.offlineMode == true
            val offlineTrack = offlineRepository?.getOfflineTrack(track.id)
            val isTrackAvailableOffline = track.id.startsWith("local:") ||
                    (offlineTrack?.downloadStatus == DownloadStatus.COMPLETED)

            if (isOfflineMode && !isTrackAvailableOffline) {
                val nextOfflineIndex = findNextOfflineTrackIndex(_queueState.value.currentIndex)
                if (nextOfflineIndex != null) {
                    _queueState.update { it.copy(currentIndex = nextOfflineIndex) }
                    playCurrentQueueTrack()
                    return@launch
                }
            }

            val cachedUrl = cacheMutex.withLock { prefetchCache.remove(track.id) }
            val url = try {
                cachedUrl
                    ?: streamCacheRepository?.getCachedUrl(track.id, STREAM_URL_TTL_MS)
                    ?: resolveStreamWithRetry(track)
            } catch (_: AgeRestrictedException) {
                emitPlaybackError("Track is age-restricted (sign-in required), skipping...")
                skipCurrentQueueTrack()
                return@launch
            }

            if (url == null) {
                emitPlaybackError("Stream not available, skipping...")
                skipCurrentQueueTrack()
                return@launch
            }

            if (!url.startsWith("file:")) {
                streamCacheRepository?.cacheUrl(track.id, url)
            }

            meloPlayer.load(url, track, targetSeek)
            emitPlaybackEvent(PlaybackEvent.TrackStarted(track, positionMs = targetSeek))
            persistCurrentSession(immediate = true)

            launch(dispatcher) {
                try {
                    recordPlayUseCase?.invoke(track)
                } catch (_: Exception) {
                }
            }

            if (!track.id.startsWith("local:") && !url.startsWith("file:")) {
                launch(dispatcher) {
                    delay(4000.milliseconds)
                    downloadManager?.cacheTrack(track)
                }
            }

            if (track.sourceId == null) {
                launch(dispatcher) {
                    try {
                        val sid = getStreamUseCase.resolveSourceId(track)
                        if (sid != null) {
                            _queueState.update { current ->
                                val updatedTracks = current.tracks.map {
                                    if (it.id == track.id) it.copy(sourceId = sid) else it
                                }
                                current.copy(tracks = updatedTracks)
                            }
                        }
                    } catch (_: Throwable) {
                    }
                }
            }

            schedulePrefetch()
        }
    }

    private fun schedulePrefetch() {
        prefetchJob?.cancel()
        val q = _queueState.value
        if (shouldPrefetchRadio(q)) {
            handleAutoplay(forcePlayNext = false)
        }

        val queueTracks = q.tracks
        val nextIndices = (q.currentIndex + 1 until minOf(queueTracks.size, q.currentIndex + 3))
        if (nextIndices.isEmpty()) return

        prefetchJob = scope.launch(dispatcher) {
            delay(750.milliseconds)
            for (idx in nextIndices) {
                val nextTrack = queueTracks.getOrNull(idx) ?: continue
                val alreadyCached = cacheMutex.withLock { nextTrack.id in prefetchCache }
                if (!alreadyCached) {
                    val url = getStreamUseCase(nextTrack) ?: continue
                    if (!url.startsWith("file:")) {
                        streamCacheRepository?.cacheUrl(nextTrack.id, url)
                    }
                    cacheMutex.withLock { prefetchCache[nextTrack.id] = url }
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

    /**
     * Resolves the stream URL with up to [MAX_STREAM_RESOLVE_ATTEMPTS] attempts,
     * waiting [STREAM_RETRY_DELAY_MS] between failures. Returns null when the
     * stream could not be resolved after all retries.
     */
    private suspend fun resolveStreamWithRetry(track: Track): String? {
        var attempts = 0
        while (attempts < MAX_STREAM_RESOLVE_ATTEMPTS) {
            try {
                val resolved = getStreamUseCase(track)
                if (resolved != null) return resolved
            } catch (e: AgeRestrictedException) {
                throw e
            } catch (_: Exception) {
                // Transient failure — retry below.
            }
            attempts++
            if (attempts < MAX_STREAM_RESOLVE_ATTEMPTS) {
                delay(STREAM_RETRY_DELAY_MS.milliseconds)
            }
        }
        return null
    }

    /**
     * Advances playback past the current track when it cannot be played:
     * prefers the next offline track (offline mode), then the next queued
     * track, and finally falls back to radio autoplay / stop.
     */
    private suspend fun skipCurrentQueueTrack() {
        val nextOfflineIndex = findNextOfflineTrackIndex(_queueState.value.currentIndex)
        if (nextOfflineIndex != null) {
            _queueState.update { it.copy(currentIndex = nextOfflineIndex) }
            playCurrentQueueTrack()
        } else if (_queueState.value.hasNext) {
            playNext()
        } else {
            handleAutoplay(forcePlayNext = true)
        }
    }

    private fun emitPlaybackEvent(event: PlaybackEvent) {
        _events.tryEmit(event)
    }

    private fun emitPlaybackError(message: String) {
        emitPlaybackEvent(PlaybackEvent.Error(message))
    }

    /**
     * True when the queue is close enough to its end to fetch radio continuations
     * ahead of time, so playback transitions seamlessly without an interruption.
     */
    private fun shouldPrefetchRadio(q: QueueState): Boolean {
        if (getRadioUseCase == null || q.currentIndex < 0 || q.tracks.isEmpty()) return false
        return q.currentIndex >= q.tracks.size - RADIO_AUTOPLAY_MARGIN
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

                val resolvedSourceId = try {
                    getStreamUseCase.resolveSourceId(currentTrack)
                } catch (_: Throwable) {
                    null
                }

                if (resolvedSourceId != null && currentTrack.sourceId == null) {
                    _queueState.update { current ->
                        val updatedTracks = current.tracks.map {
                            if (it.id == currentTrack.id) it.copy(sourceId = resolvedSourceId) else it
                        }
                        current.copy(tracks = updatedTracks)
                    }
                }

                val videoId = resolvedSourceId
                    ?: currentTrack.sourceId
                    ?: (if (currentTrack.id.startsWith("piped:")) currentTrack.id.removePrefix("piped:") else null)
                    ?: currentTrack.id

                val radioTracks = getRadioUseCase(videoId).filter { rec ->
                    _queueState.value.tracks.none {
                        it.id == rec.id ||
                                (!it.sourceId.isNullOrBlank() && it.sourceId == rec.sourceId) ||
                                (it.title.equals(rec.title, ignoreCase = true) && it.artist.equals(
                                    rec.artist,
                                    ignoreCase = true
                                ))
                    }
                }

                if (radioTracks.isNotEmpty()) {
                    _queueState.update { current ->
                        val manualEnd =
                            current.currentIndex + current.userQueueCount + 1
                        val newTracks = current.tracks.toMutableList()
                        newTracks.addAll(manualEnd.coerceAtMost(current.tracks.size), radioTracks)
                        current.copy(tracks = newTracks)
                    }
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

    private fun handleTrackFinished() {
        if (_queueState.value.hasNext) {
            playNext()
        } else {
            handleAutoplay(forcePlayNext = true)
        }
    }

    private fun persistCurrentSession(immediate: Boolean = false) {
        if (saveSessionUseCase == null) return
        saveSessionJob?.cancel()
        val action = suspend {
            val q = _queueState.value
            if (q.tracks.isEmpty() || q.currentIndex < 0) {
                clearSessionUseCase?.invoke()
            } else {
                val currentPos = if (!isTrackLoaded && pendingRestorePositionMs > 0) {
                    pendingRestorePositionMs
                } else {
                    playbackState.value.progressMs
                }
                lastSavedPositionMs = currentPos
                saveSessionUseCase(
                    SavedSession(
                        queue = q.tracks,
                        queueIndex = q.currentIndex,
                        positionMs = currentPos,
                    )
                )
            }
        }
        if (immediate) {
            scope.launch(dispatcher) { action() }
        } else {
            saveSessionJob = scope.launch(dispatcher) {
                delay(1000.milliseconds)
                action()
            }
        }
    }

    private companion object {
        private const val STREAM_URL_TTL_MS = 3 * 60 * 60 * 1000L

        /** Maximum attempts to resolve a stream before skipping the track. */
        private const val MAX_STREAM_RESOLVE_ATTEMPTS = 3

        /** Delay between stream resolution retries. */
        private const val STREAM_RETRY_DELAY_MS = 700L

        /**
         * How close to the end of the queue (tracks remaining) the radio
         * autoplay fetch is triggered, so the radio continues seamlessly.
         */
        private const val RADIO_AUTOPLAY_MARGIN = 5
    }
}