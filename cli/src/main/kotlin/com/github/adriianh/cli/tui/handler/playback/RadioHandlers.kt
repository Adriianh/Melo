package com.github.adriianh.cli.tui.handler.playback

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.handler.resolveSimilarTracks
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Max tracks to keep in the radio queue; older played tracks are evicted. */
private const val MAX_QUEUE_SIZE = 50

internal fun MeloScreen.loadSimilarAndPlay() {
    if (state.isOfflineMode) return
    val seed = state.player.nowPlaying ?: return
    val alreadyPlayed = state.player.queue.map { it.id }.toSet()
    state = state.copy(
        player = state.player.copy(
            isPlaying = false,
            isLoadingAudio = true,
            isRadioMode = true,
            progress = 0.0
        )
    )
    scope.launch {
        try {
            val related = resolveSimilarTracks(seed, limit = 15)
                .filter { it.id !in alreadyPlayed }
                .distinctBy { it.id }
                .shuffled()
                .take(10)

            if (related.isEmpty()) {
                appRunner()?.runOnRenderThread {
                    state = state.copy(player = state.player.copy(isLoadingAudio = false, isRadioMode = false))
                }
                return@launch
            }
            appRunner()?.runOnRenderThread {
                state = state.copy(
                    player = state.player.copy(
                        queue = related,
                        queueIndex = -1,
                        queueCursor = 0,
                        isLoadingAudio = false,
                        isRadioMode = true
                    )
                )
                playFromQueue(0)
            }
        } catch (e: Exception) {
            appRunner()?.runOnRenderThread {
                state = state.copy(
                    player = state.player.copy(
                        isPlaying = false,
                        isLoadingAudio = false,
                        audioError = e.message,
                        isRadioMode = false
                    )
                )
            }
        }
    }
}

internal fun MeloScreen.loadMoreRadioTracks() {
    if (state.isOfflineMode) return
    if (!state.player.isRadioMode || state.player.isLoadingMoreRadio || state.player.queue.isEmpty()) return

    val seed = state.player.queue.last()
    val alreadyPlayed = state.player.queue.map { it.id }.toSet()
    state = state.copy(player = state.player.copy(isLoadingMoreRadio = true))

    scope.launch {
        try {
            val related = resolveSimilarTracks(seed, limit = 10, offset = 0)
                .filter { it.id !in alreadyPlayed }
                .distinctBy { it.id }
                .shuffled()
                .take(10)

            if (isActive) appRunner()?.runOnRenderThread {
                if (related.isNotEmpty()) {
                    val combined = state.player.queue + related
                    val (pruned, newIdx) = pruneQueue(combined, state.player.queueIndex)
                    state = state.copy(
                        player = state.player.copy(
                            queue = pruned,
                            queueIndex = newIdx,
                            queueCursor = minOf(state.player.queueCursor, (pruned.size - 1).coerceAtLeast(0)),
                            isLoadingMoreRadio = false
                        )
                    )
                } else {
                    state = state.copy(player = state.player.copy(isLoadingMoreRadio = false))
                }
            }
        } catch (_: Exception) {
            if (isActive) appRunner()?.runOnRenderThread {
                state = state.copy(player = state.player.copy(isLoadingMoreRadio = false))
            }
        }
    }
}

/**
 * Prunes already-played tracks from the front of the queue when it exceeds [MAX_QUEUE_SIZE].
 * Returns the pruned list and the adjusted queueIndex.
 */
private fun pruneQueue(
    queue: List<Track>,
    currentIndex: Int
): Pair<List<Track>, Int> {
    if (queue.size <= MAX_QUEUE_SIZE || currentIndex <= 0) return queue to currentIndex
    // Keep a small buffer of ~5 played tracks for "previous" navigation
    val dropCount = (currentIndex - 5).coerceAtLeast(0)
    if (dropCount == 0) return queue to currentIndex
    return queue.drop(dropCount) to (currentIndex - dropCount)
}