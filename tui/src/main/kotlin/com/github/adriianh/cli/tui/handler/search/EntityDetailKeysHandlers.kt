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


internal fun MeloScreen.toggleEntityFavorite(entity: SearchResult) {
    val favEntity = when (entity) {
        is SearchResult.Album -> entity.toFavoriteEntity()
        is SearchResult.Artist -> entity.toFavoriteEntity()
        is SearchResult.Playlist -> entity.toFavoriteEntity()
        is SearchResult.Song -> {
            toggleFavorite(entity.track)
            return
        }
    }
    scope.launch {
        val currentlyFav = state.isFavoriteEntity(favEntity.id)
        val newFavState = !currentlyFav

        toggleFavoriteEntity?.invoke(favEntity)

        if (!newFavState) {
            appRunner()?.runOnRenderThread {
                val rawId = favEntity.id.removePrefix("piped:")
                val updatedRemoteAlbums = state.collections.remoteAlbums.filterNot {
                    it.id == favEntity.id || it.id.removePrefix("piped:") == rawId
                }
                val updatedRemoteArtists = state.collections.remoteArtists.filterNot {
                    it.id == favEntity.id || it.id.removePrefix("piped:") == rawId
                }
                val updatedRemotePlaylists = state.collections.remotePlaylists.filterNot {
                    it.id == favEntity.id || it.id.removePrefix("piped:") == rawId
                }
                state = state.copy(
                    collections = state.collections.copy(
                        remoteAlbums = updatedRemoteAlbums,
                        remoteArtists = updatedRemoteArtists,
                        remotePlaylists = updatedRemotePlaylists
                    )
                )
            }
        }

        val settings = settingsViewState.currentSettings
        val isLoggedIn = !settings.sessionCookies.isNullOrBlank()
        if (isLoggedIn && settings.syncLikesToYouTube) {
            val rawId = favEntity.id.removePrefix("piped:")
            try {
                when (favEntity.type) {
                    FavoriteEntityType.ALBUM -> toggleLikeAlbum?.invoke(rawId, newFavState)
                    FavoriteEntityType.ARTIST -> subscribeChannel?.invoke(rawId, newFavState)
                    FavoriteEntityType.PLAYLIST -> toggleLikePlaylist?.invoke(rawId, newFavState)
                }
            } catch (_: Exception) {
            }
        }
    }
}

