package com.github.adriianh.cli.tui.handler.playback

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.RepeatMode

internal fun MeloScreen.toggleShuffle() {
    state = state.copy(player = state.player.copy(shuffleEnabled = !state.player.shuffleEnabled))
}

internal fun MeloScreen.cycleRepeat() {
    state = state.copy(
        player = state.player.copy(
            repeatMode = when (state.player.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
        )
    )
}