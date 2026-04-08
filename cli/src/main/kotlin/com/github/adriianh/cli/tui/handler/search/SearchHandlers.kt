package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SidebarSection
import com.github.adriianh.cli.tui.handler.*
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.MeloAction
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal fun MeloScreen.performSearch() {
    val query = searchInputState.text()
    if (query.isBlank()) return
    lastQuery = query
    loadMoreJob?.cancel()
    loadMoreJob = null

    val currentTab = (state.screen as? ScreenState.Search)?.tab ?: com.github.adriianh.cli.tui.SearchTab.SONGS

    if (state.isOfflineMode) {
        val q = query.lowercase()
        val filtered = state.collections.offlineTracks
            .filter { it.downloadStatus == DownloadStatus.COMPLETED }
            .map { it.track }
            .filter { it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) }
            .distinctBy { it.id }

        state = state.copy(
            screen = ScreenState.Search(
                query = query,
                tab = currentTab,
                results = filtered,
                isLoading = false,
                selectedIndex = 0,
                hasMore = false
            ),
            detail = state.detail.copy(selectedTrack = filtered.firstOrNull()),
            navigation = state.navigation.copy(activeSection = SidebarSection.SEARCH)
        )
        sidebarNavList.selected(NAV_SECTIONS.indexOf(SidebarSection.SEARCH))
        resultList.selected(0)
        focusResults()
        return
    }

    state = state.copy(
        screen = ScreenState.Search(query = query, tab = currentTab, isLoading = true, errorMessage = null, hasMore = currentTab == com.github.adriianh.cli.tui.SearchTab.SONGS),
        detail = state.detail.copy(selectedTrack = null),
        navigation = state.navigation.copy(activeSection = SidebarSection.SEARCH)
    )
    sidebarNavList.selected(NAV_SECTIONS.indexOf(SidebarSection.SEARCH))
    scope.launch {
        try {
            when (currentTab) {
                com.github.adriianh.cli.tui.SearchTab.SONGS -> {
                    val results = searchTracks(query)
                    val firstTrack = results.firstOrNull()
                    appRunner()?.runOnRenderThread {
                        updateScreen<ScreenState.Search> {
                            it.copy(
                                results = results, isLoading = false, selectedIndex = 0,
                                hasMore = loadMoreTracks.hasMore(results.size)
                            )
                        }
                        state = state.copy(detail = state.detail.copy(selectedTrack = firstTrack))
                        resultList.selected(0)
                        focusResults()
                    }
                    if (firstTrack != null) loadTrackDetails(firstTrack.id)
                }
                com.github.adriianh.cli.tui.SearchTab.ALBUMS -> {
                    val results = searchAlbums(query)
                    appRunner()?.runOnRenderThread {
                        updateScreen<ScreenState.Search> {
                            it.copy(albumResults = results, isLoading = false, selectedIndex = 0, hasMore = false)
                        }
                        resultList.selected(0)
                        focusResults()
                    }
                }
                com.github.adriianh.cli.tui.SearchTab.ARTISTS -> {
                    val results = searchArtists(query)
                    appRunner()?.runOnRenderThread {
                        updateScreen<ScreenState.Search> {
                            it.copy(artistResults = results, isLoading = false, selectedIndex = 0, hasMore = false)
                        }
                        resultList.selected(0)
                        focusResults()
                    }
                }
                com.github.adriianh.cli.tui.SearchTab.PLAYLISTS -> {
                    val results = searchPlaylists(query)
                    appRunner()?.runOnRenderThread {
                        updateScreen<ScreenState.Search> {
                            it.copy(playlistResults = results, isLoading = false, selectedIndex = 0, hasMore = false)
                        }
                        resultList.selected(0)
                        focusResults()
                    }
                }
            }
        } catch (e: Exception) {
            appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Search> {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Search failed: ${e.message}"
                    )
                }
            }
        }
    }
}

internal fun MeloScreen.loadMore() {
    if (state.isOfflineMode || lastQuery.isBlank()) return
    val currentResults = (state.screen as? ScreenState.Search)?.results ?: return
    val offset = currentResults.size
    updateScreen<ScreenState.Search> { it.copy(isLoadingMore = true) }
    loadMoreJob = scope.launch {
        try {
            val more = loadMoreTracks(lastQuery, offset)
            if (isActive) appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Search> {
                    it.copy(
                        results = it.results + more, isLoadingMore = false,
                        hasMore = loadMoreTracks.hasMore(offset + more.size)
                    )
                }
            }
        } catch (_: Exception) {
            appRunner()?.runOnRenderThread { updateScreen<ScreenState.Search> { it.copy(isLoadingMore = false) } }
        }
    }
}

internal fun MeloScreen.focusResults() {
    appRunner()?.focusManager()?.setFocus("results-panel")
}

internal fun MeloScreen.handleSearchBarKey(event: KeyEvent): EventResult {
    if (event.code() == KeyCode.CHAR && !event.modifiers().ctrl() && !event.modifiers().alt()) {
        val c = event.character()
        if (c >= '\u007F') {
            searchInputState.insert(c)
            return EventResult.HANDLED
        }
    }

    val s = state.screen as? ScreenState.Search ?: return EventResult.UNHANDLED
    if (s.results.isNotEmpty() &&
        (event.matches(Actions.MOVE_DOWN) || event.matches(Actions.MOVE_UP))
    ) {
        return handleResultsKey(event)
    }
    return handleGlobalShortcuts(event)
}

