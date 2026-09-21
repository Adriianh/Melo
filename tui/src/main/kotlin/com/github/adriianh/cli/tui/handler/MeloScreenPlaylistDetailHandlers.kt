package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openBatchOptions
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playList
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.SortDirection
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackSortOrder
import com.github.adriianh.core.domain.model.filterAndSortTracks
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch

/**
 * Handles key events while viewing a playlist's track list: typing, navigation,
 * reordering, selection actions and track removal.
 */
internal fun MeloScreen.handlePlaylistDetailKey(event: KeyEvent): EventResult {
    val screen = state.screen as? ScreenState.Library ?: return handleGlobalShortcuts(event)

    if (screen.isTyping) return handlePlaylistDetailTypingKey(event)

    val filtered = filterAndSortTracks(
        tracks = screen.playlistTracks,
        sortOrder = screen.playlistDetailSortOrder,
        sortDirection = screen.playlistDetailSortDirection,
        query = screen.playlistDetailSearchQuery
    )

    val navResult = handlePlaylistDetailNavigationKey(screen, filtered, event)
    if (navResult == EventResult.HANDLED) return navResult

    return handlePlaylistDetailSelectionActions(screen, filtered, event)
}

internal fun MeloScreen.reorderPlaylistTrack(fromIndex: Int, toIndex: Int) {
    val screen = state.screen as? ScreenState.Library ?: return
    val pl = screen.selectedPlaylist ?: return

    if (screen.playlistDetailSortOrder != TrackSortOrder.DEFAULT || screen.playlistDetailSearchQuery.isNotBlank()) {
        return
    }

    val currentTracks = screen.playlistTracks.toMutableList()
    if (fromIndex !in currentTracks.indices || toIndex !in currentTracks.indices || fromIndex == toIndex) return

    val moved = currentTracks.removeAt(fromIndex)
    currentTracks.add(toIndex, moved)
    updateScreen<ScreenState.Library> { it.copy(playlistTracks = currentTracks) }
    playlistTracksList.selected(toIndex)

    scope.launch {
        reorderPlaylistTracks?.invoke(pl.id, currentTracks.map { it.id })
    }
}

internal fun MeloScreen.openLocalPlaylistDetail(pl: Playlist, autoPlay: Boolean = false) {
    updateScreen<ScreenState.Library> {
        it.copy(
            selectedPlaylist = pl,
            isInPlaylistDetail = true,
            playlistTracks = emptyList(),
            playlistDetailSearchQuery = "",
            playlistDetailSortOrder = TrackSortOrder.DEFAULT,
            playlistDetailSortDirection = SortDirection.ASCENDING,
            isTyping = false
        )
    }
    playlistTracksJob?.cancel()
    playlistTracksJob = scope.launch {
        getPlaylistTracks(pl.id).collect { tracks ->
            appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Library> { it.copy(playlistTracks = tracks) }
                if (autoPlay && tracks.isNotEmpty()) {
                    playList(tracks, 0)
                }
            }
        }
    }
}

