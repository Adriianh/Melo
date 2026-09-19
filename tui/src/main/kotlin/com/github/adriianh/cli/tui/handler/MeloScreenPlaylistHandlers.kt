package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.LibraryPlaylistItem
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SortDirection
import com.github.adriianh.cli.tui.TrackSortOrder
import com.github.adriianh.cli.tui.filterAndSortTracks
import com.github.adriianh.cli.tui.filteredAndSortedPlaylists
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.playFromQueue
import com.github.adriianh.cli.tui.handler.playback.playList
import com.github.adriianh.cli.tui.handler.search.openEntityDetails
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch

internal fun MeloScreen.handlePlaylistsKey(event: KeyEvent): EventResult {
    val actualState = state.screen as? ScreenState.Library ?: return handleGlobalShortcuts(event)

    val filtered = state.filteredAndSortedPlaylists(
        sourceFilter = actualState.playlistsSourceFilter,
        sortOrder = actualState.playlistsSortOrder,
        sortDirection = actualState.playlistsSortDirection,
        query = actualState.playlistsSearchQuery
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
                        playlistsSearchQuery = ""
                    )
                }
                return EventResult.HANDLED
            }

            event.code() == KeyCode.BACKSPACE -> {
                updateScreen<ScreenState.Library> {
                    it.copy(
                        playlistsSearchQuery = it.playlistsSearchQuery.dropLast(
                            1
                        )
                    )
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
            val item =
                filtered.getOrNull(playlistsList.selected()) ?: return EventResult.HANDLED
            when (item) {
                is LibraryPlaylistItem.Local -> openLocalPlaylistDetail(item.playlist)
                is LibraryPlaylistItem.Remote -> openEntityDetails(item.playlist)
            }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('n') -> {
            state = state.copy(playlistInteraction = state.playlistInteraction.copy(playlistInputMode = PlaylistInputMode.CREATE, playlistInput = ""))
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('r') -> {
            val item =
                filtered.getOrNull(playlistsList.selected()) ?: return handleGlobalShortcuts(
                    event
                )
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
            val item =
                filtered.getOrNull(playlistsList.selected()) ?: return handleGlobalShortcuts(
                    event
                )
            if (item is LibraryPlaylistItem.Local) {
                scope.launch { deletePlaylist(item.playlist.id) }
                return EventResult.HANDLED
            }
        }

        event.isCharIgnoreCase('p') -> {
            val item =
                filtered.getOrNull(playlistsList.selected()) ?: return handleGlobalShortcuts(
                    event
                )
            when (item) {
                is LibraryPlaylistItem.Local -> openLocalPlaylistDetail(
                    item.playlist,
                    autoPlay = true
                )

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

internal fun MeloScreen.handlePlaylistDetailKey(event: KeyEvent): EventResult {
    val screen = state.screen as? ScreenState.Library ?: return handleGlobalShortcuts(event)

    val filtered = filterAndSortTracks(
        tracks = screen.playlistTracks,
        sortOrder = screen.playlistDetailSortOrder,
        sortDirection = screen.playlistDetailSortDirection,
        query = screen.playlistDetailSearchQuery
    )

    if (screen.isTyping) {
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
                    it.copy(
                        playlistDetailSearchQuery = it.playlistDetailSearchQuery.dropLast(
                            1
                        )
                    )
                }
                return EventResult.HANDLED
            }

            event.code() == KeyCode.CHAR -> {
                val text = event.string()
                updateScreen<ScreenState.Library> { it.copy(playlistDetailSearchQuery = it.playlistDetailSearchQuery + text) }
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

        event.isCharIgnoreCase('q') -> {
            filtered.getOrNull(playlistTracksList.selected())?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('d') || event.code() == KeyCode.DELETE -> {
            val pl = screen.selectedPlaylist ?: return handleGlobalShortcuts(event)
            val track = filtered.getOrNull(playlistTracksList.selected())
                ?: return handleGlobalShortcuts(event)
            scope.launch { removeTrackFromPlaylist(pl.id, track.id) }
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}

internal fun MeloScreen.handlePlaylistInput(event: KeyEvent): EventResult {
    val interaction = state.playlistInteraction
    when {
        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(playlistInteraction = interaction.copy(playlistInputMode = PlaylistInputMode.NONE, playlistInput = ""))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.ENTER -> {
            val name = interaction.playlistInput.trim()
            if (name.isNotBlank()) {
                when (interaction.playlistInputMode) {
                    PlaylistInputMode.CREATE -> scope.launch { createPlaylist(name) }
                    PlaylistInputMode.RENAME -> {
                        val pl = state.collections.playlists.getOrNull(playlistsList.selected())
                        if (pl != null) scope.launch { renamePlaylist(pl.id, name) }
                    }
                    PlaylistInputMode.PICKER, PlaylistInputMode.NONE -> {}
                }
            }
            state = state.copy(playlistInteraction = interaction.copy(playlistInputMode = PlaylistInputMode.NONE, playlistInput = ""))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.BACKSPACE -> {
            state = state.copy(playlistInteraction = interaction.copy(playlistInput = interaction.playlistInput.dropLast(1)))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && !event.modifiers().ctrl() && !event.modifiers().alt() -> {
            val str = event.string()
            if (str.isNotBlank() || str == " ") {
                state =
                    state.copy(playlistInteraction = interaction.copy(playlistInput = interaction.playlistInput + str))
            }
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}

internal fun MeloScreen.handlePlaylistPicker(event: KeyEvent): EventResult {
    val interaction = state.playlistInteraction
    val playlists = state.collections.playlists
    when {
        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(playlistInteraction = interaction.copy(playlistInputMode = PlaylistInputMode.NONE, playlistPickerTrack = null))
            return EventResult.HANDLED
        }
        event.matches(Actions.MOVE_DOWN) -> {
            state = state.copy(playlistInteraction = interaction.copy(playlistPickerCursor = minOf(playlists.lastIndex, interaction.playlistPickerCursor + 1)))
            return EventResult.HANDLED
        }
        event.matches(Actions.MOVE_UP) -> {
            state = state.copy(playlistInteraction = interaction.copy(playlistPickerCursor = maxOf(0, interaction.playlistPickerCursor - 1)))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.ENTER -> {
            val pl = playlists.getOrNull(interaction.playlistPickerCursor)
                ?: return handleGlobalShortcuts(event)
            val track = interaction.playlistPickerTrack ?: return handleGlobalShortcuts(event)
            scope.launch { addTrackToPlaylist(pl.id, track) }
            state = state.copy(playlistInteraction = interaction.copy(playlistInputMode = PlaylistInputMode.NONE, playlistPickerTrack = null))
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
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
                    state = state.copy(player = state.player.copy(queue = tracks, queueIndex = -1, isRadioMode = false))
                    playFromQueue(0)
                }
            }
        }
    }
}

internal fun MeloScreen.openPlaylistDetail(index: Int, autoPlay: Boolean = false) {
    val pl = state.collections.playlists.getOrNull(index) ?: return
    openLocalPlaylistDetail(pl, autoPlay)
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
}

internal fun MeloScreen.openPlaylistPicker(track: Track) {
    val playlists = state.collections.playlists
    
    state = if (playlists.isEmpty()) {
        state.copy(playlistInteraction = state.playlistInteraction.copy(playlistInputMode = PlaylistInputMode.CREATE, playlistInput = "", playlistPickerTrack = track))
    } else {
        state.copy(playlistInteraction = state.playlistInteraction.copy(playlistInputMode = PlaylistInputMode.PICKER, playlistPickerTrack = track, playlistPickerCursor = 0))
    }
}