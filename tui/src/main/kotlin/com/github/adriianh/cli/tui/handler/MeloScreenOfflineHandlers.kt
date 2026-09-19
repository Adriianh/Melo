package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.OfflineFilterType
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.filterAndSortOfflineTracks
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playList
import com.github.adriianh.core.domain.model.MeloAction
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

/**
 * Handles key events for the Offline (Downloads) screen.
 */
internal fun MeloScreen.handleOfflineKey(event: KeyEvent): EventResult {
    val actualState = state.screen as? ScreenState.Offline ?: return handleGlobalShortcuts(event)

    val filteredDownloads = filterAndSortOfflineTracks(
        downloads = actualState.downloads,
        filterType = actualState.filterType,
        sortOrder = actualState.sortOrder,
        sortDirection = actualState.sortDirection,
        query = actualState.searchQuery
    )

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
                val text = event.string()
                updateScreen<ScreenState.Offline> { it.copy(searchQuery = it.searchQuery + text) }
                return EventResult.HANDLED
            }
        }
        return EventResult.HANDLED
    }

    when {
        event.isCtrlF() -> {
            updateScreen<ScreenState.Offline> { it.copy(isTyping = true) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.TAB || event.isCharIgnoreCase('s') -> {
            val nextFilter = when (actualState.filterType) {
                OfflineFilterType.ALL -> OfflineFilterType.MANUAL
                OfflineFilterType.MANUAL -> OfflineFilterType.CACHE
                OfflineFilterType.CACHE -> OfflineFilterType.ALL
            }
            updateScreen<ScreenState.Offline> {
                it.copy(
                    filterType = nextFilter,
                    selectedIndex = 0
                )
            }
            offlineList.selected(0)
            return EventResult.HANDLED
        }

        event.isChar('O') || (event.modifiers().shift() && event.isCharIgnoreCase('o')) -> {
            updateScreen<ScreenState.Offline> {
                it.copy(
                    sortDirection = it.sortDirection.toggle(),
                    selectedIndex = 0
                )
            }
            offlineList.selected(0)
            return EventResult.HANDLED
        }

        event.isChar('o') && !event.modifiers().shift() -> {
            updateScreen<ScreenState.Offline> {
                it.copy(
                    sortOrder = it.sortOrder.next(),
                    selectedIndex = 0
                )
            }
            offlineList.selected(0)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            if (actualState.searchQuery.isNotEmpty()) {
                updateScreen<ScreenState.Offline> { it.copy(searchQuery = "") }
                return EventResult.HANDLED
            }
        }

        event.matches(Actions.MOVE_DOWN) -> {
            val newIndex =
                minOf(filteredDownloads.lastIndex.coerceAtLeast(0), actualState.selectedIndex + 1)
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
            val idx = actualState.selectedIndex
            if (idx in filteredDownloads.indices) playList(filteredDownloads.map { it.track }, idx)
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('m') -> {
            val track = filteredDownloads.getOrNull(actualState.selectedIndex)?.track
            if (track != null) openTrackOptions(track)
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('d') || event.matchesAction(
            MeloAction.DELETE,
            settingsViewState.currentSettings
        ) -> {
            val track = filteredDownloads.getOrNull(actualState.selectedIndex)?.track
            if (track != null) {
                deleteDownloadedTrack(track.id)
            }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
            filteredDownloads.getOrNull(actualState.selectedIndex)?.let { addToQueue(it.track) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
            filteredDownloads.getOrNull(actualState.selectedIndex)?.let { toggleFavorite(it.track) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings) -> {
            filteredDownloads.getOrNull(actualState.selectedIndex)
                ?.let { openPlaylistPicker(it.track) }
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}