package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * The shared PlaybackManager restores the saved queue/session during its init.
 * Here we only wait for that restore to land and kick off playback at the
 * saved position (the PM applies the restored position seek automatically).
 */
internal suspend fun MeloScreen.restoreLastSession() {
    var waited = 0
    while (waited < 10_000 && playbackManager.queueState.value.tracks.isEmpty()) {
        delay(250.milliseconds)
        waited += 250
    }
    if (playbackManager.queueState.value.tracks.isEmpty()) return

    appRunner()?.runOnRenderThread { state = state.copy(isRestoringSession = true) }

    val playback = playbackManager.playbackState.value
    if (playbackManager.queueState.value.currentTrack != null &&
        !playback.isPlaying && !playback.isBuffering
    ) {
        playbackManager.togglePlayPause()
    }

    waited = 0
    while (waited < 10_000) {
        delay(250.milliseconds)
        waited += 250
        val current = playbackManager.playbackState.value
        if (current.isPlaying || current.isBuffering) break
        if (current.error != null) break
    }

    appRunner()?.runOnRenderThread { state = state.copy(isRestoringSession = false) }
}