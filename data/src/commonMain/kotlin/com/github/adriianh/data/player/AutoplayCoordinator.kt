package com.github.adriianh.data.player

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.QueueState
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.core.domain.usecase.offline.GetOfflineSuggestionsUseCase
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

internal data class AutoplayProviders(
    val getStreamUseCase: GetStreamUseCase,
    val getRadioUseCase: GetRadioUseCase?,
    val offlineSuggestionsUseCase: GetOfflineSuggestionsUseCase?,
    val getSettingsUseCase: GetSettingsUseCase?,
)

/**
 * Coordinates radio and offline autoplay continuations.
 *
 * Continuations are always appended to the end of the queue so they never
 * cut in front of remaining playlist/album tracks or manually queued tracks.
 */
internal class AutoplayCoordinator(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val queueController: QueueController,
    private val providers: AutoplayProviders,
    private val isEffectivelyOffline: () -> Boolean,
) {
    private var autoplayJob: Job? = null
    private var isAutoplayFetching = false

    fun shouldPrefetchRadio(q: QueueState): Boolean =
        q.repeatMode == RepeatMode.NONE &&
            hasProvider(isEffectivelyOffline()) &&
            q.currentIndex >= 0 &&
            q.tracks.isNotEmpty() &&
            q.currentIndex >= q.tracks.lastIndex

    fun trigger(
        forcePlayNext: Boolean,
        onPlayNext: () -> Unit,
        onPark: () -> Unit,
    ) {
        val currentTrack = queueController.current.currentTrack ?: return
        val hasAutoplayProvider = hasProvider(isEffectivelyOffline())

        if (isAutoplayFetching || !hasAutoplayProvider) {
            if (forcePlayNext) onPark()
            return
        }

        isAutoplayFetching = true
        autoplayJob =
            scope.launch(dispatcher) {
                try {
                    val autoplayEnabled =
                        providers.getSettingsUseCase
                            ?.invoke()
                            ?.firstOrNull()
                            ?.autoplay ?: true
                    if (!autoplayEnabled) {
                        if (forcePlayNext) onPark()
                        return@launch
                    }

                    val tracksToAdd =
                        if (isEffectivelyOffline()) {
                            fetchOfflineSuggestions(currentTrack)
                        } else {
                            fetchOnlineRadioTracks(currentTrack)
                        }

                    applyAutoplayTracks(tracksToAdd, forcePlayNext, onPlayNext, onPark)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    if (forcePlayNext) onPark()
                } finally {
                    isAutoplayFetching = false
                }
            }
    }

    fun cancel() {
        autoplayJob?.cancel()
        autoplayJob = null
        isAutoplayFetching = false
    }

    private fun hasProvider(offline: Boolean): Boolean =
        if (offline) {
            providers.offlineSuggestionsUseCase != null
        } else {
            providers.getRadioUseCase != null
        }

    private suspend fun fetchOfflineSuggestions(currentTrack: Track): List<Track> {
        val queueIds =
            queueController.current.tracks
                .map { it.id }
                .toSet()
        return providers.offlineSuggestionsUseCase?.invoke(currentTrack, queueIds, OFFLINE_SUGGESTION_LIMIT)
            ?: emptyList()
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchOnlineRadioTracks(currentTrack: Track): List<Track> {
        val radioUseCase = providers.getRadioUseCase ?: return emptyList()
        val resolvedSourceId =
            try {
                providers.getStreamUseCase.resolveSourceId(currentTrack)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }

        if (resolvedSourceId != null && currentTrack.sourceId == null) {
            queueController.update { state ->
                val updatedTracks =
                    state.tracks.map {
                        if (it.id == currentTrack.id) it.copy(sourceId = resolvedSourceId) else it
                    }
                state.copy(tracks = updatedTracks)
            }
        }

        val videoId =
            resolvedSourceId
                ?: currentTrack.sourceId
                ?: currentTrack.id.takeIf { it.startsWith("piped:") }?.removePrefix("piped:")
                ?: currentTrack.id

        return radioUseCase(videoId).filter { rec ->
            queueController.current.tracks.none { existing ->
                existing.id == rec.id ||
                    (!existing.sourceId.isNullOrBlank() && existing.sourceId == rec.sourceId) ||
                    (
                        existing.title.equals(rec.title, ignoreCase = true) &&
                            existing.artist.equals(rec.artist, ignoreCase = true)
                    )
            }
        }
    }

    private fun applyAutoplayTracks(
        tracksToAdd: List<Track>,
        forcePlayNext: Boolean,
        onPlayNext: () -> Unit,
        onPark: () -> Unit,
    ) {
        if (tracksToAdd.isEmpty()) {
            if (forcePlayNext) onPark()
            return
        }

        queueController.update { state ->
            state.copy(tracks = state.tracks + tracksToAdd)
        }
        if (forcePlayNext) {
            val nextIdx = queueController.current.currentIndex + 1
            if (nextIdx in queueController.current.tracks.indices) {
                queueController.update { state ->
                    val newCount = (state.userQueueCount - 1).coerceAtLeast(0)
                    state.copy(currentIndex = nextIdx, userQueueCount = newCount)
                }
                onPlayNext()
            }
        }
    }

    private companion object {
        private const val OFFLINE_SUGGESTION_LIMIT = 10
    }
}
