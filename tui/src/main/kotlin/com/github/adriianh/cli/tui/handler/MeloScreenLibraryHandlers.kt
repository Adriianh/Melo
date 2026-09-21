package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.LibraryTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openBatchOptions
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playList
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.filterAndSortTracks
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

/**
 * Handles key events for the Library screen, including Favorites, Playlists, and Local sections.
 */
internal fun MeloScreen.handleLibraryKey(event: KeyEvent): EventResult {
    when (state.playlistInteraction.playlistInputMode) {
        PlaylistInputMode.CREATE,
        PlaylistInputMode.RENAME -> return handlePlaylistInput(event)

        PlaylistInputMode.PICKER -> return handlePlaylistPicker(event)
        PlaylistInputMode.NONE -> {}
    }

    val actualState = state.screen as? ScreenState.Library ?: return handleGlobalShortcuts(event)
    val isFocused = appRunner()?.focusManager()?.focusedId() == "library-panel"
    if (!isFocused) return handleGlobalShortcuts(event)

    if (event.isChar('1')) {
        updateScreen<ScreenState.Library> { it.copy(libraryTab = LibraryTab.FAVORITES) }
        return EventResult.HANDLED
    }
    if (event.isChar('2')) {
        updateScreen<ScreenState.Library> { it.copy(libraryTab = LibraryTab.PLAYLISTS, isInPlaylistDetail = false) }
        return EventResult.HANDLED
    }
    if (event.isChar('3')) {
        updateScreen<ScreenState.Library> { it.copy(libraryTab = LibraryTab.LOCAL) }
        loadLocalTracks()
        return EventResult.HANDLED
    }

    return when (actualState.libraryTab) {
        LibraryTab.FAVORITES -> handleFavoritesKey(event)
        LibraryTab.PLAYLISTS -> if (actualState.isInPlaylistDetail) handlePlaylistDetailKey(event) else handlePlaylistsKey(
            event
        )

        LibraryTab.LOCAL -> handleLocalLibraryKey(event)
    }
}

