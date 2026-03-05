package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.*
import com.github.adriianh.cli.tui.util.ArtworkRenderer
import com.github.adriianh.core.domain.model.SimilarTrack
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.*

internal fun MeloScreen.performSearch() {
    val query = searchInputState.text()
    if (query.isBlank()) return
    lastQuery = query
    loadMoreJob?.cancel()
    loadMoreJob = null
    state = state.copy(isLoading = true, errorMessage = null, selectedTrack = null,
        hasMore = true, activeSection = SidebarSection.SEARCH)
    sidebarList.selected(SidebarSection.SEARCH.ordinal)
    scope.launch {
        try {
            val results = searchTracks(query)
            val firstTrack = results.firstOrNull()
            appRunner()?.runOnRenderThread {
                state = state.copy(results = results, isLoading = false, selectedIndex = 0,
                    selectedTrack = firstTrack, hasMore = loadMoreTracks.hasMore(results.size))
                resultList.selected(0)
                focusResults()
            }
            if (firstTrack != null) loadTrackDetails(firstTrack.id)
        } catch (e: Exception) {
            appRunner()?.runOnRenderThread {
                state = state.copy(isLoading = false, errorMessage = "Search failed: ${e.message}")
            }
        }
    }
}

internal fun MeloScreen.loadMore() {
    if (lastQuery.isBlank()) return
    loadMoreJob?.cancel()
    val offset = state.results.size
    state = state.copy(isLoadingMore = true)
    loadMoreJob = scope.launch {
        try {
            val more = loadMoreTracks(lastQuery, offset)
            if (isActive) appRunner()?.runOnRenderThread {
                state = state.copy(results = state.results + more, isLoadingMore = false,
                    hasMore = loadMoreTracks.hasMore(offset + more.size))
            }
        } catch (_: Exception) {
            appRunner()?.runOnRenderThread { state = state.copy(isLoadingMore = false) }
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
    state = state.copy(lyrics = null, isLoadingLyrics = false, similarTracks = emptyList(), artworkData = null)
    detailsJob = scope.launch {
        val fullTrackDeferred = async { getTrack(trackId) }
        val similarDeferred = async {
            val artist = knownTrack?.artist ?: fullTrackDeferred.await()?.artist ?: return@async emptyList<SimilarTrack>()
            val title  = knownTrack?.title  ?: fullTrackDeferred.await()?.title  ?: return@async emptyList<SimilarTrack>()
            getSimilarTracks(artist, title)
        }
        val fullTrack = fullTrackDeferred.await() ?: knownTrack ?: return@launch
        val artworkData = fullTrack.artworkUrl?.let { ArtworkRenderer.load(it) }
        if (isActive) {
            appRunner()?.runOnRenderThread { state = state.copy(selectedTrack = fullTrack, artworkData = artworkData) }
            val similar = similarDeferred.await()
            if (isActive) appRunner()?.runOnRenderThread { state = state.copy(similarTracks = similar) }
        }
    }
}

internal fun MeloScreen.loadLyrics() {
    val track = state.selectedTrack ?: return
    state = state.copy(isLoadingLyrics = true, lyrics = null)
    scope.launch {
        val lyrics = getLyrics(track.artist, track.title)
        appRunner()?.runOnRenderThread {
            state = state.copy(lyrics = lyrics ?: "Lyrics not found", isLoadingLyrics = false)
        }
    }
}

internal fun MeloScreen.focusResults() {
    appRunner()?.focusManager()?.setFocus("results-panel")
}