internal fun MeloScreen.handleResultsKey(event: KeyEvent): EventResult {
    // Overlay intercepts keys from any screen
    when (state.playlistInteraction.playlistInputMode) {
        PlaylistInputMode.CREATE,
        PlaylistInputMode.RENAME -> return handlePlaylistInput(event)

        PlaylistInputMode.PICKER -> return handlePlaylistPicker(event)
        PlaylistInputMode.NONE -> {}
    }

    val actualState = state.screen as? ScreenState.Search ?: return EventResult.UNHANDLED

    val isFocused = appRunner()?.focusManager()?.focusedId() == "results-panel"
    if (isFocused) {
        if (event.code() == KeyCode.TAB || event.matches(Actions.MOVE_RIGHT)) {
            val nextTab = com.github.adriianh.cli.tui.SearchTab.entries[(actualState.tab.ordinal + 1) % com.github.adriianh.cli.tui.SearchTab.entries.size]
            state = state.copy(screen = actualState.copy(tab = nextTab, selectedIndex = 0))
            if (actualState.query.isNotBlank()) performSearch()
            return EventResult.HANDLED
        }
        if (event.matches(Actions.MOVE_LEFT)) {
            val prevTab = com.github.adriianh.cli.tui.SearchTab.entries[(actualState.tab.ordinal - 1 + com.github.adriianh.cli.tui.SearchTab.entries.size) % com.github.adriianh.cli.tui.SearchTab.entries.size]
            state = state.copy(screen = actualState.copy(tab = prevTab, selectedIndex = 0))
            if (actualState.query.isNotBlank()) performSearch()
            return EventResult.HANDLED
        }
    }

    val listSize = when (actualState.tab) {
        com.github.adriianh.cli.tui.SearchTab.SONGS -> actualState.results.size
        com.github.adriianh.cli.tui.SearchTab.ALBUMS -> actualState.albumResults.size
        com.github.adriianh.cli.tui.SearchTab.ARTISTS -> actualState.artistResults.size
        com.github.adriianh.cli.tui.SearchTab.PLAYLISTS -> actualState.playlistResults.size
    }

    if (listSize == 0) return EventResult.UNHANDLED

    when {
        event.matches(Actions.MOVE_DOWN) -> {
            if (!isFocused) return EventResult.UNHANDLED
            val newIndex = minOf(listSize - 1, actualState.selectedIndex + 1)
            resultList.selected(newIndex)
            state = state.copy(screen = actualState.copy(selectedIndex = newIndex), player = state.player.copy(marqueeOffset = 0))
            marqueeTick = 0
            if (actualState.tab == com.github.adriianh.cli.tui.SearchTab.SONGS) {
                actualState.results.getOrNull(newIndex)?.let { track ->
                    state = state.copy(detail = state.detail.copy(selectedTrack = track))
                    debouncedLoadDetails(track)
                }
                if (newIndex >= listSize - 5 && !actualState.isLoadingMore && actualState.hasMore) loadMore()
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            if (!isFocused) return EventResult.UNHANDLED
            val newIndex = maxOf(0, actualState.selectedIndex - 1)
            resultList.selected(newIndex)
            state = state.copy(screen = actualState.copy(selectedIndex = newIndex), player = state.player.copy(marqueeOffset = 0))
            marqueeTick = 0
            if (actualState.tab == com.github.adriianh.cli.tui.SearchTab.SONGS) {
                actualState.results.getOrNull(newIndex)?.let { track ->
                    state = state.copy(detail = state.detail.copy(selectedTrack = track))
                    debouncedLoadDetails(track)
                }
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            if (!isFocused || actualState.tab != com.github.adriianh.cli.tui.SearchTab.SONGS) return EventResult.UNHANDLED
            val selected = actualState.results.getOrNull(resultList.selected()) ?: return EventResult.UNHANDLED
            downloadTrack(selected, DownloadType.PREFETCH)
            playTrack(selected)
            return EventResult.HANDLED
        }

        actualState.tab == com.github.adriianh.cli.tui.SearchTab.SONGS && event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
            actualState.results.getOrNull(actualState.selectedIndex)?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        actualState.tab == com.github.adriianh.cli.tui.SearchTab.SONGS && event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
            actualState.results.getOrNull(actualState.selectedIndex)?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        actualState.tab == com.github.adriianh.cli.tui.SearchTab.SONGS && event.matchesAction(MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings) -> {
            val track = actualState.results.getOrNull(actualState.selectedIndex)
            if (track != null) openPlaylistPicker(track)
            return EventResult.HANDLED
        }

        actualState.tab == com.github.adriianh.cli.tui.SearchTab.SONGS && event.matchesAction(MeloAction.LYRICS, settingsViewState.currentSettings) -> {
            loadLyrics()
            return EventResult.HANDLED
        }

        actualState.tab == com.github.adriianh.cli.tui.SearchTab.SONGS && event.code() == KeyCode.CHAR && (event.character() == 'm' || event.character() == 'o') -> {
            val track = actualState.results.getOrNull(actualState.selectedIndex)
            if (track != null) openTrackOptions(track)
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}