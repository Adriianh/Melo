package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SearchTab
import com.github.adriianh.cli.tui.handler.handleGlobalShortcuts
import com.github.adriianh.cli.tui.handler.handlePlaylistInput
import com.github.adriianh.cli.tui.handler.handlePlaylistPicker
import com.github.adriianh.cli.tui.handler.isCtrlA
import com.github.adriianh.cli.tui.handler.matchesAction
import com.github.adriianh.cli.tui.handler.openPlaylistPicker
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openBatchOptions
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.cli.tui.handler.toggleFavorite
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.MeloAction
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

/** Actualiza selección y detalle al mover el cursor dentro de los resultados. */
private fun MeloScreen.updateSelectedResult(
    actualState: ScreenState.Search,
    newIndex: Int
) {
    state = state.copy(
        screen = actualState.copy(selectedIndex = newIndex),
        player = state.player.copy(marqueeOffset = 0)
    )
    marqueeTick = 0
    if (actualState.tab == SearchTab.SONGS) {
        actualState.results.getOrNull(newIndex)?.let { track ->
            state = state.copy(
                detail = state.detail.copy(
                    selectedTrack = track,
                    selectedEntity = null,
                    artworkData = null
                )
            )
            debouncedLoadDetails(track)
        }
    } else {
        val entity = when (actualState.tab) {
            SearchTab.ALBUMS -> actualState.albumResults.getOrNull(newIndex)
            SearchTab.ARTISTS -> actualState.artistResults.getOrNull(newIndex)
            SearchTab.PLAYLISTS -> actualState.playlistResults.getOrNull(newIndex)
            SearchTab.SONGS -> null
        }
        if (entity != null) {
            state = state.copy(
                detail = state.detail.copy(
                    selectedTrack = null,
                    selectedEntity = entity,
                    artworkData = null
                )
            )
            debouncedLoadEntityDetails(entity)
        }
    }
}

/** Movimiento por la lista de resultados y reproducción con ENTER. */
private fun MeloScreen.handleResultsMovementKey(
    actualState: ScreenState.Search,
    event: KeyEvent,
    isFocused: Boolean,
    listSize: Int
): EventResult {
    when {
        event.matches(Actions.MOVE_DOWN) -> {
            if (!isFocused) return handleGlobalShortcuts(event)
            val newIndex = minOf(listSize - 1, actualState.selectedIndex + 1)
            resultList.selected(newIndex)
            updateSelectedResult(actualState, newIndex)
            if (newIndex >= listSize - 5 && !actualState.isLoadingMore && actualState.hasMore) loadMore()
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            if (!isFocused) return handleGlobalShortcuts(event)
            val newIndex = maxOf(0, actualState.selectedIndex - 1)
            resultList.selected(newIndex)
            updateSelectedResult(actualState, newIndex)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            if (!isFocused) return handleGlobalShortcuts(event)
            when (actualState.tab) {
                SearchTab.SONGS -> {
                    val selected = actualState.results.getOrNull(resultList.selected())
                        ?: return handleGlobalShortcuts(event)
                    downloadTrack(selected, DownloadType.PREFETCH)
                    playTrack(selected)
                    return EventResult.HANDLED
                }

                SearchTab.ALBUMS -> {
                    val selected = actualState.albumResults.getOrNull(resultList.selected())
                        ?: return handleGlobalShortcuts(event)
                    openEntityDetails(selected)
                    return EventResult.HANDLED
                }

                SearchTab.ARTISTS -> {
                    val selected = actualState.artistResults.getOrNull(resultList.selected())
                        ?: return handleGlobalShortcuts(event)
                    openEntityDetails(selected)
                    return EventResult.HANDLED
                }

                SearchTab.PLAYLISTS -> {
                    val selected = actualState.playlistResults.getOrNull(resultList.selected())
                        ?: return handleGlobalShortcuts(event)
                    openEntityDetails(selected)
                    return EventResult.HANDLED
                }
            }
        }
    }
    return EventResult.UNHANDLED
}

