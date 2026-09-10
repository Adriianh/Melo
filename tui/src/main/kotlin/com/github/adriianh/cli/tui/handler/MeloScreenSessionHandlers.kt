package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.handler.playback.playFromQueue
import com.github.adriianh.cli.tui.handler.playback.seekTo
import com.github.adriianh.core.domain.repository.SavedSession
import kotlinx.coroutines.delay

internal suspend fun MeloScreen.restoreLastSession() {
    val session = restoreSession() ?: return

    appRunner()?.runOnRenderThread {
        state = state.copy(
            player = state.player.copy(
                queue = session.queue,
                queueIndex = session.queueIndex,
            ),
            isRestoringSession = true,
        )
        playFromQueue(session.queueIndex)
    }

    // Give the render thread a moment to process before polling
    delay(300)

    var waited = 0
    while (waited < 10_000) {
        delay(250)
        waited += 250
        val s = state
        when {
            !s.player.isLoadingAudio && s.player.isPlaying -> {
                if (session.positionMs > 3_000L) {
                    val duration = s.player.nowPlaying?.durationMs?.takeIf { it > 0 } ?: break
                    appRunner()?.runOnRenderThread {
                        seekTo(session.positionMs.toDouble() / duration)
                    }
                }
                break
            }
            s.player.audioError != null -> break
        }
    }

    appRunner()?.runOnRenderThread { state = state.copy(isRestoringSession = false) }
}

internal suspend fun MeloScreen.persistSession() {
    val currentState = state
    if (currentState.player.nowPlaying == null || currentState.player.queue.isEmpty() || currentState.player.queueIndex < 0) {
        clearSession()
        return
    }
    saveSession(
        SavedSession(
            queue = currentState.player.queue,
            queueIndex = currentState.player.queueIndex,
            positionMs = (currentState.player.progress * currentState.player.nowPlaying.durationMs).toLong(),
        )
    )
}
