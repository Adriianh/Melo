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

    val currentQueue = state.player.queue.toMutableList()
    val newManualCount = state.player.userQueueCount + 1

    val insertIndex = if (state.player.queueIndex < 0) 0 else minOf(
        state.player.queueIndex + newManualCount,
        currentQueue.size
    )
    currentQueue.add(insertIndex, track)

    val newIndex =
        if (state.player.queueIndex < 0 && state.player.nowPlaying == null) 0 else state.player.queueIndex
    val newRadioMode = state.player.isRadioMode && state.player.nowPlaying != null
    state = state.copy(
        player = state.player.copy(
            queue = currentQueue,
            queueIndex = newIndex,
            isRadioMode = newRadioMode,
            userQueueCount = newManualCount
        )
    )
    if (state.player.nowPlaying == null && !state.player.isLoadingAudio) playFromQueue(0)
}

internal fun MeloScreen.removeFromQueue(index: Int) {
    if (index < 0 || index >= state.player.queue.size) return
    val removingPlaying = index == state.player.queueIndex
    val newQueue = state.player.queue.toMutableList().also { it.removeAt(index) }
    val newIndex = when {
        newQueue.isEmpty() -> -1
        index < state.player.queueIndex -> state.player.queueIndex - 1
        index == state.player.queueIndex -> minOf(index, newQueue.lastIndex)
        else -> state.player.queueIndex
    }

    val manualStart = state.player.queueIndex + 1
    val manualEnd = state.player.queueIndex + state.player.userQueueCount
    val newUserQueueCount =
        if (index in manualStart..manualEnd && state.player.userQueueCount > 0) {
            state.player.userQueueCount - 1
        } else {
            state.player.userQueueCount
        }

    state = state.copy(
        player = state.player.copy(
            queue = newQueue,
            queueIndex = newIndex,
            queueCursor = minOf(state.player.queueCursor, (newQueue.size - 1).coerceAtLeast(0)),
            userQueueCount = newUserQueueCount
        )
    )
    if (removingPlaying) {
        audioPlayer.stop()
        if (newQueue.isEmpty()) state = state.copy(
            player = state.player.copy(
                nowPlaying = null,
                isPlaying = false,
                isRadioMode = false,
                progress = 0.0,
                userQueueCount = 0
            )
        )
        else playFromQueue(if (newIndex >= 0 && newIndex < newQueue.size) newIndex else 0)
    }
}

internal fun MeloScreen.clearQueue() {
    audioPlayer.stop()
    state = state.copy(
        player = state.player.copy(
            queue = emptyList(), queueIndex = -1, queueCursor = 0,
            nowPlaying = null, isPlaying = false, isRadioMode = false,
            progress = 0.0, userQueueCount = 0
        ),
    )
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
        ) || (event.code() == KeyCode.CHAR && event.character() == 'd') -> {
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