/** Acciones sobre el resultado seleccionado (favoritos, cola, selección, opciones...). */
private fun MeloScreen.handleResultsActionKey(
    actualState: ScreenState.Search,
    event: KeyEvent
): EventResult {
    when {
        actualState.tab == SearchTab.SONGS && event.matchesAction(
            MeloAction.FAVORITE,
            settingsViewState.currentSettings
        ) -> {
            actualState.results.getOrNull(actualState.selectedIndex)?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        actualState.tab != SearchTab.SONGS && (
                event.isChar('F') || event.matchesAction(
                    MeloAction.FAVORITE,
                    settingsViewState.currentSettings
                )
                ) -> {
            val entity = when (actualState.tab) {
                SearchTab.ALBUMS -> actualState.albumResults.getOrNull(actualState.selectedIndex)
                SearchTab.ARTISTS -> actualState.artistResults.getOrNull(actualState.selectedIndex)
                SearchTab.PLAYLISTS -> actualState.playlistResults.getOrNull(actualState.selectedIndex)
                SearchTab.SONGS -> null
            }
            if (entity != null) {
                toggleEntityFavorite(entity)
                return EventResult.HANDLED
            }
        }

        actualState.tab == SearchTab.SONGS && event.matchesAction(
            MeloAction.ADD_TO_QUEUE,
            settingsViewState.currentSettings
        ) -> {
            actualState.results.getOrNull(actualState.selectedIndex)?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        actualState.tab == SearchTab.SONGS && (
                event.isCharIgnoreCase('v') || event.matchesAction(
                    MeloAction.TOGGLE_SELECTION,
                    settingsViewState.currentSettings
                ) || (state.selection.isNotEmpty && event.isChar(' '))
                ) -> {
            val track = actualState.results.getOrNull(actualState.selectedIndex)
            if (track != null) {
                state = state.copy(selection = state.selection.toggle(track))
                return EventResult.HANDLED
            }
        }

        actualState.tab == SearchTab.SONGS && event.isCtrlA() -> {
            state = state.copy(selection = state.selection.selectAll(actualState.results))
            return EventResult.HANDLED
        }

        actualState.tab == SearchTab.SONGS && event.code() == KeyCode.ESCAPE && state.selection.isNotEmpty -> {
            state = state.copy(selection = state.selection.clear())
            return EventResult.HANDLED
        }

        actualState.tab == SearchTab.SONGS && event.matchesAction(
            MeloAction.ADD_PLAYLIST,
            settingsViewState.currentSettings
        ) -> {
            if (state.selection.isNotEmpty) {
                openPlaylistPicker(state.selection.tracks())
            } else {
                val track = actualState.results.getOrNull(actualState.selectedIndex)
                if (track != null) openPlaylistPicker(track)
            }
            return EventResult.HANDLED
        }

        actualState.tab == SearchTab.SONGS && (event.isCharIgnoreCase('m') || event.matchesAction(
            MeloAction.TRACK_OPTIONS,
            settingsViewState.currentSettings
        )) -> {
            if (state.selection.isNotEmpty) {
                openBatchOptions(state.selection.tracks())
                return EventResult.HANDLED
            }
            val track = actualState.results.getOrNull(actualState.selectedIndex)
            if (track != null) openTrackOptions(track)
            return EventResult.HANDLED
        }

        actualState.tab == SearchTab.SONGS && event.isChar('A') -> {
            val track = actualState.results.getOrNull(actualState.selectedIndex)
            if (track != null && track.artist.isNotBlank()) {
                searchInputState.clear()
                searchInputState.insert(track.artist)
                updateScreen<ScreenState.Search> { it.copy(tab = SearchTab.ARTISTS) }
                performSearch()
            }
            return EventResult.HANDLED
        }

        actualState.tab == SearchTab.SONGS && event.isChar('B') -> {
            val track = actualState.results.getOrNull(actualState.selectedIndex)
            if (track != null && track.album.isNotBlank()) {
                searchInputState.clear()
                searchInputState.insert(track.album)
                updateScreen<ScreenState.Search> { it.copy(tab = SearchTab.ALBUMS) }
                performSearch()
            }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handleResultsKey(event: KeyEvent): EventResult {
    // Overlay intercepts keys from any screen
    when (state.playlistInteraction.playlistInputMode) {
        PlaylistInputMode.CREATE,
        PlaylistInputMode.RENAME -> return handlePlaylistInput(event)

        PlaylistInputMode.PICKER -> return handlePlaylistPicker(event)
        PlaylistInputMode.NONE -> {}
    }

    val actualState = state.screen as? ScreenState.Search ?: return handleGlobalShortcuts(event)

    if (event.modifiers().alt()) {
        if (event.code() == KeyCode.RIGHT) {
            switchSearchTab(true)
            return EventResult.HANDLED
        }
        if (event.code() == KeyCode.LEFT) {
            switchSearchTab(false)
            return EventResult.HANDLED
        }
    }

    val isFocused = appRunner()?.focusManager()?.focusedId() == "results-panel"
    if (isFocused) {
        if (event.matches(Actions.MOVE_RIGHT)) {
            switchSearchTab(true)
            return EventResult.HANDLED
        }
        if (event.matches(Actions.MOVE_LEFT)) {
            switchSearchTab(false)
            return EventResult.HANDLED
        }
    }

    when {
        event.isChar('1') -> {
            selectSearchTab(SearchTab.SONGS)
            return EventResult.HANDLED
        }

        event.isChar('2') -> {
            selectSearchTab(SearchTab.ALBUMS)
            return EventResult.HANDLED
        }

        event.isChar('3') -> {
            selectSearchTab(SearchTab.ARTISTS)
            return EventResult.HANDLED
        }

        event.isChar('4') -> {
            selectSearchTab(SearchTab.PLAYLISTS)
            return EventResult.HANDLED
        }

        event.isChar('/') -> {
            appRunner()?.focusManager()?.setFocus("search-bar")
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            appRunner()?.focusManager()?.setFocus("search-bar")
            return EventResult.HANDLED
        }

        event.code() == KeyCode.TAB -> {
            if (actualState.tab == SearchTab.SONGS && state.detail.selectedTrack != null) {
                appRunner()?.focusManager()?.setFocus("detail-panel")
            } else if (actualState.tab != SearchTab.SONGS && state.detail.selectedEntity != null) {
                appRunner()?.focusManager()?.setFocus("desc-area")
            } else {
                appRunner()?.focusManager()?.setFocus("search-bar")
            }
            return EventResult.HANDLED
        }
    }

    val listSize = when (actualState.tab) {
        SearchTab.SONGS -> actualState.results.size
        SearchTab.ALBUMS -> actualState.albumResults.size
        SearchTab.ARTISTS -> actualState.artistResults.size
        SearchTab.PLAYLISTS -> actualState.playlistResults.size
    }

    if (listSize == 0) return handleGlobalShortcuts(event)

    if (event.matches(Actions.MOVE_DOWN) || event.matches(Actions.MOVE_UP) || event.code() == KeyCode.ENTER) {
        return handleResultsMovementKey(actualState, event, isFocused, listSize)
    }

    val actionResult = handleResultsActionKey(actualState, event)
    return if (actionResult == EventResult.HANDLED) actionResult else handleGlobalShortcuts(event)
}