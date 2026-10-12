package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.resolveSimilarTracks
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.util.resolveOfflineEntitySync
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.time.Duration.Companion.milliseconds

internal fun buildArtistDashboardItems(artist: SearchResult.Artist): List<Any> {
    val dashboardItems = mutableListOf<Any>()
    val topSongs = artist.topSongs.orEmpty()
    val hasSongSection =
        artist.sections.any { section ->
            section.items.any { it is SearchResult.Song }
        }
    val sections =
        buildList {
            if (topSongs.isNotEmpty() && !hasSongSection) {
                add(
                    SearchResult.ArtistSection(
                        title = "Top Songs",
                        items =
                            topSongs.map { track ->
                                SearchResult.Song(track = track)
                            },
                    ),
                )
            }
            addAll(artist.sections)
        }
    val chunkedSections = sections.filter { it.items.isNotEmpty() }.chunked(2)
    chunkedSections.forEach { chunk ->
        if (chunk.size == 2) {
            dashboardItems.add(Pair(chunk[0], chunk[1]))
        } else {
            dashboardItems.add(Pair(chunk[0], null))
        }
    }
    return dashboardItems
}

private fun mergeEntityDetails(
    original: SearchResult,
    loaded: SearchResult,
): SearchResult =
    when (original) {
        is SearchResult.Album if loaded is SearchResult.Album -> {
            loaded.copy(
                title = loaded.title.ifBlank { original.title },
                author = loaded.author.ifBlank { original.author },
                year = loaded.year ?: original.year,
                artworkUrl = loaded.artworkUrl ?: original.artworkUrl,
            )
        }

        is SearchResult.Playlist if loaded is SearchResult.Playlist -> {
            loaded.copy(
                title = loaded.title.ifBlank { original.title },
                author = loaded.author.ifBlank { original.author },
                trackCount = loaded.trackCount ?: original.trackCount,
                artworkUrl = loaded.artworkUrl ?: original.artworkUrl,
            )
        }

        is SearchResult.Artist if loaded is SearchResult.Artist -> {
            loaded.copy(
                name = loaded.name.ifBlank { original.name },
                artworkUrl = loaded.artworkUrl ?: original.artworkUrl,
            )
        }

        else -> loaded
    }

internal fun MeloScreen.debouncedLoadDetails(track: Track) {
    try {
        detailsJob?.cancel()
    } catch (_: Exception) {
    }
    detailsJob =
        scope.launch {
            delay(150.milliseconds)
            if (isActive) loadTrackDetails(track.id, track)
        }
}

internal fun MeloScreen.debouncedLoadEntityDetails(entity: SearchResult) {
    try {
        detailsJob?.cancel()
    } catch (_: Exception) {
    }
    detailsJob =
        scope.launch {
            delay(150.milliseconds)
            if (isActive) loadEntityDetails(entity)
        }
}

