package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.LibraryTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.filterAndSortTracks
import com.github.adriianh.cli.tui.filteredAndSortedFavorites
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
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

internal fun MeloScreen.handleLocalLibraryKey(event: KeyEvent): EventResult {
    val actualState = state.screen as? ScreenState.Library ?: return handleGlobalShortcuts(event)
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

    val filtered = filterAndSortTracks(
        tracks = tabFiltered,
        sortOrder = actualState.localSortOrder,
        sortDirection = actualState.localSortDirection,
        query = actualState.localSearchQuery
    )

    if (actualState.isTyping) {
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

    when {
        event.code() == KeyCode.TAB -> {
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

        event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
            filtered.getOrNull(localLibraryList.selected())?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
            filtered.getOrNull(localLibraryList.selected())?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings) -> {
            filtered.getOrNull(localLibraryList.selected())?.let { openPlaylistPicker(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('m') -> {
            filtered.getOrNull(localLibraryList.selected())?.let { openTrackOptions(it) }
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}

internal fun MeloScreen.handleFavoritesKey(event: KeyEvent): EventResult {
    val actualState = state.screen as? ScreenState.Library ?: return handleGlobalShortcuts(event)

    val filtered = state.filteredAndSortedFavorites(
        sourceFilter = actualState.favoritesSourceFilter,
        sortOrder = actualState.favoritesSortOrder,
        sortDirection = actualState.favoritesSortDirection,
        query = actualState.favoritesSearchQuery
    )

    if (actualState.isTyping) {
        when {
            event.code() == KeyCode.ENTER -> {
                updateScreen<ScreenState.Library> { it.copy(isTyping = false) }
                return EventResult.HANDLED
            }

            event.code() == KeyCode.ESCAPE -> {
                updateScreen<ScreenState.Library> {
                    it.copy(
                        isTyping = false,
                        favoritesSearchQuery = ""
                    )
                }
                return EventResult.HANDLED
            }

            event.code() == KeyCode.BACKSPACE -> {
                updateScreen<ScreenState.Library> {
                    it.copy(
                        favoritesSearchQuery = it.favoritesSearchQuery.dropLast(
                            1
                        )
                    )
                }
                return EventResult.HANDLED
            }

            event.code() == KeyCode.CHAR -> {
                val text = event.string()
                updateScreen<ScreenState.Library> { it.copy(favoritesSearchQuery = it.favoritesSearchQuery + text) }
                return EventResult.HANDLED
            }
        }
        return EventResult.HANDLED
    }

    when {
        event.isCtrlF() -> {
            updateScreen<ScreenState.Library> { it.copy(isTyping = true) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('s') -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    favoritesSourceFilter = it.favoritesSourceFilter.next(),
                    selectedIndex = 0
                )
            }
            favoritesList.selected(0)
            return EventResult.HANDLED
        }

        event.isChar('O') || (event.modifiers().shift() && event.isCharIgnoreCase('o')) -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    favoritesSortDirection = it.favoritesSortDirection.toggle(),
                    selectedIndex = 0
                )
            }
            favoritesList.selected(0)
            return EventResult.HANDLED
        }

        event.isChar('o') && !event.modifiers().shift() -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    favoritesSortOrder = it.favoritesSortOrder.next(),
                    selectedIndex = 0
                )
            }
            favoritesList.selected(0)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            if (actualState.favoritesSearchQuery.isNotEmpty()) {
                updateScreen<ScreenState.Library> { it.copy(favoritesSearchQuery = "") }
                return EventResult.HANDLED
            }
        }

        event.matches(Actions.MOVE_DOWN) -> {
            favoritesList.selected(
                minOf(
                    filtered.lastIndex.coerceAtLeast(0),
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
            val tracks = filtered.map { it.track }
            val idx = favoritesList.selected()
            if (idx in tracks.indices) playList(tracks, idx)
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
            filtered.getOrNull(favoritesList.selected())?.let { removeFavoriteTrack(it.track) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
            filtered.getOrNull(favoritesList.selected())?.let { addToQueue(it.track) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings) -> {
            val item = filtered.getOrNull(favoritesList.selected())
            if (item != null) openPlaylistPicker(item.track)
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('m') -> {
            val item = filtered.getOrNull(favoritesList.selected())
            if (item != null) openTrackOptions(item.track)
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('y') -> {
            syncYouTubeLibrary()
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}