package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SearchTab
import com.github.adriianh.cli.tui.SidebarSection
import com.github.adriianh.cli.tui.handler.NAV_SECTIONS
import com.github.adriianh.core.domain.model.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

private fun MeloScreen.cancelSearchJobs() {
    try {
        suggestionsJob?.cancel()
    } catch (_: Exception) {
    }
    suggestionsJob = null

    try {
        searchJob?.cancel()
    } catch (_: Exception) {
    }
    searchJob = null

    try {
        loadMoreJob?.cancel()
    } catch (_: Exception) {
    }
    loadMoreJob = null
}

/** Coincidencia difusa de un token dentro de un texto (mismo orden de caracteres). */
private fun fuzzyMatch(token: String, text: String): Boolean {
    var i = 0
    for (c in text) {
        if (i < token.length && c == token[i]) i++
    }
    return i == token.length
}

private fun MeloScreen.performOfflineSearch(query: String, currentTab: SearchTab) {
    val queryTokens = query.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }

    val filtered = state.collections.offlineTracks
        .filter { it.downloadStatus == DownloadStatus.COMPLETED }
        .map { it.track }
        .filter { track ->
            val title = track.title.lowercase()
            val artist = track.artist.lowercase()
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
}

private fun MeloScreen.startNetworkSearch(query: String, currentTab: SearchTab) {
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

    searchJob = scope.launch {
        try {
            supervisorScope {
                var searchException: Throwable? = null
                val tracksDeferred = async {
                    runCatching { searchTracks(query) }
                        .onFailure {
                            if (searchException == null && it !is CancellationException) searchException =
                                it
                        }
                        .getOrDefault(emptyList())
                }
                val albumsDeferred = async {
                    runCatching { searchAlbums(query) }
                        .onFailure {
                            if (searchException == null && it !is CancellationException) searchException =
                                it
                        }
                        .getOrDefault(emptyList())
                }
                val artistsDeferred = async {
                    runCatching { searchArtists(query) }
                        .onFailure {
                            if (searchException == null && it !is CancellationException) searchException =
                                it
                        }
                        .getOrDefault(emptyList())
                }
                val playlistsDeferred = async {
                    runCatching { searchPlaylists(query) }
                        .onFailure {
                            if (searchException == null && it !is CancellationException) searchException =
                                it
                        }
                        .getOrDefault(emptyList())
                }

                val tracks = tracksDeferred.await()
                val albums = albumsDeferred.await()
                val artists = artistsDeferred.await()
                val playlists = playlistsDeferred.await()

                if (!isActive) return@supervisorScope

                val allEmpty =
                    tracks.isEmpty() && albums.isEmpty() && artists.isEmpty() && playlists.isEmpty()
                val errorMessage = if (allEmpty && searchException != null) {
                    "Search failed: ${searchException.message ?: "Network error"}"
                } else null

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
                            hasMore = initialHasMore,
                            errorMessage = errorMessage
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
        } catch (e: CancellationException) {
            throw e
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

    cancelSearchJobs()

    val currentTab = (state.screen as? ScreenState.Search)?.tab ?: SearchTab.SONGS

    if (state.isOfflineMode) {
        performOfflineSearch(query, currentTab)
    } else {
        startNetworkSearch(query, currentTab)
    }
}

internal fun MeloScreen.selectSearchTab(tab: SearchTab) {
    val actualState = state.screen as? ScreenState.Search ?: return
    if (actualState.tab == tab) return

    val nextHasMore = when (tab) {
        SearchTab.SONGS -> loadMoreTracks.hasMore(actualState.results.size)
        SearchTab.ALBUMS -> loadMoreAlbums.hasMore(actualState.albumResults.size)
        SearchTab.ARTISTS -> loadMoreArtists.hasMore(actualState.artistResults.size)
        SearchTab.PLAYLISTS -> loadMorePlaylists.hasMore(actualState.playlistResults.size)
    }

    state = state.copy(
        screen = actualState.copy(
            tab = tab,
            selectedIndex = 0,
            hasMore = nextHasMore
        )
    )
    resultList.selected(0)

    val updatedState = state.screen as ScreenState.Search
    if (tab == SearchTab.SONGS) {
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
        val firstEntity = when (tab) {
            SearchTab.ALBUMS -> updatedState.albumResults.firstOrNull()
            SearchTab.ARTISTS -> updatedState.artistResults.firstOrNull()
            SearchTab.PLAYLISTS -> updatedState.playlistResults.firstOrNull()
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

internal fun MeloScreen.switchSearchTab(forward: Boolean) {
    val actualState = state.screen as? ScreenState.Search ?: return
    val entries = SearchTab.entries
    val newOrdinal = if (forward) {
        (actualState.tab.ordinal + 1) % entries.size
    } else {
        (actualState.tab.ordinal - 1 + entries.size) % entries.size
    }
    selectSearchTab(entries[newOrdinal])
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