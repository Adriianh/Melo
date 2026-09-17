package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.handleGlobalShortcuts
import com.github.adriianh.cli.tui.handler.loadMoreSimilar
import com.github.adriianh.cli.tui.handler.matchesAction
import com.github.adriianh.cli.tui.handler.openPlaylistPicker
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.playList
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.cli.tui.handler.resolveSimilarTracks
import com.github.adriianh.cli.tui.handler.toggleFavorite
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
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
                } catch (_: Exception) {
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
                }
            }

            launch {
                val artworkData = artworkUrl?.let {
                    try {
                        artworkRenderer.load(it)
                    } catch (_: Exception) {
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

internal fun MeloScreen.loadNowPlayingMetadata(track: Track) {
    try {
        nowPlayingMetadataJob?.cancel()
    } catch (_: Exception) {
    }
    if (state.isOfflineMode) return
    nowPlayingMetadataJob = scope.launch {
        val resolvedMetadata = if (track.artworkUrl == null || track.album.isBlank()) {
            metadataProvider.resolveMetadata(track.title, track.artist)
        } else null

        val artworkUrl = resolvedMetadata?.artworkUrl ?: track.artworkUrl
        val album = resolvedMetadata?.album ?: track.album

        if (isActive) appRunner()?.runOnRenderThread {
            if (state.player.nowPlaying?.id == track.id) {
                val updatedTrack = state.player.nowPlaying?.copy(
                    artworkUrl = artworkUrl, album = album
                ) ?: track
                state = state.copy(
                    player = state.player.copy(
                        nowPlaying = updatedTrack
                    )
                )
                if (state.player.isPlaying) {
                    mediaSession.updateTrack(updatedTrack, updatedTrack.durationMs)
                }
            }
        }

        launch {
            val artwork = artworkUrl?.let { artworkRenderer.load(it) }

            if (isActive) appRunner()?.runOnRenderThread {
                if (state.player.nowPlaying?.id == track.id) {
                    state = state.copy(
                        player = state.player.copy(
                            nowPlayingArtwork = artwork
                        )
                    )

                    if (settingsViewState.currentSettings.discordRpcEnabled) {
                        discordRpcManager.updateActivity(
                            state.player.nowPlaying, state.player.isPlaying
                        )
                    }
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
                lyrics = "Lyrics are unavailable in Offline Mode", isLoadingLyrics = false
            )
        )
        return
    }
    state = state.copy(detail = state.detail.copy(isLoadingLyrics = true, lyrics = null))
    scope.launch {
        val lyrics = getLyrics(track.artist, track.title)
        appRunner()?.runOnRenderThread {
            state = state.copy(
                detail = state.detail.copy(
                    lyrics = lyrics ?: "Lyrics not found", isLoadingLyrics = false
                )
            )
        }
    }
}

internal fun MeloScreen.handleDetailKey(event: KeyEvent): EventResult {
    when {
        event.isChar('1') -> {
            state = state.copy(detail = state.detail.copy(detailTab = DetailTab.INFO))
            return EventResult.HANDLED
        }

        event.isChar('2') -> {
            state = state.copy(detail = state.detail.copy(detailTab = DetailTab.LYRICS))
            if (state.detail.lyrics == null && !state.detail.isLoadingLyrics) loadLyrics()
            appRunner()?.focusManager()?.setFocus("lyrics-area")
            return EventResult.HANDLED
        }

        event.isChar('3') -> {
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
            state = state.copy(
                detail = state.detail.copy(
                    similarCursor = maxOf(
                        0, state.detail.similarCursor - 1
                    )
                )
            )
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER && state.detail.detailTab == DetailTab.SIMILAR -> {
            state.detail.similarTracks.getOrNull(state.detail.similarCursor)?.let { playTrack(it) }
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}

internal fun MeloScreen.handleEntityDetailKey(event: KeyEvent): EventResult {
    val actualDetail =
        state.screen as? ScreenState.EntityDetail ?: return handleGlobalShortcuts(event)

    val isDescFocused = appRunner()?.focusManager()?.focusedId() == "desc-area"
    if (isDescFocused) {
        if (event.code() == KeyCode.ESCAPE || (event.modifiers()
                .alt() && event.code() == KeyCode.LEFT)
        ) {
            val targetFocus =
                if (actualDetail.entity is SearchResult.Artist) "artist-dashboard-list" else "entity-tracks-list"
            appRunner()?.focusManager()?.setFocus(targetFocus)
            return EventResult.HANDLED
        }
        return handleGlobalShortcuts(event)
    }

    if (actualDetail.entity is SearchResult.Artist) {
        val items = actualDetail.artistDashboardItems
        val listSize = items.size

        when {
            event.code() == KeyCode.ESCAPE || (event.modifiers()
                .alt() && event.code() == KeyCode.LEFT) -> {
                state = state.copy(
                    screen = actualDetail.returnScreen,
                    navigation = state.navigation.copy(activeSection = actualDetail.returnSection)
                )
                val targetFocus = when (actualDetail.returnScreen) {
                    is ScreenState.Home -> "home-panel"
                    is ScreenState.Search -> "results-panel"
                    is ScreenState.EntityDetail -> {
                        if (actualDetail.returnScreen.entity is SearchResult.Artist) "artist-dashboard-list" else "entity-tracks-list"
                    }

                    else -> "home-panel"
                }
                appRunner()?.focusManager()?.setFocus(targetFocus)
                return EventResult.HANDLED
            }

            event.matches(Actions.MOVE_DOWN) && listSize > 0 -> {
                val currentItem = items.getOrNull(artistDashboardList.selected())
                if (currentItem is Pair<*, *>) {
                    val section =
                        if (actualDetail.artistDashboardX == 1) currentItem.second else currentItem.first
                    if (section is SearchResult.ArtistSection) {
                        val currentY = actualDetail.artistDashboardPositions[section.title] ?: 0
                        if (currentY < section.items.size - 1) {
                            val newPositions = actualDetail.artistDashboardPositions.toMutableMap()
                            newPositions[section.title] = currentY + 1
                            state =
                                state.copy(screen = actualDetail.copy(artistDashboardPositions = newPositions))
                            return EventResult.HANDLED
                        }
                    }
                }

                var newIndex = minOf(listSize - 1, artistDashboardList.selected() + 1)
                while (newIndex < listSize - 1 && items[newIndex] is String) {
                    newIndex++
                }
                if (items[newIndex] !is String) {
                    artistDashboardList.selected(newIndex)
                    if (items[newIndex] is Pair<*, *>) {
                        val pair = items[newIndex] as Pair<*, *>
                        if (actualDetail.artistDashboardX == 1 && pair.second == null) {
                            state = state.copy(screen = actualDetail.copy(artistDashboardX = 0))
                        }
                    } else {
                        state = state.copy(screen = actualDetail.copy(artistDashboardX = 0))
                    }
                }
                return EventResult.HANDLED
            }

            event.matches(Actions.MOVE_UP) && listSize > 0 -> {
                val currentItem = items.getOrNull(artistDashboardList.selected())
                if (currentItem is Pair<*, *>) {
                    val section =
                        if (actualDetail.artistDashboardX == 1) currentItem.second else currentItem.first
                    if (section is SearchResult.ArtistSection) {
                        val currentY = actualDetail.artistDashboardPositions[section.title] ?: 0
                        if (currentY > 0) {
                            val newPositions = actualDetail.artistDashboardPositions.toMutableMap()
                            newPositions[section.title] = currentY - 1
                            state =
                                state.copy(screen = actualDetail.copy(artistDashboardPositions = newPositions))
                            return EventResult.HANDLED
                        }
                    }
                }

                var newIndex = maxOf(0, artistDashboardList.selected() - 1)
                while (newIndex > 0 && items[newIndex] is String) {
                    newIndex--
                }
                if (items[newIndex] !is String) {
                    artistDashboardList.selected(newIndex)
                    if (items[newIndex] is Pair<*, *>) {
                        val pair = items[newIndex] as Pair<*, *>
                        if (actualDetail.artistDashboardX == 1 && pair.second == null) {
                            state = state.copy(screen = actualDetail.copy(artistDashboardX = 0))
                        }
                    } else {
                        state = state.copy(screen = actualDetail.copy(artistDashboardX = 0))
                    }
                }
                return EventResult.HANDLED
            }

            event.matches(Actions.MOVE_LEFT) && listSize > 0 -> {
                val item = items.getOrNull(artistDashboardList.selected())
                if (item is Pair<*, *> && actualDetail.artistDashboardX == 1) {
                    state = state.copy(screen = actualDetail.copy(artistDashboardX = 0))
                    return EventResult.HANDLED
                }
            }

            event.matches(Actions.MOVE_RIGHT) && listSize > 0 -> {
                val item = items.getOrNull(artistDashboardList.selected())
                if (item is Pair<*, *> && actualDetail.artistDashboardX == 0 && item.second != null) {
                    state = state.copy(screen = actualDetail.copy(artistDashboardX = 1))
                    return EventResult.HANDLED
                }
            }

            event.isCharIgnoreCase('d') && listSize > 0 -> {
                val desc = actualDetail.entity.description
                if (!desc.isNullOrEmpty()) {
                    appRunner()?.focusManager()?.setFocus("desc-area")
                    return EventResult.HANDLED
                }
            }

            event.code() == KeyCode.ENTER && listSize > 0 -> {
                val arrayItem = items.getOrNull(artistDashboardList.selected())
                var targetItem = if (arrayItem is Pair<*, *>) {
                    if (actualDetail.artistDashboardX == 1) arrayItem.second else arrayItem.first
                } else arrayItem

                if (targetItem is SearchResult.ArtistSection) {
                    val currentY = actualDetail.artistDashboardPositions[targetItem.title] ?: 0
                    targetItem = targetItem.items.getOrNull(currentY) ?: return EventResult.HANDLED
                }

                when (targetItem) {
                    is Track -> {
                        downloadTrack(targetItem, DownloadType.PREFETCH)
                        playTrack(targetItem)
                    }

                    is SearchResult.Song -> {
                        downloadTrack(targetItem.track, DownloadType.PREFETCH)
                        playTrack(targetItem.track)
                    }

                    is SearchResult -> {
                        openEntityDetails(targetItem)
                    }
                }
                return EventResult.HANDLED
            }

            listSize > 0 && event.matchesAction(
                MeloAction.FAVORITE, settingsViewState.currentSettings
            ) -> {
                val arrayItem = items.getOrNull(artistDashboardList.selected())
                var item = if (arrayItem is Pair<*, *>) {
                    if (actualDetail.artistDashboardX == 1) arrayItem.second else arrayItem.first
                } else arrayItem
                if (item is SearchResult.ArtistSection) {
                    val currentY = actualDetail.artistDashboardPositions[item.title] ?: 0
                    item = item.items.getOrNull(currentY) ?: return EventResult.HANDLED
                }
                (item as? Track ?: (item as? SearchResult.Song)?.track)?.let { toggleFavorite(it) }
                return EventResult.HANDLED
            }

            listSize > 0 && event.matchesAction(
                MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings
            ) -> {
                val arrayItem = items.getOrNull(artistDashboardList.selected())
                var item = if (arrayItem is Pair<*, *>) {
                    if (actualDetail.artistDashboardX == 1) arrayItem.second else arrayItem.first
                } else arrayItem
                if (item is SearchResult.ArtistSection) {
                    val currentY = actualDetail.artistDashboardPositions[item.title] ?: 0
                    item = item.items.getOrNull(currentY) ?: return EventResult.HANDLED
                }
                (item as? Track ?: (item as? SearchResult.Song)?.track)?.let { addToQueue(it) }
                return EventResult.HANDLED
            }

            listSize > 0 && event.matchesAction(
                MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings
            ) -> {
                val arrayItem = items.getOrNull(artistDashboardList.selected())
                var item = if (arrayItem is Pair<*, *>) {
                    if (actualDetail.artistDashboardX == 1) arrayItem.second else arrayItem.first
                } else arrayItem
                if (item is SearchResult.ArtistSection) {
                    val currentY = actualDetail.artistDashboardPositions[item.title] ?: 0
                    item = item.items.getOrNull(currentY) ?: return EventResult.HANDLED
                }
                (item as? Track
                    ?: (item as? SearchResult.Song)?.track)?.let { openPlaylistPicker(it) }
                return EventResult.HANDLED
            }
        }

        return handleGlobalShortcuts(event)
    }

    val tracks = actualDetail.tracks
    val listSize = tracks.size

    when {
        event.code() == KeyCode.ESCAPE || (event.modifiers()
            .alt() && event.code() == KeyCode.LEFT) -> {
            state = state.copy(
                screen = actualDetail.returnScreen,
                navigation = state.navigation.copy(activeSection = actualDetail.returnSection)
            )
            val targetFocus = when (actualDetail.returnScreen) {
                is ScreenState.Home -> "home-panel"
                is ScreenState.Search -> "results-panel"
                is ScreenState.Library -> "library-panel"
                is ScreenState.EntityDetail -> {
                    if (actualDetail.returnScreen.entity is SearchResult.Artist) "artist-dashboard-list" else "entity-tracks-list"
                }

                else -> "home-panel"
            }
            appRunner()?.focusManager()?.setFocus(targetFocus)
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) && listSize > 0 -> {
            val newIndex = minOf(listSize - 1, entityTracksList.selected() + 1)
            entityTracksList.selected(newIndex)
            state = state.copy(screen = actualDetail.copy(selectedIndex = newIndex))
            val track = tracks.getOrNull(newIndex)
            if (track != null) {
                state = state.copy(
                    detail = state.detail.copy(
                        selectedTrack = track
                    )
                )
                debouncedLoadDetails(track)
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) && listSize > 0 -> {
            val newIndex = maxOf(0, entityTracksList.selected() - 1)
            entityTracksList.selected(newIndex)
            state = state.copy(screen = actualDetail.copy(selectedIndex = newIndex))
            val track = tracks.getOrNull(newIndex)
            if (track != null) {
                state = state.copy(
                    detail = state.detail.copy(
                        selectedTrack = track
                    )
                )
                debouncedLoadDetails(track)
            }
            return EventResult.HANDLED
        }

        event.isChar(' ') && listSize > 0 -> {
            val firstTrack = tracks.firstOrNull() ?: return handleGlobalShortcuts(event)
            downloadTrack(firstTrack, DownloadType.PREFETCH)
            playList(tracks, 0)
            return EventResult.HANDLED
        }

        (event.code() == KeyCode.TAB || event.isCharIgnoreCase('d')) && listSize > 0 -> {
            val desc = actualDetail.description ?: when (val e = state.detail.selectedEntity) {
                is SearchResult.Artist -> e.description
                is SearchResult.Album -> e.description
                is SearchResult.Playlist -> e.description
                else -> null
            }

            if (!desc.isNullOrBlank()) {
                appRunner()?.focusManager()?.setFocus("desc-area")
                return EventResult.HANDLED
            }
        }

        event.code() == KeyCode.ENTER && listSize > 0 -> {
            val selectedIndex = entityTracksList.selected()
            val track = tracks.getOrNull(selectedIndex) ?: return handleGlobalShortcuts(event)
            downloadTrack(track, DownloadType.PREFETCH)
            playList(tracks, selectedIndex)
            return EventResult.HANDLED
        }

        listSize > 0 && event.matchesAction(
            MeloAction.FAVORITE, settingsViewState.currentSettings
        ) -> {
            tracks.getOrNull(entityTracksList.selected())?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        listSize > 0 && event.matchesAction(
            MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings
        ) -> {
            tracks.getOrNull(entityTracksList.selected())?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        listSize > 0 && event.matchesAction(
            MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings
        ) -> {
            tracks.getOrNull(entityTracksList.selected())?.let { openPlaylistPicker(it) }
            return EventResult.HANDLED
        }
    }

    return handleGlobalShortcuts(event)
}