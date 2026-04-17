package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SearchTab
import com.github.adriianh.cli.tui.SidebarSection
import com.github.adriianh.cli.tui.handler.NAV_SECTIONS
import com.github.adriianh.cli.tui.handler.handleGlobalShortcuts
import com.github.adriianh.cli.tui.handler.handlePlaylistInput
import com.github.adriianh.cli.tui.handler.handlePlaylistPicker
import com.github.adriianh.cli.tui.handler.matchesAction
import com.github.adriianh.cli.tui.handler.openPlaylistPicker
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.cli.tui.handler.toggleFavorite
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.MeloAction
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal fun MeloScreen.handleSearchQueryChange(query: String) {
    if (query.isBlank()) {
        val currentScreen = state.screen
        if (currentScreen !is ScreenState.Search) {
            state = state.copy(
                screen = ScreenState.Search(
                    query = query,
                    isShowingSuggestions = true
                ),
                navigation = state.navigation.copy(activeSection = SidebarSection.SEARCH)
            )
            sidebarNavList.selected(NAV_SECTIONS.indexOf(SidebarSection.SEARCH))
        } else {
            updateScreen<ScreenState.Search> {
                it.copy(query = query, isShowingSuggestions = true, selectedSuggestionIndex = null)
            }
        }
        scope.launch {
            try {
                val rawSuggestions =
                    searchInteractors.getSearchHistory(query, limit = 10).firstOrNull()
                        ?: emptyList()
                if (isActive) {
                    appRunner()?.runOnRenderThread {
                        val s = state.screen as? ScreenState.Search ?: return@runOnRenderThread
                        if (s.query.isBlank()) {
                            val visualLocal = rawSuggestions.map { "${MeloTheme.ICON_HISTORY} $it" }
                            updateScreen<ScreenState.Search> {
                                it.copy(searchSuggestions = visualLocal)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        return
    }

    val currentScreen = state.screen
    if (currentScreen !is ScreenState.Search) {
        state = state.copy(
            screen = ScreenState.Search(
                query = query,
                isShowingSuggestions = true
            ),
            navigation = state.navigation.copy(activeSection = SidebarSection.SEARCH)
        )
        sidebarNavList.selected(NAV_SECTIONS.indexOf(SidebarSection.SEARCH))
    } else {
        updateScreen<ScreenState.Search> {
            it.copy(query = query, isShowingSuggestions = true, selectedSuggestionIndex = null)
        }
    }

    scope.launch {
        try {
            val localHistory = searchInteractors.getSearchHistory(query, limit = 5)
                .firstOrNull() ?: emptyList()

            if (isActive) {
                appRunner()?.runOnRenderThread {
                    val s = state.screen as? ScreenState.Search ?: return@runOnRenderThread
                    if (s.query == query) {
                        val visualLocal = localHistory.map { "${MeloTheme.ICON_HISTORY} $it" }
                        updateScreen<ScreenState.Search> {
                            it.copy(searchSuggestions = visualLocal.ifEmpty {
                                listOf("Loading network suggestions...")
                            })
                        }
                    }
                }
            }

            val networkSuggestions = try {
                searchInteractors.getSearchSuggestions(query)
            } catch (_: Exception) {
                emptyList()
            }

            if (isActive) {
                appRunner()?.runOnRenderThread {
                    val s = state.screen as? ScreenState.Search ?: return@runOnRenderThread
                    if (s.query == query) {
                        val visualLocal = localHistory.map { "${MeloTheme.ICON_HISTORY} $it" }
                        val filteredNetwork = networkSuggestions
                            .filter { net ->
                                localHistory.none { loc ->
                                    net.equals(loc, ignoreCase = true)
                                }
                            }
                            .take(10 - visualLocal.size)
                            .map { "${MeloTheme.ICON_SEARCH} $it" }

                        val finalSuggestions = (visualLocal + filteredNetwork)
                            .ifEmpty { listOf("No recent queries for '$query'") }

                        updateScreen<ScreenState.Search> {
                            it.copy(searchSuggestions = finalSuggestions)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}

internal fun MeloScreen.performSearch() {
    val searchState = state.screen as? ScreenState.Search
    val query =
        if (searchState?.selectedSuggestionIndex != null && searchState.searchSuggestions.isNotEmpty()) {
            val suggestion = searchState.searchSuggestions[searchState.selectedSuggestionIndex]
                .removePrefix("${MeloTheme.ICON_HISTORY} ")
                .removePrefix("${MeloTheme.ICON_SEARCH} ")
            searchInputState.clear()
            for (c in suggestion) searchInputState.insert(c)
            suggestion
        } else {
            searchInputState.text()
        }

    lastObservedSearchQuery = query

    if (query.isBlank()) return
    lastQuery = query
    scope.launch { searchInteractors.saveSearchQuery(query) }

    try {
        loadMoreJob?.cancel()
    } catch (_: Exception) {
    }
    loadMoreJob = null

    val currentTab = (state.screen as? ScreenState.Search)?.tab ?: SearchTab.SONGS

    if (state.isOfflineMode) {
        val queryTokens = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }

        fun fuzzyMatch(token: String, text: String): Boolean {
            var i = 0
            for (c in text) {
                if (i < token.length && c == token[i]) i++
            }
            return i == token.length
        }

        val filtered = state.collections.offlineTracks
            .filter { it.downloadStatus == DownloadStatus.COMPLETED }
            .map { it.track }
            .filter { track ->
                val title = track.title.lowercase()
                val artist = track.artist.lowercase()
                // Require all query tokens to match somewhere (exactly or fuzzy)
                queryTokens.all { token ->
                    token in title || token in artist || fuzzyMatch(token, title) || fuzzyMatch(
                        token,
                        artist
                    )
                }
            }
            .distinctBy { it.id }

        state = state.copy(
            screen = ScreenState.Search(
                query = query,
                tab = currentTab,
                results = filtered,
                searchSuggestions = emptyList(),
                isShowingSuggestions = false,
                selectedSuggestionIndex = null,
                albumResults = emptyList(),
                artistResults = emptyList(),
                playlistResults = emptyList(),
                isLoading = false,
                selectedIndex = 0,
                hasMore = false
            ),
            detail = state.detail.copy(
                selectedTrack = filtered.firstOrNull(),
                detailTab = DetailTab.INFO
            ),
            navigation = state.navigation.copy(activeSection = SidebarSection.SEARCH)
        )
        sidebarNavList.selected(NAV_SECTIONS.indexOf(SidebarSection.SEARCH))
        resultList.selected(0)
        focusResults()
        return
    }

    state = state.copy(
        screen = ScreenState.Search(
            query = query,
            tab = currentTab,
            isLoading = true,
            errorMessage = null,
            hasMore = false,
            searchSuggestions = emptyList(),
            isShowingSuggestions = false,
            selectedSuggestionIndex = null
        ),
        detail = state.detail.copy(
            selectedTrack = null,
            selectedEntity = null,
            artworkData = null,
            detailTab = DetailTab.INFO
        ),
        navigation = state.navigation.copy(activeSection = SidebarSection.SEARCH)
    )
    sidebarNavList.selected(NAV_SECTIONS.indexOf(SidebarSection.SEARCH))

    scope.launch {
        try {
            coroutineScope {
                val tracksDeferred = async { searchTracks(query) }
                val albumsDeferred = async { searchAlbums(query) }
                val artistsDeferred = async { searchArtists(query) }
                val playlistsDeferred = async { searchPlaylists(query) }

                val tracks = tracksDeferred.await()
                val albums = albumsDeferred.await()
                val artists = artistsDeferred.await()
                val playlists = playlistsDeferred.await()

                if (!isActive) return@coroutineScope

                val initialHasMore = when (currentTab) {
                    SearchTab.SONGS -> loadMoreTracks.hasMore(tracks.size)
                    SearchTab.ALBUMS -> loadMoreAlbums.hasMore(albums.size)
                    SearchTab.ARTISTS -> loadMoreArtists.hasMore(artists.size)
                    SearchTab.PLAYLISTS -> loadMorePlaylists.hasMore(playlists.size)
                }

                appRunner()?.runOnRenderThread {
                    updateScreen<ScreenState.Search> {
                        it.copy(
                            results = tracks,
                            albumResults = albums,
                            artistResults = artists,
                            playlistResults = playlists,
                            isLoading = false,
                            selectedIndex = 0,
                            hasMore = initialHasMore
                        )
                    }
                    if (currentTab == SearchTab.SONGS) {
                        state = state.copy(
                            detail = state.detail.copy(selectedTrack = tracks.firstOrNull())
                        )
                    } else {
                        val firstEntity = when (currentTab) {
                            SearchTab.ALBUMS -> albums.firstOrNull()
                            SearchTab.ARTISTS -> artists.firstOrNull()
                            SearchTab.PLAYLISTS -> playlists.firstOrNull()
                        }
                        state = state.copy(detail = state.detail.copy(selectedEntity = firstEntity))
                    }
                    resultList.selected(0)
                    focusResults()
                }

                if (currentTab == SearchTab.SONGS) {
                    tracks.firstOrNull()?.let { loadTrackDetails(it.id) }
                } else {
                    val firstEntity = when (currentTab) {
                        SearchTab.ALBUMS -> albums.firstOrNull()
                        SearchTab.ARTISTS -> artists.firstOrNull()
                        SearchTab.PLAYLISTS -> playlists.firstOrNull()
                    }
                    firstEntity?.let { debouncedLoadEntityDetails(it) }
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

internal fun MeloScreen.switchSearchTab(forward: Boolean) {
    val actualState = state.screen as? ScreenState.Search ?: return
    val entries = SearchTab.entries
    val newOrdinal = if (forward) {
        (actualState.tab.ordinal + 1) % entries.size
    } else {
        (actualState.tab.ordinal - 1 + entries.size) % entries.size
    }

    val nextTab = entries[newOrdinal]
    val nextHasMore = when (nextTab) {
        SearchTab.SONGS -> loadMoreTracks.hasMore(actualState.results.size)
        SearchTab.ALBUMS -> loadMoreAlbums.hasMore(actualState.albumResults.size)
        SearchTab.ARTISTS -> loadMoreArtists.hasMore(actualState.artistResults.size)
        SearchTab.PLAYLISTS -> loadMorePlaylists.hasMore(actualState.playlistResults.size)
    }

    state = state.copy(
        screen = actualState.copy(
            tab = nextTab,
            selectedIndex = 0,
            hasMore = nextHasMore
        )
    )
    resultList.selected(0)

    val updatedState = state.screen as ScreenState.Search
    if (nextTab == SearchTab.SONGS) {
        val firstTrack = updatedState.results.firstOrNull()
        state = state.copy(
            detail = state.detail.copy(
                selectedTrack = firstTrack,
                selectedEntity = null,
                artworkData = null
            )
        )
        if (firstTrack != null) debouncedLoadDetails(firstTrack)
    } else {
        val firstEntity = when (updatedState.tab) {
            SearchTab.ALBUMS -> updatedState.albumResults.firstOrNull()
            SearchTab.ARTISTS -> updatedState.artistResults.firstOrNull()
            SearchTab.PLAYLISTS -> updatedState.playlistResults.firstOrNull()
            else -> null
        }
        state = state.copy(
            detail = state.detail.copy(
                selectedTrack = null,
                selectedEntity = firstEntity,
                artworkData = null
            )
        )
        if (firstEntity != null) debouncedLoadEntityDetails(firstEntity)
    }
}

internal fun MeloScreen.loadMore() {
    if (state.isOfflineMode || lastQuery.isBlank()) return
    val searchState = state.screen as? ScreenState.Search ?: return
    updateScreen<ScreenState.Search> { it.copy(isLoadingMore = true) }
    loadMoreJob = scope.launch {
        try {
            when (searchState.tab) {
                SearchTab.SONGS -> {
                    val offset = searchState.results.size
                    val more = loadMoreTracks(lastQuery, offset)
                    if (isActive) appRunner()?.runOnRenderThread {
                        updateScreen<ScreenState.Search> {
                            it.copy(
                                results = it.results + more, isLoadingMore = false,
                                hasMore = loadMoreTracks.hasMore(offset + more.size)
                            )
                        }
                    }
                }

                SearchTab.ALBUMS -> {
                    val offset = searchState.albumResults.size
                    val more = loadMoreAlbums(lastQuery, offset)
                    if (isActive) appRunner()?.runOnRenderThread {
                        updateScreen<ScreenState.Search> {
                            it.copy(
                                albumResults = it.albumResults + more, isLoadingMore = false,
                                hasMore = loadMoreAlbums.hasMore(offset + more.size)
                            )
                        }
                    }
                }

                SearchTab.ARTISTS -> {
                    val offset = searchState.artistResults.size
                    val more = loadMoreArtists(lastQuery, offset)
                    if (isActive) appRunner()?.runOnRenderThread {
                        updateScreen<ScreenState.Search> {
                            it.copy(
                                artistResults = it.artistResults + more, isLoadingMore = false,
                                hasMore = loadMoreArtists.hasMore(offset + more.size)
                            )
                        }
                    }
                }

                SearchTab.PLAYLISTS -> {
                    val offset = searchState.playlistResults.size
                    val more = loadMorePlaylists(lastQuery, offset)
                    if (isActive) appRunner()?.runOnRenderThread {
                        updateScreen<ScreenState.Search> {
                            it.copy(
                                playlistResults = it.playlistResults + more, isLoadingMore = false,
                                hasMore = loadMorePlaylists.hasMore(offset + more.size)
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            if (isActive) appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Search> {
                    it.copy(isLoadingMore = false)
                }
            }
        }
    }
}

internal fun MeloScreen.focusResults() {
    appRunner()?.focusManager()?.setFocus("results-panel")
}

internal fun MeloScreen.handleSearchBarKey(event: KeyEvent): EventResult {
    val searchState = state.screen as? ScreenState.Search
    if (searchState != null && searchState.isShowingSuggestions && searchState.searchSuggestions.isNotEmpty()) {
        if (event.code() == KeyCode.ESCAPE) {
            updateScreen<ScreenState.Search> { it.copy(isShowingSuggestions = false) }
            return EventResult.HANDLED
        }
        if (event.code() == KeyCode.DOWN) {
            val nextIndex = if (searchState.selectedSuggestionIndex == null) 0 else minOf(
                searchState.searchSuggestions.size - 1,
                searchState.selectedSuggestionIndex + 1
            )
            updateScreen<ScreenState.Search> { it.copy(selectedSuggestionIndex = nextIndex) }
            return EventResult.HANDLED
        }
        if (event.code() == KeyCode.UP) {
            val prevIndex = if (searchState.selectedSuggestionIndex == null) -1 else maxOf(
                -1,
                searchState.selectedSuggestionIndex - 1
            )
            updateScreen<ScreenState.Search> { it.copy(selectedSuggestionIndex = if (prevIndex == -1) null else prevIndex) }
            return EventResult.HANDLED
        }
        if (event.matchesAction(MeloAction.DELETE, settingsViewState.currentSettings)) {
            if (searchState.selectedSuggestionIndex != null) {
                val suggestionToDeleteRaw =
                    searchState.searchSuggestions[searchState.selectedSuggestionIndex]
                val isLocalHistory = suggestionToDeleteRaw.startsWith("${MeloTheme.ICON_HISTORY} ")
                if (isLocalHistory) {
                    val suggestionToDelete =
                        suggestionToDeleteRaw.removePrefix("${MeloTheme.ICON_HISTORY} ")
                    scope.launch {
                        searchInteractors.deleteSearchQuery(suggestionToDelete)
                        val newSuggestions =
                            searchState.searchSuggestions.filterIndexed { index, _ ->
                                index != searchState.selectedSuggestionIndex
                            }
                        val newIndex = if (newSuggestions.isEmpty()) null else minOf(
                            searchState.selectedSuggestionIndex,
                            newSuggestions.size - 1
                        )
                        appRunner()?.runOnRenderThread {
                            updateScreen<ScreenState.Search> {
                                it.copy(
                                    searchSuggestions = newSuggestions,
                                    selectedSuggestionIndex = newIndex,
                                    isShowingSuggestions = newSuggestions.isNotEmpty()
                                )
                            }
                        }
                    }
                    return EventResult.HANDLED
                }
            }
        }
    }

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

    if (event.code() == KeyCode.CHAR && !event.modifiers().ctrl() && !event.modifiers().alt()) {
        val c = event.character()
        if (c >= '\u007F') {
            searchInputState.insert(c)
            return EventResult.HANDLED
        }
    }

    val s = state.screen as? ScreenState.Search ?: return handleGlobalShortcuts(event)
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

    val listSize = when (actualState.tab) {
        SearchTab.SONGS -> actualState.results.size
        SearchTab.ALBUMS -> actualState.albumResults.size
        SearchTab.ARTISTS -> actualState.artistResults.size
        SearchTab.PLAYLISTS -> actualState.playlistResults.size
    }

    if (listSize == 0) return handleGlobalShortcuts(event)

    when {
        event.matches(Actions.MOVE_DOWN) -> {
            if (!isFocused) return handleGlobalShortcuts(event)
            val newIndex = minOf(listSize - 1, actualState.selectedIndex + 1)
            resultList.selected(newIndex)
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
                if (newIndex >= listSize - 5 && !actualState.isLoadingMore && actualState.hasMore) loadMore()
            } else {
                val entity = when (actualState.tab) {
                    SearchTab.ALBUMS -> actualState.albumResults.getOrNull(newIndex)
                    SearchTab.ARTISTS -> actualState.artistResults.getOrNull(newIndex)
                    SearchTab.PLAYLISTS -> actualState.playlistResults.getOrNull(newIndex)
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
                if (newIndex >= listSize - 5 && !actualState.isLoadingMore && actualState.hasMore) loadMore()
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            if (!isFocused) return handleGlobalShortcuts(event)
            val newIndex = maxOf(0, actualState.selectedIndex - 1)
            resultList.selected(newIndex)
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

        actualState.tab == SearchTab.SONGS && event.matchesAction(
            MeloAction.FAVORITE,
            settingsViewState.currentSettings
        ) -> {
            actualState.results.getOrNull(actualState.selectedIndex)?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        actualState.tab == SearchTab.SONGS && event.matchesAction(
            MeloAction.ADD_TO_QUEUE,
            settingsViewState.currentSettings
        ) -> {
            actualState.results.getOrNull(actualState.selectedIndex)?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        actualState.tab == SearchTab.SONGS && event.matchesAction(
            MeloAction.ADD_PLAYLIST,
            settingsViewState.currentSettings
        ) -> {
            val track = actualState.results.getOrNull(actualState.selectedIndex)
            if (track != null) openPlaylistPicker(track)
            return EventResult.HANDLED
        }

        actualState.tab == SearchTab.SONGS && event.matchesAction(
            MeloAction.LYRICS,
            settingsViewState.currentSettings
        ) -> {
            loadLyrics()
            return EventResult.HANDLED
        }

        actualState.tab == SearchTab.SONGS && event.code() == KeyCode.CHAR && (event.character() == 'm' || event.character() == 'o') -> {
            val track = actualState.results.getOrNull(actualState.selectedIndex)
            if (track != null) openTrackOptions(track)
            return EventResult.HANDLED
        }

        actualState.tab == SearchTab.SONGS && event.code() == KeyCode.CHAR && event.character() == 'A' -> {
            val track = actualState.results.getOrNull(actualState.selectedIndex)
            if (track != null && track.artist.isNotBlank()) {
                searchInputState.clear()
                for (c in track.artist) searchInputState.insert(c)
                updateScreen<ScreenState.Search> { it.copy(tab = SearchTab.ARTISTS) }
                performSearch()
            }
            return EventResult.HANDLED
        }

        actualState.tab == SearchTab.SONGS && event.code() == KeyCode.CHAR && event.character() == 'B' -> {
            val track = actualState.results.getOrNull(actualState.selectedIndex)
            if (track != null && track.album.isNotBlank()) {
                searchInputState.clear()
                for (c in track.album) searchInputState.insert(c)
                updateScreen<ScreenState.Search> { it.copy(tab = SearchTab.ALBUMS) }
                performSearch()
            }
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}