/** Acciones comunes de lista de tracks: selección múltiple, favoritos, cola, playlist y opciones. */
internal fun MeloScreen.handleTrackListSelectionActions(
    event: KeyEvent,
    tracks: List<Track>,
    selectedIndex: Int,
    onFavorite: (Track) -> Unit
): EventResult {
    val track = tracks.getOrNull(selectedIndex)
    when {
        event.isCharIgnoreCase('v') || event.matchesAction(
            MeloAction.TOGGLE_SELECTION,
            settingsViewState.currentSettings
        ) || (state.selection.isNotEmpty && event.isChar(' ')) -> {
            if (track != null) {
                state = state.copy(selection = state.selection.toggle(track))
                return EventResult.HANDLED
            }
        }

        event.isCtrlA() -> {
            state = state.copy(selection = state.selection.selectAll(tracks))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE && state.selection.isNotEmpty -> {
            state = state.copy(selection = state.selection.clear())
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
            if (track != null) onFavorite(track)
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
            if (track != null) addToQueue(track)
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings) -> {
            if (state.selection.isNotEmpty) {
                openPlaylistPicker(state.selection.tracks())
            } else if (track != null) {
                openPlaylistPicker(track)
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
            if (track != null) openTrackOptions(track)
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

private fun MeloScreen.localFilteredAndSortedTracks(actualState: ScreenState.Library): List<Track> {
    val allPaths = settingsViewState.currentSettings.localLibraryPaths
    val tabFiltered = actualState.localTracks.filter { track ->
        if (actualState.localFilterIndex == 0) true else {
            val selectedPath = allPaths.getOrNull(actualState.localFilterIndex - 1)
            if (selectedPath != null) {
                val clean = selectedPath.trimEnd('/')
                track.id.startsWith("local:$clean/") || track.id == "local:$clean"
            } else false
        }
    }

    return filterAndSortTracks(
        tracks = tabFiltered,
        sortOrder = actualState.localSortOrder,
        sortDirection = actualState.localSortDirection,
        query = actualState.localSearchQuery
    )
}

private fun MeloScreen.handleLocalTypingKey(event: KeyEvent): EventResult {
    when {
        event.code() == KeyCode.ENTER -> {
            updateScreen<ScreenState.Library> { it.copy(isTyping = false) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    isTyping = false,
                    localSearchQuery = "",
                    searchQuery = ""
                )
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.BACKSPACE -> {
            updateScreen<ScreenState.Library> {
                val newQ = it.localSearchQuery.dropLast(1)
                it.copy(localSearchQuery = newQ, searchQuery = newQ)
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR -> {
            val text = event.string()
            updateScreen<ScreenState.Library> {
                val newQ = it.localSearchQuery + text
                it.copy(localSearchQuery = newQ, searchQuery = newQ)
            }
            return EventResult.HANDLED
        }
    }
    return EventResult.HANDLED
}

private fun MeloScreen.handleLocalNavigationKey(
    actualState: ScreenState.Library,
    filtered: List<Track>,
    event: KeyEvent
): EventResult {
    when {
        event.code() == KeyCode.TAB -> {
            val allPaths = settingsViewState.currentSettings.localLibraryPaths
            val next = (actualState.localFilterIndex + 1) % (allPaths.size + 1)
            updateScreen<ScreenState.Library> { it.copy(localFilterIndex = next, selectedIndex = 0) }
            localLibraryList.selected(0)
            return EventResult.HANDLED
        }

        event.isCtrlF() -> {
            updateScreen<ScreenState.Library> { it.copy(isTyping = true) }
            return EventResult.HANDLED
        }

        event.isChar('O') || (event.modifiers().shift() && event.isCharIgnoreCase('o')) -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    localSortDirection = it.localSortDirection.toggle(),
                    selectedIndex = 0
                )
            }
            localLibraryList.selected(0)
            return EventResult.HANDLED
        }

        event.isChar('o') && !event.modifiers().shift() -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    localSortOrder = it.localSortOrder.next(),
                    selectedIndex = 0
                )
            }
            localLibraryList.selected(0)
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('r') -> {
            loadLocalTracks()
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            if (actualState.localSearchQuery.isNotEmpty()) {
                updateScreen<ScreenState.Library> {
                    it.copy(
                        localSearchQuery = "",
                        searchQuery = ""
                    )
                }
                return EventResult.HANDLED
            }
        }

        event.matches(Actions.MOVE_DOWN) -> {
            val newIndex = minOf(filtered.lastIndex.coerceAtLeast(0), localLibraryList.selected() + 1)
            localLibraryList.selected(newIndex)
            updateScreen<ScreenState.Library> { it.copy(selectedIndex = newIndex) }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            val newIndex = maxOf(0, localLibraryList.selected() - 1)
            localLibraryList.selected(newIndex)
            updateScreen<ScreenState.Library> { it.copy(selectedIndex = newIndex) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            val idx = localLibraryList.selected()
            if (idx in filtered.indices) playList(filtered, idx)
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handleLocalLibraryKey(event: KeyEvent): EventResult {
    val actualState = state.screen as? ScreenState.Library ?: return handleGlobalShortcuts(event)
    val filtered = localFilteredAndSortedTracks(actualState)

    if (actualState.isTyping) return handleLocalTypingKey(event)

    val navResult = handleLocalNavigationKey(actualState, filtered, event)
    if (navResult == EventResult.HANDLED) return navResult

    val actionResult = handleTrackListSelectionActions(
        event = event,
        tracks = filtered,
        selectedIndex = localLibraryList.selected(),
        onFavorite = { toggleFavorite(it) }
    )
    return if (actionResult == EventResult.HANDLED) actionResult else handleGlobalShortcuts(event)
}