package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.handleGlobalShortcuts
import com.github.adriianh.cli.tui.handler.isCtrlA
import com.github.adriianh.cli.tui.handler.isCtrlF
import com.github.adriianh.cli.tui.handler.loadMoreSimilar
import com.github.adriianh.cli.tui.handler.matchesAction
import com.github.adriianh.cli.tui.handler.openPlaylistPicker
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openBatchOptions
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playList
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.cli.tui.handler.playback.seekToMs
import com.github.adriianh.cli.tui.handler.resolveSimilarTracks
import com.github.adriianh.cli.tui.handler.toggleFavorite
import com.github.adriianh.cli.tui.isFavoriteEntity
import com.github.adriianh.cli.tui.util.LrcParser
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.FavoriteEntityType
import com.github.adriianh.core.domain.model.LyricsTranslationMode
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.core.domain.model.filterAndSortTracks
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.model.toFavoriteEntity
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.time.Duration.Companion.milliseconds


private fun mergeEntityDetails(original: SearchResult, loaded: SearchResult): SearchResult {
    return when (original) {
        is SearchResult.Album if loaded is SearchResult.Album -> {
            loaded.copy(
                title = loaded.title.ifBlank { original.title },
                author = loaded.author.ifBlank { original.author },
                year = loaded.year ?: original.year,
                artworkUrl = loaded.artworkUrl ?: original.artworkUrl
            )
        }

        is SearchResult.Playlist if loaded is SearchResult.Playlist -> {
            loaded.copy(
                title = loaded.title.ifBlank { original.title },
                author = loaded.author.ifBlank { original.author },
                trackCount = loaded.trackCount ?: original.trackCount,
                artworkUrl = loaded.artworkUrl ?: original.artworkUrl
            )
        }

        is SearchResult.Artist if loaded is SearchResult.Artist -> {
            loaded.copy(
                name = loaded.name.ifBlank { original.name },
                artworkUrl = loaded.artworkUrl ?: original.artworkUrl
            )
        }

        else -> loaded
    }
}

internal fun MeloScreen.debouncedLoadDetails(track: Track) {
    try {
        detailsJob?.cancel()
    } catch (_: Exception) {
    }
    detailsJob = scope.launch {
        delay(150.milliseconds)
        if (isActive) loadTrackDetails(track.id, track)
    }
}

internal fun MeloScreen.debouncedLoadEntityDetails(entity: SearchResult) {
    try {
        detailsJob?.cancel()
    } catch (_: Exception) {
    }
    detailsJob = scope.launch {
        delay(150.milliseconds)
        if (isActive) loadEntityDetails(entity)
    }
}

internal fun MeloScreen.openEntityDetails(entity: SearchResult) {
    val returnScreen = state.screen
    val returnSection = state.navigation.activeSection
    val title = when (entity) {
        is SearchResult.Album -> entity.title
        is SearchResult.Artist -> entity.name
        is SearchResult.Playlist -> entity.title
        is SearchResult.Song -> entity.track.title
    }
    val subtitle = when (entity) {
        is SearchResult.Album -> entity.author
        is SearchResult.Playlist -> entity.author
        is SearchResult.Artist -> null
        is SearchResult.Song -> entity.track.artist
    }
    val description = when (entity) {
        is SearchResult.Album -> entity.description
        is SearchResult.Playlist -> entity.description
        is SearchResult.Artist -> entity.description
        is SearchResult.Song -> null
    }

    state = state.copy(
        detail = state.detail.copy(
            selectedEntity = entity,
            selectedTrack = null,
            isLoadingEntityMeta = true,
            artworkData = null
        ),
        screen = ScreenState.EntityDetail(
            entity = entity,
            title = title,
            subtitle = subtitle,
            description = description,
            tracks = emptyList(),
            artistDashboardItems = emptyList(),
            selectedIndex = 0,
            isLoading = true,
            errorMessage = null,
            returnScreen = returnScreen,
            returnSection = returnSection
        )
    )

    if (entity !is SearchResult.Artist) {
        entityTracksList.selected(0)
        appRunner()?.focusManager()?.setFocus("entity-tracks-list")
    } else {
        artistDashboardList.selected(0)
        appRunner()?.focusManager()?.setFocus("artist-dashboard-list")
    }

    debouncedLoadEntityDetails(entity)

    scope.launch {
        val loaded = try {
            getEntityDetails(entity)
        } catch (e: Exception) {
            if (isActive) appRunner()?.runOnRenderThread {
                val cur = state.screen as? ScreenState.EntityDetail ?: return@runOnRenderThread
                state = state.copy(
                    screen = cur.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load details"
                    )
                )
            }
            return@launch
        }

        val tracks = when (loaded) {
            is SearchResult.Album -> loaded.songs.orEmpty()
            is SearchResult.Playlist -> loaded.songs.orEmpty()
            else -> emptyList()
        }

        val dashboardItems = mutableListOf<Any>()
        if (loaded is SearchResult.Artist) {
            val sections = buildList {
                addAll(loaded.sections)
            }
            val chunkedSections = sections.filter { it.items.isNotEmpty() }.chunked(2)
            chunkedSections.forEach { chunk ->
                if (chunk.size == 2) {
                    dashboardItems.add(Pair(chunk[0], chunk[1]))
                } else {
                    dashboardItems.add(Pair(chunk[0], null))
                }
            }
        }

        if (isActive) appRunner()?.runOnRenderThread {
            val actualScreen = state.screen as? ScreenState.EntityDetail ?: return@runOnRenderThread
            val firstTrack = tracks.firstOrNull()
            state = state.copy(
                detail = state.detail.copy(
                    selectedEntity = loaded,
                    selectedTrack = firstTrack,
                    isLoadingEntityMeta = false
                ),
                screen = actualScreen.copy(
                    tracks = tracks,
                    artistDashboardItems = dashboardItems,
                    artistDashboardX = 0,
                    artistDashboardY = 0,
                    selectedIndex = 0,
                    isLoading = false
                )
            )
            if (loaded is SearchResult.Artist) {
                val firstItemIndex = dashboardItems.indexOfFirst { it !is String }
                if (firstItemIndex != -1) {
                    artistDashboardList.selected(firstItemIndex)
                }
                appRunner()?.focusManager()?.setFocus("artist-dashboard-list")
            } else {
                entityTracksList.selected(0)
                if (firstTrack != null) {
                    debouncedLoadDetails(firstTrack)
                }
                appRunner()?.focusManager()?.setFocus("entity-tracks-list")
            }
        }
    }
}

