package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.repository.SavedSession
import kotlinx.coroutines.delay

internal suspend fun MeloScreen.restoreLastSession() {
    val session = restoreSession() ?: return

    appRunner()?.runOnRenderThread {
        state = state.copy(
            queue = session.queue,
            queueIndex = session.queueIndex,
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
            !s.isLoadingAudio && s.isPlaying -> {
                if (session.positionMs > 3_000L) {
                    val duration = s.nowPlaying?.durationMs?.takeIf { it > 0 } ?: break
                    appRunner()?.runOnRenderThread {
                        seekTo(session.positionMs.toDouble() / duration)
                    }
                }
                break
            }
            s.audioError != null -> break
        }
    }

    appRunner()?.runOnRenderThread { state = state.copy(isRestoringSession = false) }
}

internal suspend fun MeloScreen.persistSession() {
    val s = state
    if (s.nowPlaying == null || s.queue.isEmpty() || s.queueIndex < 0) {
        clearSession()
        return
    }
    saveSession(
        SavedSession(
            queue = s.queue,
            queueIndex = s.queueIndex,
            positionMs = (s.progress * s.nowPlaying.durationMs).toLong(),
        )
    )
}