internal fun MeloScreen.handleEntityDetailKey(event: KeyEvent): EventResult {
    val isDescFocused = appRunner()?.focusManager()?.focusedId() == "desc-area"
    if (isDescFocused) {
        if (event.code() == KeyCode.ESCAPE || event.code() == KeyCode.TAB || (event.modifiers()
                .alt() && event.code() == KeyCode.LEFT)
        ) {
            val targetFocus = when (val curScreen = state.screen) {
                is ScreenState.Search -> "results-panel"
                is ScreenState.EntityDetail -> {
                    if (curScreen.entity is SearchResult.Artist) "artist-dashboard-list" else "entity-tracks-list"
                }

                else -> "results-panel"
            }
            appRunner()?.focusManager()?.setFocus(targetFocus)
            return EventResult.HANDLED
        }
        return handleGlobalShortcuts(event)
    }

    val actualDetail =
        state.screen as? ScreenState.EntityDetail ?: return handleGlobalShortcuts(event)

    if (event.isChar('F') || (event.modifiers().shift() && event.isCharIgnoreCase('f'))) {
        toggleEntityFavorite(actualDetail.entity)
        return EventResult.HANDLED
    }

    if (actualDetail.entity is SearchResult.Artist) {
        val items = actualDetail.artistDashboardItems
        val listSize = items.size

        when {
            event.code() == KeyCode.ESCAPE || (event.modifiers()
                .alt() && event.code() == KeyCode.LEFT) -> {
                if (actualDetail.returnScreen is ScreenState.Home) {
                    cachedHomeScreen = actualDetail.returnScreen
                } else if (actualDetail.returnScreen is ScreenState.Stats) {
                    cachedStatsScreen = actualDetail.returnScreen
                }
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

            (event.code() == KeyCode.TAB || event.isCharIgnoreCase('d')) && listSize > 0 -> {
                val desc = actualDetail.entity.description
                if (!desc.isNullOrEmpty()) {
                    appRunner()?.focusManager()?.setFocus("desc-area")
                    return EventResult.HANDLED
                }
            }

            listSize > 0 && (event.isCharIgnoreCase('m') || event.matchesAction(
                MeloAction.TRACK_OPTIONS,
                settingsViewState.currentSettings
            )) -> {
                val arrayItem = items.getOrNull(artistDashboardList.selected())
                var item = if (arrayItem is Pair<*, *>) {
                    if (actualDetail.artistDashboardX == 1) arrayItem.second else arrayItem.first
                } else arrayItem
                if (item is SearchResult.ArtistSection) {
                    val currentY = actualDetail.artistDashboardPositions[item.title] ?: 0
                    item = item.items.getOrNull(currentY) ?: return EventResult.HANDLED
                }
                (item as? Track
                    ?: (item as? SearchResult.Song)?.track)?.let { openTrackOptions(it) }
                return EventResult.HANDLED
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

            event.isCharIgnoreCase('r') -> {
                val seedTrack = actualDetail.entity.topSongs?.firstOrNull()
                    ?: items.filterIsInstance<Track>().firstOrNull()
                    ?: items.filterIsInstance<SearchResult.Song>().firstOrNull()?.track
                if (seedTrack != null) {
                    downloadTrack(seedTrack, DownloadType.PREFETCH)
                    playTrack(seedTrack)
                    return EventResult.HANDLED
                }
            }
        }

        return handleGlobalShortcuts(event)
    }

    val tracks = filterAndSortTracks(
        tracks = actualDetail.tracks,
        sortOrder = actualDetail.sortOrder,
        sortDirection = actualDetail.sortDirection,
        query = actualDetail.searchQuery
    )
    val listSize = tracks.size

    if (actualDetail.isTyping) {
        when {
            event.code() == KeyCode.ENTER -> {
                state = state.copy(screen = actualDetail.copy(isTyping = false))
                return EventResult.HANDLED
            }

            event.code() == KeyCode.ESCAPE -> {
                state = state.copy(screen = actualDetail.copy(isTyping = false, searchQuery = ""))
                return EventResult.HANDLED
            }

            event.code() == KeyCode.BACKSPACE -> {
                state = state.copy(
                    screen = actualDetail.copy(
                        searchQuery = actualDetail.searchQuery.dropLast(1)
                    )
                )
                return EventResult.HANDLED
            }

            event.code() == KeyCode.CHAR -> {
                val text = event.string()
                state =
                    state.copy(screen = actualDetail.copy(searchQuery = actualDetail.searchQuery + text))
                return EventResult.HANDLED
            }
        }
        return EventResult.HANDLED
    }

    when {
        event.isCtrlF() -> {
            state = state.copy(screen = actualDetail.copy(isTyping = true))
            return EventResult.HANDLED
        }

        event.isChar('O') || (event.modifiers().shift() && event.isCharIgnoreCase('o')) -> {
            state = state.copy(
                screen = actualDetail.copy(
                    sortDirection = actualDetail.sortDirection.toggle(),
                    selectedIndex = 0
                )
            )
            entityTracksList.selected(0)
            return EventResult.HANDLED
        }

        event.isChar('o') && !event.modifiers().shift() -> {
            state = state.copy(
                screen = actualDetail.copy(
                    sortOrder = actualDetail.sortOrder.next(),
                    selectedIndex = 0
                )
            )
            entityTracksList.selected(0)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE -> {
            if (actualDetail.searchQuery.isNotEmpty()) {
                state = state.copy(screen = actualDetail.copy(searchQuery = ""))
                return EventResult.HANDLED
            }
            if (actualDetail.returnScreen is ScreenState.Home) {
                cachedHomeScreen = actualDetail.returnScreen
            } else if (actualDetail.returnScreen is ScreenState.Stats) {
                cachedStatsScreen = actualDetail.returnScreen
            }
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

        event.modifiers().alt() && event.code() == KeyCode.LEFT -> {
            if (actualDetail.returnScreen is ScreenState.Home) {
                cachedHomeScreen = actualDetail.returnScreen
            } else if (actualDetail.returnScreen is ScreenState.Stats) {
                cachedStatsScreen = actualDetail.returnScreen
            }
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

        event.isCharIgnoreCase('s') && listSize > 0 -> {
            val shuffled = tracks.shuffled()
            shuffled.firstOrNull()?.let { downloadTrack(it, DownloadType.PREFETCH) }
            playList(shuffled, 0)
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('r') && listSize > 0 -> {
            val track = tracks.getOrNull(entityTracksList.selected()) ?: tracks.first()
            downloadTrack(track, DownloadType.PREFETCH)
            playTrack(track)
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

        listSize > 0 && (
                event.isCharIgnoreCase('v') || event.matchesAction(
                    MeloAction.TOGGLE_SELECTION,
                    settingsViewState.currentSettings
                ) || (state.selection.isNotEmpty && event.isChar(' '))
                ) -> {
            val track = tracks.getOrNull(entityTracksList.selected())
            if (track != null) {
                state = state.copy(selection = state.selection.toggle(track))
                return EventResult.HANDLED
            }
        }

        listSize > 0 && event.isCtrlA() -> {
            state = state.copy(selection = state.selection.selectAll(tracks))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ESCAPE && state.selection.isNotEmpty -> {
            state = state.copy(selection = state.selection.clear())
            return EventResult.HANDLED
        }

        listSize > 0 && event.matchesAction(
            MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings
        ) -> {
            if (state.selection.isNotEmpty) {
                openPlaylistPicker(state.selection.tracks())
            } else {
                tracks.getOrNull(entityTracksList.selected())?.let { openPlaylistPicker(it) }
            }
            return EventResult.HANDLED
        }

        listSize > 0 && (event.isCharIgnoreCase('m') || event.matchesAction(
            MeloAction.TRACK_OPTIONS,
            settingsViewState.currentSettings
        )) -> {
            if (state.selection.isNotEmpty) {
                openBatchOptions(state.selection.tracks())
                return EventResult.HANDLED
            }
            tracks.getOrNull(entityTracksList.selected())?.let { openTrackOptions(it) }
            return EventResult.HANDLED
        }
    }

    return handleGlobalShortcuts(event)
}
