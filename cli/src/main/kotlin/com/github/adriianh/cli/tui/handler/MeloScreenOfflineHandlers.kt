package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.OfflineFilterType
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playList
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

/**
 * Handles key events for the Offline (Downloads) screen.
 */
internal fun MeloScreen.handleOfflineKey(event: KeyEvent): EventResult {
    val actualState = state.screen as? ScreenState.Offline ?: return EventResult.UNHANDLED

    if (actualState.isTyping) {
        when {
            event.code() == KeyCode.ENTER -> {
                updateScreen<ScreenState.Offline> { it.copy(isTyping = false) }
                return EventResult.HANDLED
            }

            event.code() == KeyCode.ESCAPE -> {
                updateScreen<ScreenState.Offline> { it.copy(isTyping = false, searchQuery = "") }
                return EventResult.HANDLED
            }

            event.code() == KeyCode.BACKSPACE -> {
                updateScreen<ScreenState.Offline> { it.copy(searchQuery = it.searchQuery.dropLast(1)) }
                return EventResult.HANDLED
            }

            event.code() == KeyCode.CHAR -> {
                val character = event.character()
                updateScreen<ScreenState.Offline> { it.copy(searchQuery = it.searchQuery + character) }
                return EventResult.HANDLED
            }
        }
        return EventResult.HANDLED
    }

    when {
        event.code() == KeyCode.CHAR && event.character() == '/' -> {
            updateScreen<ScreenState.Offline> { it.copy(isTyping = true) }
            return EventResult.HANDLED
        }

        (event.code() == KeyCode.CHAR && (event.character() == 'f' || event.character() == 'F')) ||
                (event.code() == KeyCode.TAB) -> {
            val nextFilter = when (actualState.filterType) {
                OfflineFilterType.ALL -> OfflineFilterType.MANUAL
                OfflineFilterType.MANUAL -> OfflineFilterType.CACHE
                OfflineFilterType.CACHE -> OfflineFilterType.ALL
            }
            updateScreen<ScreenState.Offline> { it.copy(filterType = nextFilter) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            updateScreen<ScreenState.Offline> { it.copy(searchQuery = "") }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) -> {
            val collection = actualState.downloads
            val newIndex = minOf(collection.lastIndex.coerceAtLeast(0), actualState.selectedIndex + 1)
            offlineList.selected(newIndex)
            updateScreen<ScreenState.Offline> { it.copy(selectedIndex = newIndex) }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            val newIndex = maxOf(0, actualState.selectedIndex - 1)
            offlineList.selected(newIndex)
            updateScreen<ScreenState.Offline> { it.copy(selectedIndex = newIndex) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            val collection = actualState.downloads
            val idx = actualState.selectedIndex
            if (idx in collection.indices) playList(collection.map { it.track }, idx)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && (event.character() == 'm' || event.character() == 'o') -> {
            val collection = actualState.downloads
            val track = collection.getOrNull(actualState.selectedIndex)?.track
            if (track != null) openTrackOptions(track)
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}