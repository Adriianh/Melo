package com.github.adriianh.data.player

import com.github.adriianh.core.domain.manager.DownloadManager
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.network.NetworkMonitor
import com.github.adriianh.core.domain.player.AudioOutputDevice
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.player.PlaybackEvent
import com.github.adriianh.core.domain.player.PlaybackManager
import com.github.adriianh.core.domain.player.PlaybackState
import com.github.adriianh.core.domain.player.QueueState
import com.github.adriianh.core.domain.provider.AgeRestrictedException
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.repository.StreamCacheRepository
import com.github.adriianh.core.domain.usecase.offline.GetOfflineSuggestionsUseCase
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.playback.RecordPlayUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
import com.github.adriianh.core.domain.usecase.session.ClearSessionUseCase
import com.github.adriianh.core.domain.usecase.session.RestoreSessionUseCase
import com.github.adriianh.core.domain.usecase.session.SaveSessionUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.domain.usecase.settings.UpdateSettingsUseCase
import com.github.adriianh.core.util.MeloDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.coroutines.ContinuationInterceptor

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
    private val networkMonitor: NetworkMonitor? = null,
    ioDispatcher: CoroutineDispatcher? = null,
    getOfflineSuggestionsUseCase: GetOfflineSuggestionsUseCase? = null,
) : PlaybackManager {
    private val dispatcher: CoroutineDispatcher =
        ioDispatcher
            ?: (scope.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher)
            ?: MeloDispatchers.IO

    private val queueController = QueueController()
    private val effectiveOfflineSuggestionsUseCase =
        getOfflineSuggestionsUseCase ?: offlineRepository?.let { GetOfflineSuggestionsUseCase(it) }

    private val audioSettings: AudioSettingsCoordinator =
        AudioSettingsCoordinator(
            scope = scope,
            dispatcher = dispatcher,
            meloPlayer = meloPlayer,
            getSettingsUseCase = getSettingsUseCase,
            updateSettingsUseCase = updateSettingsUseCase,
            onOfflineModeToggledOnline = { recovery.onOfflineModeToggledOnline(recoveryCallbacks) },
        )

    private val recovery: PlaybackRecoveryCoordinator =
        PlaybackRecoveryCoordinator(
            scope = scope,
            dispatcher = dispatcher,
            queueController = queueController,
            meloPlayer = meloPlayer,
            environment =
                RecoveryEnvironment(
                    offlineRepository = offlineRepository,
                    networkMonitor = networkMonitor,
                    isOfflineSettingsEnabled = { audioSettings.isOfflineSettingsEnabled },
                ),
        )

    private val autoplayCoordinator: AutoplayCoordinator =
        AutoplayCoordinator(
            scope = scope,
            dispatcher = dispatcher,
            queueController = queueController,
            providers =
                AutoplayProviders(
                    getStreamUseCase = getStreamUseCase,
                    getRadioUseCase = getRadioUseCase,
                    offlineSuggestionsUseCase = effectiveOfflineSuggestionsUseCase,
                    getSettingsUseCase = getSettingsUseCase,
                ),
            isEffectivelyOffline = { recovery.isEffectivelyOffline },
        )

    private val streamResolver =
        StreamResolver(
            scope = scope,
            dispatcher = dispatcher,
            getStreamUseCase = getStreamUseCase,
            streamCacheRepository = streamCacheRepository,
            downloadManager = downloadManager,
            currentDurationMs = { meloPlayer.state.value.durationMs },
        )

    private val sessionPersistence =
        SessionPersistenceCoordinator(
            scope = scope,
            dispatcher = dispatcher,
            saveSessionUseCase = saveSessionUseCase,
            restoreSessionUseCase = restoreSessionUseCase,
            clearSessionUseCase = clearSessionUseCase,
            getSettingsUseCase = getSettingsUseCase,
        )

    private val recoveryCallbacks: RecoveryCallbacks =
        RecoveryCallbacks(
            onReplayAt = { pos -> playCurrentQueueTrack(initialSeekMs = pos) },
            onJumpToIndex = { idx ->
                queueController.update { it.copy(currentIndex = idx, userQueueCount = 0) }
                playCurrentQueueTrack()
            },
            onPark = ::parkPlayback,
            onSkip = ::skipCurrentQueueTrack,
            onError = ::emitPlaybackError,
        )

    override val playbackState: StateFlow<PlaybackState> = meloPlayer.state
    override val queueState: StateFlow<QueueState> = queueController.queueState
    override val volume: StateFlow<Float> = audioSettings.volume
    override val availableAudioOutputs: StateFlow<List<AudioOutputDevice>> = meloPlayer.availableAudioOutputs
    override val selectedAudioOutputId: StateFlow<String?> = meloPlayer.selectedAudioOutputId

    private val _events = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<PlaybackEvent> = _events.asSharedFlow()

    private var playJob: Job? = null

    @Volatile
    private var lastHandledFinishedTrackId: String? = null
    private var isTrackLoaded = false

    init {
        audioSettings.start()
        sessionPersistence.restore { tracks, validIndex, track, positionMs ->
            queueController.update { it.copy(tracks = tracks, currentIndex = validIndex) }
            meloPlayer.setIdleTrack(track, positionMs)
        }
        observePlayerState()
        recovery.observeNetwork(recoveryCallbacks)
    }

    private fun observePlayerState() {
        scope.launch(dispatcher) {
            meloPlayer.state.collect { state ->
                val trackId = state.currentTrack?.id
                val loadError = state.error
                when {
                    loadError != null -> {
                        if (trackId != null) streamResolver.invalidate(trackId)
                        recovery.handlePlayerError(trackId, state.progressMs, loadError, recoveryCallbacks)
                    }

                    state.isFinished -> {
                        recovery.clearRecovery()
                        val activeTrackId = queueController.current.currentTrack?.id
                        if (trackId != null && trackId == activeTrackId && trackId != lastHandledFinishedTrackId) {
                            lastHandledFinishedTrackId = trackId
                            handleTrackFinished()
                        }
                    }

                    state.isPlaying -> {
                        lastHandledFinishedTrackId = null
                        recovery.clearRecovery(clearLastError = true)
                        if (sessionPersistence.hasProgressDrift(state.progressMs)) {
                            persistCurrentSession(immediate = true)
                        }
                    }

                    isTrackLoaded && !state.isBuffering && state.currentTrack != null -> {
                        persistCurrentSession(immediate = true)
                    }
                }
            }
        }
    }

    override fun playTrack(track: Track) {
        setQueue(listOf(track), startIndex = 0)
    }

    override fun playTrackInQueue(track: Track) {
        val prevIndex = queueController.current.currentIndex
        val index = queueController.selectTrackInQueue(track.id)
        if (index != -1) {
            if (index != prevIndex) sessionPersistence.clearPendingRestore()
            playCurrentQueueTrack()
        } else {
            addToQueue(track)
        }
    }

    override fun togglePlayPause() {
        recovery.clearRecovery()
        val currentTrack = queueController.current.currentTrack
        when {
            playbackState.value.isPlaying -> {
                recovery.onUserPaused()
                meloPlayer.pause()
                persistCurrentSession(immediate = true)
            }

            playbackState.value.isFinished -> {
                meloPlayer.seekTo(0)
                meloPlayer.play()
            }

            playbackState.value.error != null && currentTrack != null -> {
                playCurrentQueueTrack(initialSeekMs = playbackState.value.progressMs)
            }

            isTrackLoaded -> meloPlayer.play()

            currentTrack != null -> {
                playCurrentQueueTrack(initialSeekMs = sessionPersistence.pendingPositionFor(currentTrack.id))
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        recovery.clearRecovery()
        if (!isTrackLoaded) {
            val track = queueController.current.currentTrack
            sessionPersistence.recordPendingSeek(track?.id, positionMs)
            if (track != null) meloPlayer.setIdleTrack(track, positionMs)
        } else {
            meloPlayer.seekTo(positionMs)
        }
        persistCurrentSession(immediate = false)
    }

    override fun setVolume(volume: Float) = audioSettings.setVolume(volume)

    override fun toggleMute() = audioSettings.toggleMute()

    override fun refreshAudioOutputs() = meloPlayer.refreshAudioOutputs()

    override fun selectAudioOutput(deviceId: String?) = audioSettings.selectAudioOutput(deviceId)

    override fun release() {
        recovery.clearRecovery()
        autoplayCoordinator.cancel()
        streamResolver.cancelPrefetch()
        audioSettings.cancel()
        persistCurrentSession(immediate = true)
        sessionPersistence.cancel()
        playJob?.cancel()
        meloPlayer.release()
    }

    override fun setQueue(
        tracks: List<Track>,
        startIndex: Int,
    ) {
        recovery.clearRecovery()
        autoplayCoordinator.cancel()
        streamResolver.cancelPrefetch()
        streamResolver.clearCache()
        sessionPersistence.clearPendingRestore()
        val validIndex = queueController.setQueue(tracks, startIndex)
        if (validIndex >= 0) {
            playCurrentQueueTrack()
        } else {
            parkPlayback()
            persistCurrentSession(immediate = true)
        }
    }

    private fun isQueueEnded(q: QueueState): Boolean =
        !playbackState.value.isPlaying &&
            (playbackState.value.isFinished || lastHandledFinishedTrackId != null) &&
            q.currentIndex >= q.tracks.lastIndex

    override fun addToQueue(track: Track) {
        val shouldAutoStart = queueController.addToQueue(track, isQueueEnded(queueController.current))
        if (shouldAutoStart) {
            sessionPersistence.clearPendingRestore()
            playCurrentQueueTrack()
        } else {
            persistCurrentSession(immediate = false)
        }
    }

    override fun insertToQueue(
        track: Track,
        index: Int,
    ) {
        val shouldAutoStart = queueController.insertToQueue(track, index, isQueueEnded(queueController.current))
        if (shouldAutoStart) {
            sessionPersistence.clearPendingRestore()
            playCurrentQueueTrack()
        } else {
            persistCurrentSession(immediate = false)
        }
    }

    override fun removeFromQueue(index: Int) {
        val wasCurrent = queueController.removeFromQueue(index) ?: return
        if (wasCurrent) {
            sessionPersistence.clearPendingRestore()
            if (queueController.current.tracks.isEmpty()) {
                parkPlayback()
                persistCurrentSession(immediate = true)
            } else {
                playCurrentQueueTrack()
            }
        } else {
            persistCurrentSession(immediate = true)
        }
    }

    override fun moveQueueItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (queueController.moveItem(fromIndex, toIndex)) {
            persistCurrentSession(immediate = false)
        }
    }

    override fun playNext() {
        recovery.clearRecovery()
        sessionPersistence.clearPendingRestore()
        if (!queueController.advanceNext()) {
            triggerAutoplay(forcePlayNext = true)
            return
        }
        playCurrentQueueTrack()
    }

    override fun playPrevious() {
        recovery.clearRecovery()
        if (playbackState.value.progressMs > PREVIOUS_SEEK_THRESHOLD_MS) {
            meloPlayer.seekTo(0)
            return
        }
        if (!queueController.stepPrevious()) return
        sessionPersistence.clearPendingRestore()
        playCurrentQueueTrack()
    }

    override fun toggleShuffle() {
        queueController.toggleShuffle()
        persistCurrentSession(immediate = false)
    }

    override fun toggleRepeat() = queueController.toggleRepeat()

    /**
     * Parks the player with no playable media item (end of queue, unavailable stream, ...).
     *
     * ExoPlayer ignores `play()` while it is idle, so [isTrackLoaded] must be cleared here.
     */
    private fun parkPlayback(
        track: Track? = null,
        positionMs: Long = 0L,
    ) {
        isTrackLoaded = false
        if (track != null) meloPlayer.setIdleTrack(track, positionMs) else meloPlayer.stop()
    }

    private fun playCurrentQueueTrack(initialSeekMs: Long? = null) {
        val track = queueController.current.currentTrack ?: return
        streamResolver.cancelPrefetch()
        val targetSeek =
            initialSeekMs ?: if (!isTrackLoaded) sessionPersistence.pendingPositionFor(track.id) else 0L
        val replayingLoadedTrack =
            isTrackLoaded && playbackState.value.currentTrack?.id == track.id
        if (!replayingLoadedTrack) meloPlayer.stop() else lastHandledFinishedTrackId = null

        isTrackLoaded = true
        recovery.lastHandledErrorTrackId = null
        sessionPersistence.clearPendingRestore()
        playJob?.cancel()
        playJob =
            scope.launch(dispatcher) {
                loadAndStartTrack(track, targetSeek, replayingLoadedTrack)
            }
    }

    private suspend fun loadAndStartTrack(
        track: Track,
        targetSeek: Long,
        replayingLoadedTrack: Boolean,
    ) {
        when {
            isActivelyDownloading(track) -> {
                if (queueController.current.hasNext) playNext() else parkPlayback()
            }

            recovery.isEffectivelyOffline && !recovery.isTrackAvailableOffline(track) -> {
                recovery.handleUnavailableOfflineTrack(track, targetSeek, recoveryCallbacks)
            }

            else -> resolveAndStartTrack(track, targetSeek, replayingLoadedTrack)
        }
    }

    private suspend fun resolveAndStartTrack(
        track: Track,
        targetSeek: Long,
        replayingLoadedTrack: Boolean,
    ) {
        val url =
            try {
                streamResolver.resolve(track)
            } catch (_: AgeRestrictedException) {
                emitPlaybackError("Track is age-restricted (sign-in required), skipping...")
                skipCurrentQueueTrack()
                return
            }

        if (url == null) {
            recovery.handleNullStreamUrl(track, targetSeek, replayingLoadedTrack, recoveryCallbacks)
        } else {
            startResolvedTrack(track, url, targetSeek)
        }
    }

    private fun startResolvedTrack(
        track: Track,
        url: String,
        targetSeek: Long,
    ) {
        if (lastHandledFinishedTrackId == track.id) {
            lastHandledFinishedTrackId = null
        }
        meloPlayer.load(url, track, targetSeek)
        emitPlaybackEvent(PlaybackEvent.TrackStarted(track, positionMs = targetSeek))
        persistCurrentSession(immediate = true)
        recordPlayAsync(track)
        streamResolver.scheduleAutoCache(track, url)
        streamResolver.resolveSourceIdAsync(track) { sid ->
            queueController.update { state ->
                val updated = state.tracks.map { if (it.id == track.id) it.copy(sourceId = sid) else it }
                state.copy(tracks = updated)
            }
        }
        schedulePrefetch()
    }

    private fun isActivelyDownloading(track: Track): Boolean =
        downloadManager?.activeDownloads?.value?.let { activeMap ->
            activeMap.isNotEmpty() &&
                (activeMap.containsKey(track.id) || activeMap.containsKey(track.id.removePrefix("piped:")))
        } == true

    @Suppress("TooGenericExceptionCaught")
    private fun recordPlayAsync(track: Track) {
        scope.launch(dispatcher) {
            try {
                recordPlayUseCase?.invoke(track)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort history recording.
            }
        }
    }

    private fun schedulePrefetch() {
        streamResolver.cancelPrefetch()
        if (recovery.isEffectivelyOffline) return
        val q = queueController.current
        if (autoplayCoordinator.shouldPrefetchRadio(q)) {
            triggerAutoplay(forcePlayNext = false)
        }
        streamResolver.schedulePrefetch(q.tracks, q.currentIndex)
    }

    private suspend fun skipCurrentQueueTrack() {
        if (recovery.isEffectivelyOffline) {
            val nextOfflineIndex = recovery.findNextOfflineTrackIndex(queueController.current.currentIndex)
            if (nextOfflineIndex != null) {
                queueController.update { it.copy(currentIndex = nextOfflineIndex, userQueueCount = 0) }
                playCurrentQueueTrack()
            } else {
                parkPlayback()
            }
            return
        }
        if (queueController.current.hasNext) playNext() else triggerAutoplay(forcePlayNext = true)
    }

    private fun triggerAutoplay(forcePlayNext: Boolean) {
        autoplayCoordinator.trigger(
            forcePlayNext = forcePlayNext,
            onPlayNext = { playCurrentQueueTrack() },
            onPark = { parkPlayback() },
        )
    }

    private fun emitPlaybackEvent(event: PlaybackEvent) {
        _events.tryEmit(event)
    }

    private fun emitPlaybackError(message: String) {
        emitPlaybackEvent(PlaybackEvent.Error(message))
    }

    private fun handleTrackFinished() {
        if (queueController.current.hasNext) playNext() else triggerAutoplay(forcePlayNext = true)
    }

    private fun persistCurrentSession(immediate: Boolean = false) {
        sessionPersistence.persist(
            queue = queueController.current,
            progressMs = playbackState.value.progressMs,
            isTrackLoaded = isTrackLoaded,
            immediate = immediate,
        )
    }

    private companion object {
        private const val PREVIOUS_SEEK_THRESHOLD_MS = 3000L
    }
}
