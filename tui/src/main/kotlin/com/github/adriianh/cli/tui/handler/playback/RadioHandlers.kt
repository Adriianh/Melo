package com.github.adriianh.cli.tui.handler.playback

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.handler.resolveSimilarTracks
import kotlinx.coroutines.launch

internal fun MeloScreen.loadSimilarAndPlay() {
    if (state.isOfflineMode) return
    val seed = state.player.nowPlaying ?: return
    val alreadyPlayed = playbackManager.queueState.value.tracks.map { it.id }.toSet()
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
                    state = state.copy(
                        player = state.player.copy(
                            isLoadingAudio = false,
                            isRadioMode = false
                        )
                    )
                }
                return@launch
            }
            appRunner()?.runOnRenderThread {
                state = state.copy(
                    player = state.player.copy(
                        queueCursor = 0,
                        isLoadingAudio = false,
                        isRadioMode = true
                    )
                )
            }
            playbackManager.setQueue(related, 0)
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

/**
 * Radio continuation is now handled by the shared PlaybackManager (near-end
 * autoplay prefetch), so this is a no-op placeholder until Fase 3 cleanup.
 */
internal fun loadMoreRadioTracks() {
    // Intentionally empty: the PM prefetches radio tracks automatically.
}