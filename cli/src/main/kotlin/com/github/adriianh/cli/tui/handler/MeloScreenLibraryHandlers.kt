package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.LibraryTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.playList
import com.github.adriianh.core.domain.model.MeloAction
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

    val actualState = state.screen as? ScreenState.Library ?: return EventResult.UNHANDLED
    val isFocused = appRunner()?.focusManager()?.focusedId() == "library-panel"
    if (!isFocused) return EventResult.UNHANDLED

    if (event.code() == KeyCode.CHAR && event.character() == '1') {
        updateScreen<ScreenState.Library> { it.copy(libraryTab = LibraryTab.FAVORITES) }
        return EventResult.HANDLED
    }
    if (event.code() == KeyCode.CHAR && event.character() == '2') {
        updateScreen<ScreenState.Library> { it.copy(libraryTab = LibraryTab.PLAYLISTS, isInPlaylistDetail = false) }
        return EventResult.HANDLED
    }
    if (event.code() == KeyCode.CHAR && event.character() == '3') {
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

internal fun MeloScreen.handleLocalLibraryKey(event: KeyEvent): EventResult {
    val actualState = state.screen as? ScreenState.Library ?: return EventResult.UNHANDLED
    val allPaths = settingsViewState.currentSettings.localLibraryPaths

    val filtered = actualState.localTracks.filter { track ->
        val matchesTab = if (actualState.localFilterIndex == 0) true else {
            val selectedPath = allPaths.getOrNull(actualState.localFilterIndex - 1)
            selectedPath != null && track.id.startsWith("local:$selectedPath")
        }

        val matchesSearch = if (actualState.searchQuery.isEmpty()) true else {
            val q = actualState.searchQuery.lowercase()
            track.title.lowercase().contains(q) || track.artist.lowercase().contains(q)
        }
        matchesTab && matchesSearch
    }

    if (actualState.isTyping) {
        when {
            event.code() == KeyCode.ENTER -> {
                updateScreen<ScreenState.Library> { it.copy(isTyping = false) }
                return EventResult.HANDLED
            }

            event.code() == KeyCode.ESCAPE -> {
                updateScreen<ScreenState.Library> { it.copy(isTyping = false, searchQuery = "") }
                return EventResult.HANDLED
            }

            event.code() == KeyCode.BACKSPACE -> {
                updateScreen<ScreenState.Library> { it.copy(searchQuery = it.searchQuery.dropLast(1)) }
                return EventResult.HANDLED
            }

            event.code() == KeyCode.CHAR -> {
                val character = event.character()
                updateScreen<ScreenState.Library> { it.copy(searchQuery = it.searchQuery + character) }
                return EventResult.HANDLED
            }
        }
        return EventResult.HANDLED
    }

    when {
        event.code() == KeyCode.TAB || (event.code() == KeyCode.CHAR && event.character() == 'l') -> {
            val next = (actualState.localFilterIndex + 1) % (allPaths.size + 1)
            updateScreen<ScreenState.Library> { it.copy(localFilterIndex = next, selectedIndex = 0) }
            localLibraryList.selected(0)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'f' -> {
            val prev = if (actualState.localFilterIndex == 0) allPaths.size else actualState.localFilterIndex - 1
            updateScreen<ScreenState.Library> { it.copy(localFilterIndex = prev, selectedIndex = 0) }
            localLibraryList.selected(0)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == '/' -> {
            updateScreen<ScreenState.Library> { it.copy(isTyping = true) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            updateScreen<ScreenState.Library> { it.copy(searchQuery = "") }
            return EventResult.HANDLED
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

        event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
            filtered.getOrNull(localLibraryList.selected())?.let { addToQueue(it) }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handleFavoritesKey(event: KeyEvent): EventResult {
    when {
        event.matches(Actions.MOVE_DOWN) -> {
            favoritesList.selected(
                minOf(
                    state.collections.favorites.lastIndex.coerceAtLeast(0),
                    favoritesList.selected() + 1
                )
            )
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            favoritesList.selected(maxOf(0, favoritesList.selected() - 1))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            val tracks = state.collections.favorites
            val idx = favoritesList.selected()
            if (idx in tracks.indices) playList(tracks, idx)
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
            state.collections.favorites.getOrNull(favoritesList.selected())?.let { removeFavoriteTrack(it) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
            state.collections.favorites.getOrNull(favoritesList.selected())?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings) -> {
            val track = state.collections.favorites.getOrNull(favoritesList.selected())
            if (track != null) openPlaylistPicker(track)
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}