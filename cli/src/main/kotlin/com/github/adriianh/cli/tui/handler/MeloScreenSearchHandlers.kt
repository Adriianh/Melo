package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.*
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.*

internal fun MeloScreen.performSearch() {
    val query = searchInputState.text()
    if (query.isBlank()) return
    lastQuery = query
    loadMoreJob?.cancel()
    loadMoreJob = null
    state = state.copy(search = state.search.copy(isLoading = true, errorMessage = null, hasMore = true), 
        detail = state.detail.copy(selectedTrack = null),
        activeSection = SidebarSection.SEARCH)
    sidebarNavList.selected(NAV_SECTIONS.indexOf(SidebarSection.SEARCH))
    scope.launch {
        try {
            val results = searchTracks(query)
            val firstTrack = results.firstOrNull()
            appRunner()?.runOnRenderThread {
                state = state.copy(search = state.search.copy(results = results, isLoading = false, selectedIndex = 0,
                    hasMore = loadMoreTracks.hasMore(results.size)), 
                    detail = state.detail.copy(selectedTrack = firstTrack))
                resultList.selected(0)
                focusResults()
            }
            if (firstTrack != null) loadTrackDetails(firstTrack.id)
        } catch (e: Exception) {
            appRunner()?.runOnRenderThread {
                state = state.copy(search = state.search.copy(isLoading = false, errorMessage = "Search failed: ${e.message}"))
            }
        }
    }
}

internal fun MeloScreen.loadMore() {
    if (lastQuery.isBlank()) return
    loadMoreJob?.cancel()
    val offset = state.search.results.size
    state = state.copy(search = state.search.copy(isLoadingMore = true))
    loadMoreJob = scope.launch {
        try {
            val more = loadMoreTracks(lastQuery, offset)
            if (isActive) appRunner()?.runOnRenderThread {
                state = state.copy(search = state.search.copy(results = state.search.results + more, isLoadingMore = false,
                    hasMore = loadMoreTracks.hasMore(offset + more.size)))
            }
        } catch (_: Exception) {
            appRunner()?.runOnRenderThread { state = state.copy(search = state.search.copy(isLoadingMore = false)) }
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
    detailsJob = scope.launch {
        val fullTrackDeferred = async { getTrack(trackId) }
        val similarDeferred = async {
            val track = knownTrack ?: fullTrackDeferred.await() ?: return@async emptyList<Track>()
            resolveSimilarTracks(track, limit = 10)
        }
        val fullTrack = fullTrackDeferred.await() ?: knownTrack ?: run {
            appRunner()?.runOnRenderThread { state = state.copy(detail = state.detail.copy(isLoadingSimilar = false)) }
            return@launch
        }
        var artworkUrl = fullTrack.artworkUrl
        if (artworkUrl.isNullOrBlank()) {
            artworkUrl = artworkProvider.resolveArtwork(fullTrack.title, fullTrack.artist)
        }

        val artworkData = artworkUrl?.let { artworkRenderer.load(it) }
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

internal fun MeloScreen.loadNowPlayingMetadata(track: Track) {
    nowPlayingMetadataJob?.cancel()
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
