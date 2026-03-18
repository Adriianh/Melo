package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.*
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.*

internal fun MeloScreen.performSearch() {
    val query = searchInputState.text()
    if (query.isBlank()) return
    lastQuery = query
    loadMoreJob?.cancel()
    loadMoreJob = null

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
        screen = ScreenState.Search(isLoading = true, errorMessage = null, hasMore = true), 
        detail = state.detail.copy(selectedTrack = null),
        navigation = state.navigation.copy(activeSection = SidebarSection.SEARCH)
    )
    sidebarNavList.selected(NAV_SECTIONS.indexOf(SidebarSection.SEARCH))
    scope.launch {
        try {
            val results = searchTracks(query)
            val firstTrack = results.firstOrNull()
            appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Search> { it.copy(results = results, isLoading = false, selectedIndex = 0,
                    hasMore = loadMoreTracks.hasMore(results.size)) }
                state = state.copy(detail = state.detail.copy(selectedTrack = firstTrack))
                resultList.selected(0)
                focusResults()
            }
            if (firstTrack != null) loadTrackDetails(firstTrack.id)
        } catch (e: Exception) {
            appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Search> { it.copy(isLoading = false, errorMessage = "Search failed: ${e.message}") }
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
                updateScreen<ScreenState.Search> { it.copy(results = it.results + more, isLoadingMore = false,
                    hasMore = loadMoreTracks.hasMore(offset + more.size)) }
            }
        } catch (_: Exception) {
            appRunner()?.runOnRenderThread { updateScreen<ScreenState.Search> { it.copy(isLoadingMore = false) } }
        }
    }
}

internal fun MeloScreen.debouncedLoadDetails(track: Track) {
    detailsJob?.cancel()
    detailsJob = scope.launch {
        delay(150)
        if (isActive) loadTrackDetails(track.id, track)
    }
}

internal fun MeloScreen.loadTrackDetails(trackId: String, knownTrack: Track? = null) {
    detailsJob?.cancel()
    state = state.copy(detail = state.detail.copy(lyrics = null, isLoadingLyrics = false, similarTracks = emptyList(), isLoadingSimilar = true, artworkData = null))
    if (state.isOfflineMode) {
        state = state.copy(detail = state.detail.copy(isLoadingSimilar = false))
        return
    }
    detailsJob = scope.launch {
        supervisorScope {
            val fullTrackDeferred = async { 
                try {
                    getTrack(trackId)
                } catch (_: Exception) { null }
            }
            
            val similarDeferred = async {
                try {
                    val track = knownTrack ?: fullTrackDeferred.await() ?: return@async emptyList<Track>()
                    resolveSimilarTracks(track, limit = 10)
                } catch (_: Exception) { emptyList() }
            }

            val fullTrack = fullTrackDeferred.await() ?: knownTrack ?: run {
                appRunner()?.runOnRenderThread { state = state.copy(detail = state.detail.copy(isLoadingSimilar = false)) }
                return@supervisorScope
            }

            var artworkUrl = fullTrack.artworkUrl
            if (artworkUrl.isNullOrBlank()) {
                artworkUrl = artworkProvider.resolveArtwork(fullTrack.title, fullTrack.artist)
            }

            val artworkData = artworkUrl?.let { 
                try {
                    artworkRenderer.load(it)
                } catch (_: Exception) { null }
            }

            if (isActive) {
                appRunner()?.runOnRenderThread {
                    state = state.copy(detail = state.detail.copy(selectedTrack = fullTrack, artworkData = artworkData))
                }
                val similar = similarDeferred.await()
                if (isActive) appRunner()?.runOnRenderThread {
                    state = state.copy(detail = state.detail.copy(similarTracks = similar, isLoadingSimilar = false))
                }
            }
        }
    }
}

internal fun MeloScreen.loadNowPlayingMetadata(track: Track) {
    nowPlayingMetadataJob?.cancel()
    if (state.isOfflineMode) return
    nowPlayingMetadataJob = scope.launch {
        var artworkUrl = track.artworkUrl ?: getTrack(track.id)?.artworkUrl
        if (artworkUrl.isNullOrBlank()) {
            artworkUrl = artworkProvider.resolveArtwork(track.title, track.artist)
        }
        val artwork = artworkUrl?.let { artworkRenderer.load(it) }
        if (isActive) appRunner()?.runOnRenderThread {
            if (state.player.nowPlaying?.id == track.id) {
                state = state.copy(player = state.player.copy(nowPlayingArtwork = artwork))
            }
        }
    }
}

internal fun MeloScreen.loadLyrics() {
    val track = state.detail.selectedTrack ?: return
    if (state.isOfflineMode) {
        state = state.copy(detail = state.detail.copy(lyrics = "Lyrics are unavailable in Offline Mode", isLoadingLyrics = false))
        return
    }
    state = state.copy(detail = state.detail.copy(isLoadingLyrics = true, lyrics = null))
    scope.launch {
        val lyrics = getLyrics(track.artist, track.title)
        appRunner()?.runOnRenderThread {
            state = state.copy(detail = state.detail.copy(lyrics = lyrics ?: "Lyrics not found", isLoadingLyrics = false))
        }
    }
}

internal fun MeloScreen.focusResults() {
    appRunner()?.focusManager()?.setFocus("results-panel")
}
