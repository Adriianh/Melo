package com.github.adriianh.cli.tui.handler.playback

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.player.RepeatMode

internal fun MeloScreen.toggleShuffle() {
    playbackManager.toggleShuffle()
}

internal fun MeloScreen.cycleRepeat() {
    playbackManager.toggleRepeat()
}

/** Sets shuffle to an explicit state (command bar / settings), preserving PM as source of truth. */
internal fun MeloScreen.setShuffleEnabled(enabled: Boolean) {
    if (playbackManager.queueState.value.shuffleEnabled != enabled) {
        playbackManager.toggleShuffle()
    }
}

/** Cycles repeat until it matches [mode] (command bar / settings). */
internal fun MeloScreen.setRepeatMode(mode: RepeatMode) {
    repeat(3) {
        if (playbackManager.queueState.value.repeatMode == mode) return
        playbackManager.toggleRepeat()
    }
}

/** Sets an explicit volume percentage through the PM (persists to settings). */
internal fun MeloScreen.setVolumePercent(vol: Int) {
    playbackManager.setVolume(vol.coerceIn(0, 100) / 100f)
}