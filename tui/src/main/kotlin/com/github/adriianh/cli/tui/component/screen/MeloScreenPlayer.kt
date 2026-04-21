package com.github.adriianh.cli.tui.component.screen

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.handler.onTrackProgress
import com.github.adriianh.cli.tui.handler.playback.seekBackward
import com.github.adriianh.cli.tui.handler.playback.seekForward
import com.github.adriianh.cli.tui.handler.playback.togglePlayPause

internal fun MeloScreen.handleMediaSessionPlayPause() {
    appRunner()?.runOnRenderThread { togglePlayPause() }
}

internal fun MeloScreen.handleMediaSessionNext() {
    appRunner()?.runOnRenderThread { seekForward() }
}

internal fun MeloScreen.handleMediaSessionPrevious() {
    appRunner()?.runOnRenderThread { seekBackward() }
}

internal fun MeloScreen.handleMediaSessionStop() {
    appRunner()?.runOnRenderThread {
        audioPlayer.stop()
        state = state.copy(player = state.player.copy(isPlaying = false, progress = 0.0))
    }
}

internal fun MeloScreen.handleAudioProgress(elapsedMs: Long) {
    appRunner()?.runOnRenderThread {
        val duration = state.player.nowPlaying?.durationMs ?: 0L
        val progress = if (duration > 0) (elapsedMs.toDouble() / duration).coerceIn(0.0, 1.0) else 0.0
        state = state.copy(player = state.player.copy(nowPlayingPositionMs = elapsedMs, progress = progress))
        mediaSession.updatePosition(elapsedMs)
        state.player.nowPlaying?.let { onTrackProgress(it, elapsedMs, trackStartedAt) }
    }
}

internal fun MeloScreen.handleAudioFinish() {
    appRunner()?.runOnRenderThread {
        state = state.copy(player = state.player.copy(isPlaying = false, isLoadingAudio = false, progress = 0.0))
        seekForward()
    }
}

internal fun MeloScreen.handleAudioError(err: Throwable) {
    appRunner()?.runOnRenderThread {
        state = state.copy(
            player = state.player.copy(
                isPlaying = false,
                isLoadingAudio = false,
                audioError = err.message
            )
        )
        mediaSession.notifyStopped()
    }
}