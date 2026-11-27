package com.github.adriianh.data.player

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.QueueState
import com.github.adriianh.core.domain.repository.SavedSession
import com.github.adriianh.core.domain.usecase.session.ClearSessionUseCase
import com.github.adriianh.core.domain.usecase.session.RestoreSessionUseCase
import com.github.adriianh.core.domain.usecase.session.SaveSessionUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Handles restoring the saved playback session on startup and persisting
 * queue + position updates with debouncing.
 */
internal class SessionPersistenceCoordinator(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val saveSessionUseCase: SaveSessionUseCase?,
    private val restoreSessionUseCase: RestoreSessionUseCase?,
    private val clearSessionUseCase: ClearSessionUseCase?,
    private val getSettingsUseCase: GetSettingsUseCase?,
) {
    private var saveSessionJob: Job? = null
    private var pendingRestorePositionMs: Long = 0L
    private var pendingRestoreTrackId: String? = null
    private var lastSavedPositionMs: Long = 0L

    fun restore(onRestored: (tracks: List<Track>, index: Int, track: Track, positionMs: Long) -> Unit) {
        scope.launch(dispatcher) {
            val resumeMostRecent = getSettingsUseCase?.getInitial()?.resumeLastActiveSession == true
            val session = restoreSessionUseCase?.invoke(resumeMostRecent)
            if (session != null && session.queue.isNotEmpty()) {
                val validIndex = session.queueIndex.coerceIn(0, session.queue.lastIndex)
                val track = session.queue.getOrNull(validIndex)
                if (track != null) {
                    pendingRestorePositionMs = session.positionMs
                    pendingRestoreTrackId = track.id
                    lastSavedPositionMs = session.positionMs
                    onRestored(session.queue, validIndex, track, session.positionMs)
                }
            }
        }
    }

    fun persist(
        queue: QueueState,
        progressMs: Long,
        isTrackLoaded: Boolean,
        immediate: Boolean = false,
    ) {
        if (saveSessionUseCase == null) return
        saveSessionJob?.cancel()
        val action =
            suspend {
                if (queue.tracks.isEmpty() || queue.currentIndex < 0) {
                    clearSessionUseCase?.invoke()
                } else {
                    val isPendingRestoredTrack =
                        !isTrackLoaded && pendingRestoreTrackId == queue.currentTrack?.id
                    val currentPos =
                        if (isPendingRestoredTrack && pendingRestorePositionMs > 0) {
                            pendingRestorePositionMs
                        } else {
                            progressMs
                        }
                    lastSavedPositionMs = currentPos
                    saveSessionUseCase(
                        SavedSession(
                            queue = queue.tracks,
                            queueIndex = queue.currentIndex,
                            positionMs = currentPos,
                        ),
                    )
                }
            }
        if (immediate) {
            scope.launch(dispatcher) { action() }
        } else {
            saveSessionJob =
                scope.launch(dispatcher) {
                    delay(PERSIST_DEBOUNCE_MS.milliseconds)
                    action()
                }
        }
    }

    fun pendingPositionFor(trackId: String): Long =
        if (pendingRestoreTrackId == trackId && pendingRestorePositionMs > 0) {
            pendingRestorePositionMs
        } else {
            0L
        }

    fun recordPendingSeek(
        trackId: String?,
        positionMs: Long,
    ) {
        pendingRestorePositionMs = positionMs
        pendingRestoreTrackId = trackId
        lastSavedPositionMs = positionMs
    }

    fun clearPendingRestore() {
        pendingRestorePositionMs = 0L
        pendingRestoreTrackId = null
    }

    fun hasProgressDrift(progressMs: Long): Boolean = abs(progressMs - lastSavedPositionMs) >= PROGRESS_SAVE_STEP_MS

    fun cancel() {
        saveSessionJob?.cancel()
        saveSessionJob = null
    }

    private companion object {
        private const val PERSIST_DEBOUNCE_MS = 1000L
        private const val PROGRESS_SAVE_STEP_MS = 5000L
    }
}