private fun MeloScreen.handlePlaylistDetailTypingKey(event: KeyEvent): EventResult {
    when {
        event.code() == KeyCode.ENTER -> {
            updateScreen<ScreenState.Library> { it.copy(isTyping = false) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    isTyping = false,
                    playlistDetailSearchQuery = ""
                )
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.BACKSPACE -> {
            updateScreen<ScreenState.Library> {
                it.copy(playlistDetailSearchQuery = it.playlistDetailSearchQuery.dropLast(1))
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR -> {
            val text = event.string()
            updateScreen<ScreenState.Library> {
                it.copy(playlistDetailSearchQuery = it.playlistDetailSearchQuery + text)
            }
            return EventResult.HANDLED
        }
    }
    return EventResult.HANDLED
}

/** Navigation and sorting for the playlist detail: search, sort, exit, reorder and movement. */
private fun MeloScreen.handlePlaylistDetailNavigationKey(
    screen: ScreenState.Library,
    filtered: List<Track>,
    event: KeyEvent
): EventResult {
    when {
        event.isCtrlF() -> {
            updateScreen<ScreenState.Library> { it.copy(isTyping = true) }
            return EventResult.HANDLED
        }

        event.isChar('O') || (event.modifiers().shift() && event.isCharIgnoreCase('o')) -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    playlistDetailSortDirection = it.playlistDetailSortDirection.toggle(),
                    selectedIndex = 0
                )
            }
            playlistTracksList.selected(0)
            return EventResult.HANDLED
        }

        event.isChar('o') && !event.modifiers().shift() -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    playlistDetailSortOrder = it.playlistDetailSortOrder.next(),
                    selectedIndex = 0
                )
            }
            playlistTracksList.selected(0)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            if (screen.playlistDetailSearchQuery.isNotEmpty()) {
                updateScreen<ScreenState.Library> { it.copy(playlistDetailSearchQuery = "") }
                return EventResult.HANDLED
            }
            updateScreen<ScreenState.Library> {
                it.copy(
                    isInPlaylistDetail = false,
                    selectedPlaylist = null,
                    playlistTracks = emptyList(),
                    playlistDetailSearchQuery = "",
                    playlistDetailSortOrder = TrackSortOrder.DEFAULT,
                    playlistDetailSortDirection = SortDirection.ASCENDING
                )
            }
            playlistTracksJob?.cancel()
            return EventResult.HANDLED
        }

        (event.modifiers().shift() && event.code() == KeyCode.UP) ||
                (event.modifiers().alt() && event.code() == KeyCode.UP) ||
                (event.modifiers().ctrl() && event.code() == KeyCode.UP) ||
                event.isChar('K') -> {
            val selected = playlistTracksList.selected()
            if (selected > 0) {
                reorderPlaylistTrack(selected, selected - 1)
            }
            return EventResult.HANDLED
        }

        (event.modifiers().shift() && event.code() == KeyCode.DOWN) ||
                (event.modifiers().alt() && event.code() == KeyCode.DOWN) ||
                (event.modifiers().ctrl() && event.code() == KeyCode.DOWN) ||
                event.isChar('J') -> {
            val selected = playlistTracksList.selected()
            if (selected < filtered.lastIndex) {
                reorderPlaylistTrack(selected, selected + 1)
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) -> {
            playlistTracksList.selected(
                minOf(
                    filtered.lastIndex.coerceAtLeast(0),
                    playlistTracksList.selected() + 1
                )
            )
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            playlistTracksList.selected(maxOf(0, playlistTracksList.selected() - 1))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            val idx = playlistTracksList.selected()
            if (idx in filtered.indices) playList(filtered, idx)
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

/** Selection, queue, favorite, playlist, options and removal actions for the playlist detail. */
private fun MeloScreen.handlePlaylistDetailSelectionActions(
    screen: ScreenState.Library,
    filtered: List<Track>,
    event: KeyEvent
): EventResult {
    when {
        event.matchesAction(
            MeloAction.ADD_TO_QUEUE,
            settingsViewState.currentSettings
        ) || event.isCharIgnoreCase('q') -> {
            filtered.getOrNull(playlistTracksList.selected())?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('v') || event.matchesAction(
            MeloAction.TOGGLE_SELECTION,
            settingsViewState.currentSettings
        ) || (state.selection.isNotEmpty && event.isChar(' ')) -> {
            val track = filtered.getOrNull(playlistTracksList.selected())
            if (track != null) {
                state = state.copy(selection = state.selection.toggle(track))
                return EventResult.HANDLED
            }
        }

        event.isCtrlA() -> {
            state = state.copy(selection = state.selection.selectAll(filtered))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE && state.selection.isNotEmpty -> {
            state = state.copy(selection = state.selection.clear())
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
            filtered.getOrNull(playlistTracksList.selected())?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings) -> {
            if (state.selection.isNotEmpty) {
                openPlaylistPicker(state.selection.tracks())
            } else {
                filtered.getOrNull(playlistTracksList.selected())?.let { openPlaylistPicker(it) }
            }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('m') || event.matchesAction(
            MeloAction.TRACK_OPTIONS,
            settingsViewState.currentSettings
        ) -> {
            if (state.selection.isNotEmpty) {
                openBatchOptions(state.selection.tracks())
                return EventResult.HANDLED
            }
            filtered.getOrNull(playlistTracksList.selected())?.let { openTrackOptions(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('d') || event.code() == KeyCode.DELETE -> {
            val pl = screen.selectedPlaylist ?: return handleGlobalShortcuts(event)
            if (state.selection.isNotEmpty) {
                val toRemove = state.selection.tracks()
                scope.launch {
                    toRemove.forEach { track ->
                        removeTrackFromPlaylist(pl.id, track.id)
                    }
                }
                state = state.copy(selection = state.selection.clear())
                return EventResult.HANDLED
            }
            val track = filtered.getOrNull(playlistTracksList.selected())
                ?: return handleGlobalShortcuts(event)
            scope.launch { removeTrackFromPlaylist(pl.id, track.id) }
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}