internal fun MeloScreen.openEntityDetails(entity: SearchResult) {
    val returnScreen = state.screen
    val returnSection = state.navigation.activeSection
    val title =
        when (entity) {
            is SearchResult.Album -> entity.title
            is SearchResult.Artist -> entity.name
            is SearchResult.Playlist -> entity.title
            is SearchResult.Song -> entity.track.title
        }
    val subtitle =
        when (entity) {
            is SearchResult.Album -> entity.author
            is SearchResult.Playlist -> entity.author
            is SearchResult.Artist -> null
            is SearchResult.Song -> entity.track.artist
        }
    val description =
        when (entity) {
            is SearchResult.Album -> entity.description
            is SearchResult.Playlist -> entity.description
            is SearchResult.Artist -> entity.description
            is SearchResult.Song -> null
        }

    val resolvedEntity =
        resolveOfflineEntitySync(
            initial = entity,
            entityCache = entityCache,
            allOffline = state.collections.offlineTracks,
        )
    val effectiveEntity = resolvedEntity ?: entity

    val initialTracks =
        when (effectiveEntity) {
            is SearchResult.Album -> effectiveEntity.songs.orEmpty()
            is SearchResult.Playlist -> effectiveEntity.songs.orEmpty()
            is SearchResult.Artist -> effectiveEntity.topSongs.orEmpty()
            is SearchResult.Song -> listOf(effectiveEntity.track)
        }

    val initialDashboard =
        if (effectiveEntity is SearchResult.Artist) {
            buildArtistDashboardItems(effectiveEntity)
        } else {
            emptyList()
        }

    val isResolved = resolvedEntity != null
    val hasContent = initialTracks.isNotEmpty() || initialDashboard.isNotEmpty()

    state =
        state.copy(
            detail =
                state.detail.copy(
                    selectedEntity = effectiveEntity,
                    selectedTrack = initialTracks.firstOrNull(),
                    isLoadingEntityMeta = !isResolved,
                    artworkData = null,
                ),
            screen =
                ScreenState.EntityDetail(
                    entity = effectiveEntity,
                    title = title,
                    subtitle = subtitle,
                    description = description,
                    tracks = initialTracks,
                    artistDashboardItems = initialDashboard,
                    selectedIndex = 0,
                    isLoading = !isResolved && !state.isOfflineMode,
                    errorMessage =
                        if (!isResolved && state.isOfflineMode) {
                            "No content available offline"
                        } else {
                            null
                        },
                    returnScreen = returnScreen,
                    returnSection = returnSection,
                ),
        )

    if (effectiveEntity !is SearchResult.Artist) {
        entityTracksList.selected(0)
        appRunner()?.focusManager()?.setFocus("entity-tracks-list")
    } else {
        val firstItemIndex = initialDashboard.indexOfFirst { it !is String }
        if (firstItemIndex != -1) {
            artistDashboardList.selected(firstItemIndex)
        } else {
            artistDashboardList.selected(0)
        }
        appRunner()?.focusManager()?.setFocus("artist-dashboard-list")
    }

    if (state.isOfflineMode) return
    debouncedLoadEntityDetails(entity)

    scope.launch {
        val loaded =
            try {
                getEntityDetails(entity)
            } catch (e: Exception) {
                if (isActive) {
                    appRunner()?.runOnRenderThread {
                        val cur = state.screen as? ScreenState.EntityDetail ?: return@runOnRenderThread
                        if (!isResolved && !hasContent) {
                            state =
                                state.copy(
                                    screen =
                                        cur.copy(
                                            isLoading = false,
                                            errorMessage = e.message ?: "Failed to load details",
                                        ),
                                )
                        } else {
                            state =
                                state.copy(
                                    screen =
                                        cur.copy(
                                            isLoading = false,
                                        ),
                                )
                        }
                    }
                }
                return@launch
            }

        entityCache?.save(loaded)

        val tracks =
            when (loaded) {
                is SearchResult.Album -> loaded.songs.orEmpty()
                is SearchResult.Playlist -> loaded.songs.orEmpty()
                is SearchResult.Artist -> loaded.topSongs.orEmpty()
                is SearchResult.Song -> listOf(loaded.track)
            }

        val dashboardItems =
            if (loaded is SearchResult.Artist) {
                buildArtistDashboardItems(loaded)
            } else {
                emptyList()
            }

        if (isActive) {
            appRunner()?.runOnRenderThread {
                val actualScreen = state.screen as? ScreenState.EntityDetail ?: return@runOnRenderThread
                val firstTrack = tracks.firstOrNull()
                state =
                    state.copy(
                        detail =
                            state.detail.copy(
                                selectedEntity = loaded,
                                selectedTrack = firstTrack,
                                isLoadingEntityMeta = false,
                            ),
                        screen =
                            actualScreen.copy(
                                tracks = tracks,
                                artistDashboardItems = dashboardItems,
                                artistDashboardX = 0,
                                artistDashboardY = 0,
                                selectedIndex = 0,
                                isLoading = false,
                            ),
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
}

internal fun MeloScreen.loadEntityDetails(entity: SearchResult) {
    try {
        detailsJob?.cancel()
    } catch (_: Exception) {
    }
    state =
        state.copy(
            detail =
                state.detail.copy(
                    artworkData = null,
                    isLoadingEntityMeta = true,
                    entityGenres = emptyList(),
                ),
        )
    if (state.isOfflineMode) {
        state = state.copy(detail = state.detail.copy(isLoadingEntityMeta = false))
        return
    }

    val url =
        when (entity) {
            is SearchResult.Album -> entity.artworkUrl
            is SearchResult.Artist -> entity.artworkUrl
            is SearchResult.Playlist -> entity.artworkUrl
            is SearchResult.Song -> return
        }

    detailsJob =
        scope.launch {
            launch {
                val data =
                    url?.let {
                        try {
                            artworkRenderer.load(
                                it.replace(
                                    "w120-h120",
                                    "w512-h512",
                                ),
                            )
                        } catch (_: Throwable) {
                            null
                        }
                    }
                if (isActive) {
                    appRunner()?.runOnRenderThread {
                        state =
                            state.copy(
                                detail =
                                    state.detail.copy(
                                        artworkData = data,
                                    ),
                            )
                    }
                }
            }

            val detailsDeferred =
                async {
                    try {
                        mergeEntityDetails(entity, getEntityDetails(entity))
                    } catch (_: Exception) {
                        entity
                    }
                }

            val tagsDeferred =
                async {
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
                    state =
                        state.copy(
                            detail =
                                state.detail.copy(
                                    selectedEntity = detailedEntity,
                                    entityGenres = genres,
                                    isLoadingEntityMeta = false,
                                ),
                        )
                }
            }
        }
}

internal fun MeloScreen.loadTrackDetails(
    trackId: String,
    knownTrack: Track? = null,
) {
    try {
        detailsJob?.cancel()
    } catch (_: Exception) {
    }
    state =
        state.copy(
            detail =
                state.detail.copy(
                    lyrics = null,
                    syncedLyrics = emptyList(),
                    lyricsScrollOffset = 0,
                    isAutoScrollLyrics = true,
                    isLoadingLyrics = false,
                    similarTracks = emptyList(),
                    isLoadingSimilar = true,
                    artworkData = null,
                ),
        )
    if (state.isOfflineMode) {
        state = state.copy(detail = state.detail.copy(isLoadingSimilar = false))
        return
    }
    detailsJob =
        scope.launch {
            supervisorScope {
                val fullTrackDeferred =
                    async {
                        try {
                            getTrack(trackId)
                        } catch (_: Exception) {
                            null
                        }
                    }

                val similarDeferred =
                    async {
                        try {
                            val track =
                                knownTrack ?: fullTrackDeferred.await() ?: return@async emptyList<Track>()
                            resolveSimilarTracks(track, limit = 10)
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }

                val fullTrack =
                    fullTrackDeferred.await() ?: knownTrack ?: run {
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
                        state =
                            state.copy(
                                detail =
                                    state.detail.copy(
                                        selectedTrack = updatedTrack,
                                        artworkData = null,
                                    ),
                            )
                        if (state.detail.detailTab == DetailTab.LYRICS) {
                            loadLyrics()
                        }
                    }
                }

                launch {
                    val artworkData =
                        artworkUrl?.let {
                            try {
                                artworkRenderer.load(it)
                            } catch (_: Throwable) {
                                null
                            }
                        }
                    if (isActive) {
                        appRunner()?.runOnRenderThread {
                            state =
                                state.copy(
                                    detail =
                                        state.detail.copy(
                                            artworkData = artworkData,
                                        ),
                                )
                        }
                    }
                }

                if (isActive) {
                    val similar = similarDeferred.await()
                    if (isActive) {
                        appRunner()?.runOnRenderThread {
                            state =
                                state.copy(
                                    detail =
                                        state.detail.copy(
                                            similarTracks = similar,
                                            isLoadingSimilar = false,
                                        ),
                                )
                        }
                    }
                }
            }
        }
}
