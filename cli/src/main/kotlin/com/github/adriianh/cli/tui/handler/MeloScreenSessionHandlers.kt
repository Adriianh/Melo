package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.repository.SavedSession

internal suspend fun MeloScreen.restoreLastSession() {
    val session = restoreSession() ?: return
    appRunner()?.runOnRenderThread {
        state = state.copy(
            queue      = session.queue,
            queueIndex = session.queueIndex,
            isRestoringSession = true,
        )
        playFromQueue(session.queueIndex)
    }

    var waited = 0
    while (waited < 8000) {
        kotlinx.coroutines.delay(200)
        waited += 200
        val s = state
        if (!s.isLoadingAudio && s.isPlaying) {
            if (session.positionMs > 3000L) {
                appRunner()?.runOnRenderThread { seekTo(session.positionMs.toDouble() / (s.nowPlaying?.durationMs ?: 1L)) }
            }
            break
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
            queue      = s.queue,
            queueIndex = s.queueIndex,
            positionMs = (s.progress * (s.nowPlaying.durationMs)).toLong(),
        )
    )
}

