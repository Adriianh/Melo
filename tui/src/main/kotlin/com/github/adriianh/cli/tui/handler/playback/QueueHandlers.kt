package com.github.adriianh.cli.tui.handler.playback

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.handler.handleGlobalShortcuts
import com.github.adriianh.cli.tui.handler.matchesAction
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

internal fun MeloScreen.addToQueue(track: Track) {
    if (!state.isPlayable(track)) return
    playbackManager.addToQueue(track)
}

internal fun MeloScreen.removeFromQueue(index: Int) {
    if (index < 0 || index >= playbackManager.queueState.value.tracks.size) return
    playbackManager.removeFromQueue(index)
}

internal fun MeloScreen.clearQueue() {
    state = state.copy(player = state.player.copy(isRadioMode = false))
    playbackManager.setQueue(emptyList())
}

internal fun MeloScreen.toggleQueue() {
    val nowVisible = !state.player.isQueueVisible
    state = state.copy(player = state.player.copy(isQueueVisible = nowVisible))
    if (nowVisible) appRunner()?.focusManager()?.setFocus("queue-panel")
}

internal fun MeloScreen.handleQueueKey(event: KeyEvent): EventResult {
    val isFocused = appRunner()?.focusManager()?.focusedId() == "queue-panel"
    when {
        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(player = state.player.copy(isQueueVisible = false))
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) -> {
            if (!isFocused) return handleGlobalShortcuts(event)
            val newCursor = minOf(state.player.queue.lastIndex, state.player.queueCursor + 1)
            state = state.copy(player = state.player.copy(queueCursor = newCursor))

            if (state.player.isRadioMode && !state.player.isLoadingMoreRadio && newCursor >= state.player.queue.size - 5) {
                loadMoreRadioTracks()
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            if (!isFocused) return handleGlobalShortcuts(event)
            state = state.copy(
                player = state.player.copy(
                    queueCursor = maxOf(
                        0,
                        state.player.queueCursor - 1
                    )
                )
            )
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            if (!isFocused) return handleGlobalShortcuts(event)
            if (state.player.queue.getOrNull(state.player.queueCursor) != null) playFromQueue(state.player.queueCursor)
            return EventResult.HANDLED
        }

        event.matchesAction(
            MeloAction.DELETE,
            settingsViewState.currentSettings
        ) || event.isCharIgnoreCase('d') -> {
            if (!isFocused) return handleGlobalShortcuts(event)
            removeFromQueue(state.player.queueCursor)
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.CLEAR_QUEUE, settingsViewState.currentSettings) -> {
            clearQueue()
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}