package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.handler.loadMoreSimilar
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.cli.tui.handler.resolveSimilarTracks
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.*

internal fun MeloScreen.debouncedLoadDetails(track: Track) {
    detailsJob?.cancel()
    detailsJob = scope.launch {
        delay(150)
        if (isActive) loadTrackDetails(track.id, track)
    }
}

internal fun MeloScreen.debouncedLoadEntityDetails(entity: SearchResult) {
    detailsJob?.cancel()
    detailsJob = scope.launch {
        delay(150)
        if (isActive) loadEntityDetails(entity)
    }
}

internal fun MeloScreen.loadEntityDetails(entity: SearchResult) {
    detailsJob?.cancel()
    state = state.copy(
        detail = state.detail.copy(
            artworkData = null,
            isLoadingEntityMeta = true,
            entityGenres = emptyList()
        )
    )
    if (state.isOfflineMode) {
        state = state.copy(detail = state.detail.copy(isLoadingEntityMeta = false))
        return
    }

    val url = when (entity) {
        is SearchResult.Album -> entity.artworkUrl
        is SearchResult.Artist -> entity.artworkUrl
        is SearchResult.Playlist -> entity.artworkUrl
        is SearchResult.Song -> return
    }

    detailsJob = scope.launch {
        val artworkDeferred = async {
            url?.let {
                try {
                    artworkRenderer.load(it.replace("w120-h120", "w512-h512")) // Request higher res if possible
                } catch (_: Exception) {
                    null
                }
            }
        }

        val detailsDeferred = async {
            try {
                getEntityDetails(entity)
            } catch (_: Exception) {
                entity
            }
        }

        val tagsDeferred = async {
            if (entity is SearchResult.Artist) {
                try {
                    getArtistTags(entity.name)
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

        val artworkData = artworkDeferred.await()
        val detailedEntity = detailsDeferred.await()
        val genres = tagsDeferred.await()

        if (isActive) {
            appRunner()?.runOnRenderThread {
                state = state.copy(
                    detail = state.detail.copy(
                        selectedEntity = detailedEntity,
                        artworkData = artworkData,
                        entityGenres = genres,
                        isLoadingEntityMeta = false
                    )
                )
            }
        }
    }
}

internal fun MeloScreen.loadTrackDetails(trackId: String, knownTrack: Track? = null) {
    detailsJob?.cancel()
    state = state.copy(
        detail = state.detail.copy(
            lyrics = null,
            isLoadingLyrics = false,
            similarTracks = emptyList(),
            isLoadingSimilar = true,
            artworkData = null
        )
    )
    if (state.isOfflineMode) {
        state = state.copy(detail = state.detail.copy(isLoadingSimilar = false))
        return
    }
    detailsJob = scope.launch {
        supervisorScope {
            val fullTrackDeferred = async {
                try {
                    getTrack(trackId)
                } catch (_: Exception) {
                    null
                }
            }

            val similarDeferred = async {
                try {
                    val track = knownTrack ?: fullTrackDeferred.await() ?: return@async emptyList<Track>()
                    resolveSimilarTracks(track, limit = 10)
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val fullTrack = fullTrackDeferred.await() ?: knownTrack ?: run {
                appRunner()?.runOnRenderThread {
                    state = state.copy(detail = state.detail.copy(isLoadingSimilar = false))
                }
                return@supervisorScope
            }

            var artworkUrl = fullTrack.artworkUrl
            var album = fullTrack.album
            
            if (artworkUrl.isNullOrBlank() || album.isBlank()) {
                val resolved = metadataProvider.resolveMetadata(fullTrack.title, fullTrack.artist)
                if (artworkUrl.isNullOrBlank()) artworkUrl = resolved?.artworkUrl
                if (album.isBlank()) album = resolved?.album ?: ""
            }
            
            val updatedTrack = fullTrack.copy(artworkUrl = artworkUrl, album = album)

            val artworkData = artworkUrl?.let {
                try {
                    artworkRenderer.load(it)
                } catch (_: Exception) {
                    null
                }
            }

            if (isActive) {
                appRunner()?.runOnRenderThread {
                    state = state.copy(detail = state.detail.copy(selectedTrack = updatedTrack, artworkData = artworkData))
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
        val resolvedMetadata = if (track.artworkUrl == null || track.album.isBlank()) {
            metadataProvider.resolveMetadata(track.title, track.artist)
        } else null

        val artworkUrl = resolvedMetadata?.artworkUrl ?: track.artworkUrl
        val album = resolvedMetadata?.album ?: track.album
        val artwork = artworkUrl?.let { artworkRenderer.load(it) }

        if (isActive) appRunner()?.runOnRenderThread {
            if (state.player.nowPlaying?.id == track.id) {
                state = state.copy(
                    player = state.player.copy(
                        nowPlaying = state.player.nowPlaying?.copy(
                            artworkUrl = artworkUrl,
                            album = album
                        ),
                        nowPlayingArtwork = artwork
                    )
                )

                if (settingsViewState.currentSettings.discordRpcEnabled) {
                    discordRpcManager.updateActivity(state.player.nowPlaying, state.player.isPlaying)
                }
            }
        }
    }
}

internal fun MeloScreen.loadLyrics() {
    val track = state.detail.selectedTrack ?: return
    if (state.isOfflineMode) {
        state = state.copy(
            detail = state.detail.copy(
                lyrics = "Lyrics are unavailable in Offline Mode",
                isLoadingLyrics = false
            )
        )
        return
    }
    state = state.copy(detail = state.detail.copy(isLoadingLyrics = true, lyrics = null))
    scope.launch {
        val lyrics = getLyrics(track.artist, track.title)
        appRunner()?.runOnRenderThread {
            state =
                state.copy(detail = state.detail.copy(lyrics = lyrics ?: "Lyrics not found", isLoadingLyrics = false))
        }
    }
}

internal fun MeloScreen.handleDetailKey(event: KeyEvent): EventResult {
    when {
        event.code() == KeyCode.CHAR && event.character() == '1' -> {
            state = state.copy(detail = state.detail.copy(detailTab = DetailTab.INFO))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == '2' -> {
            state = state.copy(detail = state.detail.copy(detailTab = DetailTab.LYRICS))
            if (state.detail.lyrics == null && !state.detail.isLoadingLyrics) loadLyrics()
            appRunner()?.focusManager()?.setFocus("lyrics-area")
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == '3' -> {
            state = state.copy(detail = state.detail.copy(detailTab = DetailTab.SIMILAR))
            appRunner()?.focusManager()?.setFocus("similar-area")
            return EventResult.HANDLED
        }

        event.matches(Actions.SELECT) && state.detail.detailTab == DetailTab.LYRICS -> {
            if (state.detail.lyrics == null) loadLyrics()
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) && state.detail.detailTab == DetailTab.SIMILAR -> {
            val maxIndex = (state.detail.similarTracks.size - 1).coerceAtLeast(0)
            val newCursor = minOf(maxIndex, state.detail.similarCursor + 1)
            state = state.copy(detail = state.detail.copy(similarCursor = newCursor))
            if (newCursor >= state.detail.similarTracks.size - 3 && state.detail.hasMoreSimilar && !state.detail.isLoadingMoreSimilar) {
                loadMoreSimilar()
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) && state.detail.detailTab == DetailTab.SIMILAR -> {
            state = state.copy(detail = state.detail.copy(similarCursor = maxOf(0, state.detail.similarCursor - 1)))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER && state.detail.detailTab == DetailTab.SIMILAR -> {
            state.detail.similarTracks.getOrNull(state.detail.similarCursor)?.let { playTrack(it) }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}