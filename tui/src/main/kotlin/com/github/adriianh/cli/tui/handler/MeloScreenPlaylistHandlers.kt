package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.LibraryPlaylistItem
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.filteredAndSortedPlaylists
import com.github.adriianh.cli.tui.handler.search.openEntityDetails
import com.github.adriianh.cli.tui.util.ToastKind
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch

/**
 * Handles key events while the Playlists tab of the Library screen is focused:
 * typing, sorting, filtering, opening, creating, renaming, deleting and syncing playlists.
 */
internal fun MeloScreen.handlePlaylistsKey(event: KeyEvent): EventResult {
    val actualState = state.screen as? ScreenState.Library ?: return handleGlobalShortcuts(event)

    if (actualState.isTyping) return handlePlaylistTypingKey(event)

    val filtered = state.filteredAndSortedPlaylists(
        sourceFilter = actualState.playlistsSourceFilter,
        sortOrder = actualState.playlistsSortOrder,
        sortDirection = actualState.playlistsSortDirection,
        query = actualState.playlistsSearchQuery
    )

    val navResult = handlePlaylistListNavigationKey(actualState, filtered, event)
    if (navResult == EventResult.HANDLED) return navResult

    return handlePlaylistListActionsKey(filtered, event)
}

internal fun MeloScreen.syncYouTubePlaylists() {
    val settings = settingsViewState.currentSettings
    val isLoggedIn = !settings.sessionCookies.isNullOrBlank()
    if (!isLoggedIn) return
    scope.launch {
        try {
            val result = getUserPlaylists?.invoke() ?: return@launch
            val playlists = result.getOrNull().orEmpty()
            appRunner()?.runOnRenderThread {
                state = state.copy(
                    collections = state.collections.copy(remotePlaylists = playlists)
                )
            }
        } catch (_: Exception) {
        }
    }
}

internal fun MeloScreen.syncYouTubeLibrary() {
    syncYouTubeFavorites()
    syncYouTubePlaylists()
    syncYouTubeHistory()
}

private fun MeloScreen.handlePlaylistTypingKey(event: KeyEvent): EventResult {
    when {
        event.code() == KeyCode.ENTER -> {
            updateScreen<ScreenState.Library> { it.copy(isTyping = false) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    isTyping = false,
                    playlistsSearchQuery = ""
                )
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.BACKSPACE -> {
            updateScreen<ScreenState.Library> {
                it.copy(playlistsSearchQuery = it.playlistsSearchQuery.dropLast(1))
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR -> {
            val text = event.string()
            updateScreen<ScreenState.Library> { it.copy(playlistsSearchQuery = it.playlistsSearchQuery + text) }
            return EventResult.HANDLED
        }
    }
    return EventResult.HANDLED
}

/** Navigation and sorting for the playlist list: search, source filter, sort and opening. */
private fun MeloScreen.handlePlaylistListNavigationKey(
    actualState: ScreenState.Library,
    filtered: List<LibraryPlaylistItem>,
    event: KeyEvent
): EventResult {
    when {
        event.isCtrlF() -> {
            updateScreen<ScreenState.Library> { it.copy(isTyping = true) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('s') -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    playlistsSourceFilter = it.playlistsSourceFilter.next(),
                    selectedIndex = 0
                )
            }
            playlistsList.selected(0)
            return EventResult.HANDLED
        }

        event.isChar('O') || (event.modifiers().shift() && event.isCharIgnoreCase('o')) -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    playlistsSortDirection = it.playlistsSortDirection.toggle(),
                    selectedIndex = 0
                )
            }
            playlistsList.selected(0)
            return EventResult.HANDLED
        }

        event.isChar('o') && !event.modifiers().shift() -> {
            updateScreen<ScreenState.Library> {
                it.copy(
                    playlistsSortOrder = it.playlistsSortOrder.next(),
                    selectedIndex = 0
                )
            }
            playlistsList.selected(0)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            if (actualState.playlistsSearchQuery.isNotEmpty()) {
                updateScreen<ScreenState.Library> { it.copy(playlistsSearchQuery = "") }
                return EventResult.HANDLED
            }
        }

        event.matches(Actions.MOVE_DOWN) -> {
            playlistsList.selected(
                minOf(
                    filtered.lastIndex.coerceAtLeast(0),
                    playlistsList.selected() + 1
                )
            )
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            playlistsList.selected(maxOf(0, playlistsList.selected() - 1))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            val item = filtered.getOrNull(playlistsList.selected()) ?: return EventResult.HANDLED
            when (item) {
                is LibraryPlaylistItem.Local -> openLocalPlaylistDetail(item.playlist)
                is LibraryPlaylistItem.Remote -> openEntityDetails(item.playlist)
            }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

/** Actions on the playlist list: create, rename, delete, play and sync. */
private fun MeloScreen.handlePlaylistListActionsKey(
    filtered: List<LibraryPlaylistItem>,
    event: KeyEvent
): EventResult {
    when {
        event.isCharIgnoreCase('n') -> {
            state = state.copy(
                playlistInteraction = state.playlistInteraction.copy(
                    playlistInputMode = PlaylistInputMode.CREATE,
                    playlistInput = ""
                )
            )
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('r') -> {
            val item = filtered.getOrNull(playlistsList.selected()) ?: return handleGlobalShortcuts(event)
            if (item is LibraryPlaylistItem.Local) {
                state = state.copy(
                    playlistInteraction = state.playlistInteraction.copy(
                        playlistInputMode = PlaylistInputMode.RENAME,
                        playlistInput = item.playlist.name
                    )
                )
                return EventResult.HANDLED
            }
        }

        event.isCharIgnoreCase('d') || event.code() == KeyCode.DELETE -> {
            val item = filtered.getOrNull(playlistsList.selected()) ?: return handleGlobalShortcuts(event)
            if (item is LibraryPlaylistItem.Local) {
                val playlistName = item.playlist.name
                scope.launch {
                    deletePlaylist(item.playlist.id)
                    appRunner()?.runOnRenderThread {
                        showToast("Playlist deleted: '$playlistName'", ToastKind.SUCCESS)
                    }
                }
                return EventResult.HANDLED
            }
        }

        event.isCharIgnoreCase('p') -> {
            val item = filtered.getOrNull(playlistsList.selected()) ?: return handleGlobalShortcuts(event)
            when (item) {
                is LibraryPlaylistItem.Local -> openLocalPlaylistDetail(item.playlist, autoPlay = true)
                is LibraryPlaylistItem.Remote -> openEntityDetails(item.playlist)
            }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('y') -> {
            syncYouTubeLibrary()
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}