internal fun MeloScreen.loadEntityDetails(entity: SearchResult) {
    try {
        detailsJob?.cancel()
    } catch (_: Exception) {
    }
    state = state.copy(
        detail = state.detail.copy(
            artworkData = null, isLoadingEntityMeta = true, entityGenres = emptyList()
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
        launch {
            val data = url?.let {
                try {
                    artworkRenderer.load(
                        it.replace(
                            "w120-h120", "w512-h512"
                        )
                    ) // Request higher res if possible
                } catch (_: Throwable) {
                    null
                }
            }
            if (isActive) {
                appRunner()?.runOnRenderThread {
                    state = state.copy(
                        detail = state.detail.copy(
                            artworkData = data
                        )
                    )
                }
            }
        }

        val detailsDeferred = async {
            try {
                mergeEntityDetails(entity, getEntityDetails(entity))
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

        val detailedEntity = detailsDeferred.await()
        val genres = tagsDeferred.await()

        if (isActive) {
            appRunner()?.runOnRenderThread {
                state = state.copy(
                    detail = state.detail.copy(
                        selectedEntity = detailedEntity,
                        entityGenres = genres,
                        isLoadingEntityMeta = false
                    )
                )
            }
        }
    }
}

internal fun MeloScreen.loadTrackDetails(trackId: String, knownTrack: Track? = null) {
    try {
        detailsJob?.cancel()
    } catch (_: Exception) {
    }
    state = state.copy(
        detail = state.detail.copy(
            lyrics = null,
            syncedLyrics = emptyList(),
            lyricsScrollOffset = 0,
            isAutoScrollLyrics = true,
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
                    val track =
                        knownTrack ?: fullTrackDeferred.await() ?: return@async emptyList<Track>()
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

            if (isActive) {
                appRunner()?.runOnRenderThread {
                    state = state.copy(
                        detail = state.detail.copy(
                            selectedTrack = updatedTrack, artworkData = null
                        )
                    )
                    if (state.detail.detailTab == DetailTab.LYRICS) {
                        loadLyrics()
                    }
                }
            }

            launch {
                val artworkData = artworkUrl?.let {
                    try {
                        artworkRenderer.load(it)
                    } catch (_: Throwable) {
                        null
                    }
                }
                if (isActive) {
                    appRunner()?.runOnRenderThread {
                        state = state.copy(
                            detail = state.detail.copy(
                                artworkData = artworkData
                            )
                        )
                    }
                }
            }

            if (isActive) {
                val similar = similarDeferred.await()
                if (isActive) appRunner()?.runOnRenderThread {
                    state = state.copy(
                        detail = state.detail.copy(
                            similarTracks = similar, isLoadingSimilar = false
                        )
                    )
                }
            }
        }
    }
}
