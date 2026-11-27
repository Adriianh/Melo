package com.github.adriianh.data.player

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.network.NetworkMonitor
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.repository.OfflineRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal data class RecoveryEnvironment(
    val offlineRepository: OfflineRepository?,
    val networkMonitor: NetworkMonitor?,
    val isOfflineSettingsEnabled: () -> Boolean,
)

internal data class RecoveryCallbacks(
    val onReplayAt: (positionMs: Long) -> Unit,
    val onJumpToIndex: (index: Int) -> Unit,
    val onPark: (track: Track?, positionMs: Long) -> Unit,
    val onSkip: suspend () -> Unit,
    val onError: (String) -> Unit,
)

/**
 * Handles exponential-backoff stream recovery, network disconnect/reconnect
 * resumption, and offline track availability fallback.
 */
internal class PlaybackRecoveryCoordinator(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val queueController: QueueController,
    private val meloPlayer: MeloPlayer,
    private val environment: RecoveryEnvironment,
) {
    private var recoveryAttempts = 0
    private var recoveryJob: Job? = null
    internal var lastHandledErrorTrackId: String? = null
    private var wasOfflineWaitingForNetwork = false
    private var pendingResumePositionMs = 0L
    private var pendingResumeTrackId: String? = null

    val isEffectivelyOffline: Boolean
        get() = environment.isOfflineSettingsEnabled() || (environment.networkMonitor?.isOnline?.value == false)

    fun observeNetwork(callbacks: RecoveryCallbacks) {
        val monitor = environment.networkMonitor ?: return
        scope.launch(dispatcher) {
            var wasOnline = monitor.isOnline.value
            monitor.isOnline.collect { isOnline ->
                val reconnected = !wasOnline && isOnline
                wasOnline = isOnline
                if (!isOnline) {
                    val currentTrack = queueController.current.currentTrack
                    if (currentTrack != null && !isTrackAvailableOffline(currentTrack)) {
                        wasOfflineWaitingForNetwork = true
                        if (pendingResumeTrackId != currentTrack.id) {
                            pendingResumeTrackId = currentTrack.id
                            pendingResumePositionMs = meloPlayer.state.value.progressMs
                        }
                    }
                } else if (reconnected && wasOfflineWaitingForNetwork && !environment.isOfflineSettingsEnabled()) {
                    resumeAfterReconnect(callbacks)
                }
            }
        }
    }

    fun onOfflineModeToggledOnline(callbacks: RecoveryCallbacks) {
        if (wasOfflineWaitingForNetwork && environment.networkMonitor?.isOnline?.value != false) {
            resumeAfterReconnect(callbacks)
        }
    }

    fun onUserPaused() {
        wasOfflineWaitingForNetwork = false
        pendingResumeTrackId = null
        pendingResumePositionMs = 0L
    }

    fun clearRecovery(clearLastError: Boolean = false) {
        recoveryAttempts = 0
        recoveryJob?.cancel()
        recoveryJob = null
        if (clearLastError) {
            lastHandledErrorTrackId = null
        }
    }

    suspend fun isTrackAvailableOffline(track: Track): Boolean {
        val offlineTrack =
            if (track.id.startsWith("local:")) {
                null
            } else {
                environment.offlineRepository?.getOfflineTrack(track.id)
            }
        return track.id.startsWith("local:") || offlineTrack?.downloadStatus == DownloadStatus.COMPLETED
    }

    suspend fun findNextOfflineTrackIndex(startIndex: Int): Int? {
        val tracks = queueController.current.tracks
        for (i in (startIndex + 1 until tracks.size)) {
            if (isTrackAvailableOffline(tracks[i])) return i
        }
        return null
    }

    fun handlePlayerError(
        trackId: String?,
        progressMs: Long,
        loadError: String,
        callbacks: RecoveryCallbacks,
    ) {
        val isOffline = isEffectivelyOffline || environment.networkMonitor?.isOnline?.value == false
        when {
            isOffline -> {
                wasOfflineWaitingForNetwork = true
                pendingResumePositionMs = progressMs
                pendingResumeTrackId = trackId
                callbacks.onError(loadError)
            }

            trackId != null &&
                !trackId.startsWith("local:") &&
                recoveryAttempts < MAX_PLAYBACK_RECOVERY_ATTEMPTS -> {
                scheduleRecovery(trackId, progressMs, callbacks)
            }

            trackId == null || trackId != lastHandledErrorTrackId -> {
                lastHandledErrorTrackId = trackId
                callbacks.onError(loadError)
            }
        }
    }

    suspend fun handleUnavailableOfflineTrack(
        track: Track,
        targetSeek: Long,
        callbacks: RecoveryCallbacks,
    ) {
        val nextOfflineIndex = findNextOfflineTrackIndex(queueController.current.currentIndex)
        if (nextOfflineIndex != null) {
            callbacks.onJumpToIndex(nextOfflineIndex)
        } else {
            callbacks.onPark(track, targetSeek)
            wasOfflineWaitingForNetwork = true
            pendingResumePositionMs = targetSeek
            pendingResumeTrackId = track.id
            callbacks.onError("Track is not available offline")
        }
    }

    suspend fun handleNullStreamUrl(
        track: Track,
        targetSeek: Long,
        replayingLoadedTrack: Boolean,
        callbacks: RecoveryCallbacks,
    ) {
        when {
            isEffectivelyOffline || environment.networkMonitor?.isOnline?.value == false -> {
                callbacks.onPark(track, targetSeek)
                wasOfflineWaitingForNetwork = true
                pendingResumePositionMs = targetSeek
                pendingResumeTrackId = track.id
                callbacks.onError("No internet connection")
            }

            replayingLoadedTrack && recoveryAttempts < MAX_PLAYBACK_RECOVERY_ATTEMPTS -> {
                scheduleRecovery(track.id, targetSeek, callbacks)
            }

            replayingLoadedTrack -> {
                callbacks.onPark(track, targetSeek)
                wasOfflineWaitingForNetwork = true
                pendingResumePositionMs = targetSeek
                pendingResumeTrackId = track.id
                callbacks.onError("Network connection lost. Press play to retry.")
            }

            else -> {
                callbacks.onPark(track, targetSeek)
                callbacks.onError("Stream not available, skipping...")
                callbacks.onSkip()
            }
        }
    }

    private fun resumeAfterReconnect(callbacks: RecoveryCallbacks) {
        wasOfflineWaitingForNetwork = false
        val currentTrack = queueController.current.currentTrack
        if (currentTrack != null && !meloPlayer.state.value.isPlaying) {
            val resumePos =
                if (pendingResumeTrackId == currentTrack.id) pendingResumePositionMs else 0L
            clearRecovery()
            callbacks.onReplayAt(resumePos)
        }
    }

    private fun scheduleRecovery(
        trackId: String,
        resumePos: Long,
        callbacks: RecoveryCallbacks,
    ) {
        recoveryAttempts++
        recoveryJob?.cancel()
        recoveryJob =
            scope.launch(dispatcher) {
                delay((BASE_RECOVERY_DELAY_MS * recoveryAttempts).milliseconds)
                if (queueController.current.currentTrack?.id == trackId && !meloPlayer.state.value.isPlaying) {
                    callbacks.onReplayAt(resumePos)
                }
            }
    }

    private companion object {
        private const val MAX_PLAYBACK_RECOVERY_ATTEMPTS = 4
        private const val BASE_RECOVERY_DELAY_MS = 2000L
    }
}
