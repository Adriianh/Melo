package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.*
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

/**
 * Checks if a KeyEvent matches a specific MeloAction based on current settings.
 */
internal fun KeyEvent.matchesAction(action: MeloAction, settings: Settings): Boolean {
    val binding = settings.keybindings[action] ?: return false
    
    // Check by character
    if (binding.char != null) {
        return this.code() == KeyCode.CHAR && this.character() == binding.char
    }
    
    // Check by key code
    if (binding.code != null) {
        return this.code().name == binding.code
    }
    
    return false
}

internal fun MeloScreen.handleSearchBarKey(event: KeyEvent): EventResult {
    val s = state.screen as? ScreenState.Search ?: return EventResult.UNHANDLED
    if (s.results.isNotEmpty() &&
        (event.matches(Actions.MOVE_DOWN) || event.matches(Actions.MOVE_UP))
    ) {
        return handleResultsKey(event)
    }
    return handleGlobalShortcuts(event)
}

internal fun MeloScreen.handleHomeKey(event: KeyEvent): EventResult {
    val s = state.screen as? ScreenState.Home ?: return EventResult.UNHANDLED
    val focusedId = appRunner()?.focusManager()?.focusedId()
    val isFocused = focusedId == "home-panel"
            || focusedId == "home-recent-panel"
            || focusedId == "home-favorites-panel"
    if (!isFocused) return EventResult.UNHANDLED

    if (focusedId == "home-recent-panel" && s.homeSection != HomeSection.RECENT) {
        updateScreen<ScreenState.Home> { it.copy(homeSection = HomeSection.RECENT) }
    } else if (focusedId == "home-favorites-panel" && s.homeSection != HomeSection.FAVORITES) {
        updateScreen<ScreenState.Home> { it.copy(homeSection = HomeSection.FAVORITES) }
    }

    if (event.code() == KeyCode.TAB) {
        val next = if (s.homeSection == HomeSection.RECENT) HomeSection.FAVORITES else HomeSection.RECENT
        updateScreen<ScreenState.Home> { it.copy(homeSection = next) }
        val nextFocusId = if (next == HomeSection.RECENT) "home-recent-panel" else "home-favorites-panel"
        appRunner()?.focusManager()?.setFocus(nextFocusId)
        return EventResult.HANDLED
    }

    when (s.homeSection) {
        HomeSection.RECENT -> {
            val maxIndex = (state.collections.recentTracks.size - 1).coerceAtLeast(0)
            when {
                event.matches(Actions.MOVE_DOWN) -> {
                    updateScreen<ScreenState.Home> { it.copy(homeRecentCursor = minOf(maxIndex, it.homeRecentCursor + 1)) }
                    return EventResult.HANDLED
                }

                event.matches(Actions.MOVE_UP) -> {
                    updateScreen<ScreenState.Home> { it.copy(homeRecentCursor = maxOf(0, it.homeRecentCursor - 1)) }
                    return EventResult.HANDLED
                }

                event.code() == KeyCode.ENTER -> {
                    val track = state.collections.recentTracks.getOrNull(s.homeRecentCursor)?.track ?: return EventResult.UNHANDLED
                    playTrack(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
                    val track = state.collections.recentTracks.getOrNull(s.homeRecentCursor)?.track ?: return EventResult.UNHANDLED
                    addToQueue(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
                    val track = state.collections.recentTracks.getOrNull(s.homeRecentCursor)?.track ?: return EventResult.UNHANDLED
                    toggleFavorite(track)
                    return EventResult.HANDLED
                }
                
                event.code() == KeyCode.CHAR && (event.character() == 'm' || event.character() == 'o') -> {
                    val track = state.collections.recentTracks.getOrNull(s.homeRecentCursor)?.track
                    if (track != null) openTrackOptions(track)
                    return EventResult.HANDLED
                }
            }
        }

        HomeSection.FAVORITES -> {
            val maxIndex = (state.collections.favorites.size - 1).coerceAtLeast(0)
            when {
                event.matches(Actions.MOVE_DOWN) -> {
                    updateScreen<ScreenState.Home> { it.copy(homeFavoritesCursor = minOf(maxIndex, it.homeFavoritesCursor + 1)) }
                    return EventResult.HANDLED
                }

                event.matches(Actions.MOVE_UP) -> {
                    updateScreen<ScreenState.Home> { it.copy(homeFavoritesCursor = maxOf(0, it.homeFavoritesCursor - 1)) }
                    return EventResult.HANDLED
                }

                event.code() == KeyCode.ENTER -> {
                    val track = state.collections.favorites.getOrNull(s.homeFavoritesCursor) ?: return EventResult.UNHANDLED
                    playTrack(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
                    val track = state.collections.favorites.getOrNull(s.homeFavoritesCursor) ?: return EventResult.UNHANDLED
                    addToQueue(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
                    val track = state.collections.favorites.getOrNull(s.homeFavoritesCursor) ?: return EventResult.UNHANDLED
                    toggleFavorite(track)
                    return EventResult.HANDLED
                }

                event.code() == KeyCode.CHAR && (event.character() == 'm' || event.character() == 'o') -> {
                    val track = state.collections.favorites.getOrNull(s.homeFavoritesCursor)
                    if (track != null) openTrackOptions(track)
                    return EventResult.HANDLED
                }
            }
        }
    }

    return EventResult.UNHANDLED
}

val NAV_SECTIONS = listOf(
    SidebarSection.HOME,
    SidebarSection.SEARCH,
    SidebarSection.LIBRARY,
    SidebarSection.NOW_PLAYING,
)
private val UTIL_SECTIONS = listOf(
    SidebarSection.STATS,
    SidebarSection.SETTINGS,
)

internal fun MeloScreen.handleSidebarKey(event: KeyEvent): EventResult {
    if (state.isSettingsVisible) return EventResult.HANDLED
    val isFocused = appRunner()?.focusManager()?.focusedId() == "sidebar-panel"
    if (!isFocused) return EventResult.UNHANDLED

    when {
        event.matches(Actions.MOVE_UP) -> {
            if (state.navigation.sidebarInUtil) {
                val idx = sidebarUtilList.selected()
                if (idx > 0) {
                    sidebarUtilList.selected(idx - 1)
                } else {
                    sidebarNavList.selected(NAV_SECTIONS.lastIndex)
                    sidebarUtilList.selected(-1)
                    state = state.copy(navigation = state.navigation.copy(sidebarInUtil = false))
                }
            } else {
                sidebarNavList.selected(maxOf(0, sidebarNavList.selected() - 1))
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) -> {
            if (!state.navigation.sidebarInUtil) {
                val idx = sidebarNavList.selected()
                if (idx < NAV_SECTIONS.lastIndex) {
                    sidebarNavList.selected(idx + 1)
                } else {
                    sidebarUtilList.selected(0)
                    sidebarNavList.selected(-1)
                    state = state.copy(navigation = state.navigation.copy(sidebarInUtil = true))
                }
            } else {
                sidebarUtilList.selected(minOf(UTIL_SECTIONS.lastIndex, sidebarUtilList.selected() + 1))
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.SELECT) -> return applySidebarSelection()
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.applySidebarSelection(): EventResult {
    val section = if (state.navigation.sidebarInUtil) {
        UTIL_SECTIONS.getOrNull(sidebarUtilList.selected())
    } else {
        NAV_SECTIONS.getOrNull(sidebarNavList.selected())
    }
    
    if (section == SidebarSection.SETTINGS) {
        state = state.copy(isSettingsVisible = true)
        appRunner()?.focusManager()?.setFocus("settings-panel")
        return EventResult.HANDLED
    }

    if (section != null && section != state.navigation.activeSection) {
        state = state.copy(needsGraphicsClear = true, navigation = state.navigation.copy(pendingSection = section))
        if (section == SidebarSection.STATS) loadStats()
    }
    return EventResult.HANDLED
}

internal fun MeloScreen.handleResultsKey(event: KeyEvent): EventResult {
    // Overlay intercepts keys from any screen
    when (state.playlistInteraction.playlistInputMode) {
        PlaylistInputMode.CREATE,
        PlaylistInputMode.RENAME -> return handlePlaylistInput(event)

        PlaylistInputMode.PICKER -> return handlePlaylistPicker(event)
        PlaylistInputMode.NONE -> {}
    }

    val s = state.screen as? ScreenState.Search ?: return EventResult.UNHANDLED
    if (s.results.isEmpty()) return EventResult.UNHANDLED
    val isFocused = appRunner()?.focusManager()?.focusedId() == "results-panel"
    when {
        event.matches(Actions.MOVE_DOWN) -> {
            if (!isFocused) return EventResult.UNHANDLED
            val newIndex = minOf(s.results.lastIndex, s.selectedIndex + 1)
            resultList.selected(newIndex)
            s.results.getOrNull(newIndex)?.let { track ->
                state = state.copy(
                    screen = s.copy(selectedIndex = newIndex), 
                    detail = state.detail.copy(selectedTrack = track), 
                    player = state.player.copy(marqueeOffset = 0)
                )
                marqueeTick = 0
                debouncedLoadDetails(track)
            }
            if (newIndex >= s.results.size - 5 && !s.isLoadingMore && s.hasMore) loadMore()
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            if (!isFocused) return EventResult.UNHANDLED
            val newIndex = maxOf(0, s.selectedIndex - 1)
            resultList.selected(newIndex)
            s.results.getOrNull(newIndex)?.let { track ->
                state = state.copy(
                    screen = s.copy(selectedIndex = newIndex), 
                    detail = state.detail.copy(selectedTrack = track), 
                    player = state.player.copy(marqueeOffset = 0)
                )
                marqueeTick = 0
                debouncedLoadDetails(track)
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            if (!isFocused) return EventResult.UNHANDLED
            val selected = s.results.getOrNull(resultList.selected()) ?: return EventResult.UNHANDLED
            playTrack(selected)
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
            s.results.getOrNull(s.selectedIndex)?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
            s.results.getOrNull(s.selectedIndex)?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings) -> {
            val track = s.results.getOrNull(s.selectedIndex)
            if (track != null) openPlaylistPicker(track)
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.LYRICS, settingsViewState.currentSettings) -> {
            loadLyrics()
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && (event.character() == 'm' || event.character() == 'o') -> {
            val track = s.results.getOrNull(s.selectedIndex)
            if (track != null) openTrackOptions(track)
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
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

internal fun MeloScreen.handleLibraryKey(event: KeyEvent): EventResult {
    when (state.playlistInteraction.playlistInputMode) {
        PlaylistInputMode.CREATE,
        PlaylistInputMode.RENAME -> return handlePlaylistInput(event)

        PlaylistInputMode.PICKER -> return handlePlaylistPicker(event)
        PlaylistInputMode.NONE -> {}
    }

    val s = state.screen as? ScreenState.Library ?: return EventResult.UNHANDLED
    val isFocused = appRunner()?.focusManager()?.focusedId() == "library-panel"
    if (!isFocused) return EventResult.UNHANDLED

    if (event.code() == KeyCode.CHAR && event.character() == '1') {
        updateScreen<ScreenState.Library> { it.copy(libraryTab = LibraryTab.FAVORITES) }
        return EventResult.HANDLED
    }
    if (event.code() == KeyCode.CHAR && event.character() == '2') {
        updateScreen<ScreenState.Library> { it.copy(libraryTab = LibraryTab.PLAYLISTS, isInPlaylistDetail = false) }
        return EventResult.HANDLED
    }

    return when (s.libraryTab) {
        LibraryTab.FAVORITES -> handleFavoritesKey(event)
        LibraryTab.PLAYLISTS -> if (s.isInPlaylistDetail) handlePlaylistDetailKey(event) else handlePlaylistsKey(event)
    }
}

internal fun MeloScreen.handleFavoritesKey(event: KeyEvent): EventResult {
    when {
        event.matches(Actions.MOVE_DOWN) -> {
            favoritesList.selected(minOf(state.collections.favorites.lastIndex.coerceAtLeast(0), favoritesList.selected() + 1))
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            favoritesList.selected(maxOf(0, favoritesList.selected() - 1))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            state.collections.favorites.getOrNull(favoritesList.selected())?.let { playTrack(it) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
            state.collections.favorites.getOrNull(favoritesList.selected())?.let { removeFavoriteTrack(it) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
            state.collections.favorites.getOrNull(favoritesList.selected())?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings) -> {
            val track = state.collections.favorites.getOrNull(favoritesList.selected())
            if (track != null) openPlaylistPicker(track)
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handleQueueKey(event: KeyEvent): EventResult {
    val isFocused = appRunner()?.focusManager()?.focusedId() == "queue-panel"
    when {
        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(player = state.player.copy(isQueueVisible = false))
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) -> {
            if (!isFocused) return EventResult.UNHANDLED
            val newCursor = minOf(state.player.queue.lastIndex, state.player.queueCursor + 1)
            state = state.copy(player = state.player.copy(queueCursor = newCursor))

            if (state.player.isRadioMode && !state.player.isLoadingMoreRadio && newCursor >= state.player.queue.size - 5) {
                loadMoreRadioTracks()
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            if (!isFocused) return EventResult.UNHANDLED
            state = state.copy(player = state.player.copy(queueCursor = maxOf(0, state.player.queueCursor - 1)))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            if (!isFocused) return EventResult.UNHANDLED
            if (state.player.queue.getOrNull(state.player.queueCursor) != null) playFromQueue(state.player.queueCursor)
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.DELETE, settingsViewState.currentSettings) || (event.code() == KeyCode.CHAR && event.character() == 'd') -> {
            if (!isFocused) return EventResult.UNHANDLED
            removeFromQueue(state.player.queueCursor)
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.CLEAR_QUEUE, settingsViewState.currentSettings) -> {
            clearQueue()
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handlePlayerBarKey(event: KeyEvent): EventResult {
    if (state.isSettingsVisible || state.trackOptions.isVisible) return EventResult.HANDLED
    val settings = settingsViewState.currentSettings
    when {
        event.matches(Actions.MOVE_LEFT) -> {
            seekTo(state.player.progress - 0.05); return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_RIGHT) -> {
            seekTo(state.player.progress + 0.05); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.PREVIOUS, settings) -> {
            seekBackward(); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.NEXT, settings) -> {
            seekForward(); return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && (event.character() == '<' || event.character() == ',') -> {
            seekTo(state.player.progress - 0.05); return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && (event.character() == '>' || event.character() == '.') -> {
            seekTo(state.player.progress + 0.05); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.PLAY_PAUSE, settings) || event.code() == KeyCode.CHAR && event.character() == ' ' -> {
            togglePlayPause(); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.TOGGLE_QUEUE, settings) -> {
            toggleQueue(); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.REPEAT, settings) -> {
            cycleRepeat(); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.SHUFFLE, settings) -> {
            toggleShuffle(); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.VOLUME_UP, settings) -> {
            adjustVolume(5); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.VOLUME_DOWN, settings) -> {
            adjustVolume(-5); return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handleTrackOptionsKey(event: KeyEvent): EventResult {
    val optionsCount = 5
    when {
        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(trackOptions = state.trackOptions.copy(isVisible = false))
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) -> {
            val newIndex = (state.trackOptions.selectedIndex + 1) % optionsCount
            state = state.copy(trackOptions = state.trackOptions.copy(selectedIndex = newIndex))
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            val newIndex = if (state.trackOptions.selectedIndex <= 0) optionsCount - 1 else state.trackOptions.selectedIndex - 1
            state = state.copy(trackOptions = state.trackOptions.copy(selectedIndex = newIndex))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            val track = state.trackOptions.track ?: return EventResult.UNHANDLED
            val actionIndex = state.trackOptions.selectedIndex
            state = state.copy(trackOptions = state.trackOptions.copy(isVisible = false))
            
            when (actionIndex) {
                0 -> playTrack(track)
                1 -> addToQueue(track)
                2 -> toggleFavorite(track)
                3 -> openPlaylistPicker(track)
                4 -> {
                    state = state.copy(detail = state.detail.copy(selectedTrack = track, detailTab = DetailTab.SIMILAR))
                    appRunner()?.focusManager()?.setFocus("similar-area")
                    loadMoreSimilar()
                }
            }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.openTrackOptions(track: Track) {
    state = state.copy(trackOptions = state.trackOptions.copy(track = track, selectedIndex = 0, isVisible = true))
    appRunner()?.focusManager()?.setFocus("track-options-panel")
}

internal fun MeloScreen.handleGlobalShortcuts(event: KeyEvent): EventResult {
    if (state.isSettingsVisible || state.trackOptions.isVisible) return EventResult.UNHANDLED
    
    // Only handle if NOT in search input
    val focusedId = appRunner()?.focusManager()?.focusedId()
    if (focusedId == "search-bar") return EventResult.UNHANDLED

    if (event.code() == KeyCode.CHAR) {
        val section = when (event.character()) {
            '1' -> SidebarSection.HOME
            '2' -> SidebarSection.SEARCH
            '3' -> SidebarSection.LIBRARY
            '4' -> SidebarSection.NOW_PLAYING
            '5' -> SidebarSection.STATS
            '6' -> SidebarSection.SETTINGS
            else -> null
        }
        
        if (section != null) {
            if (section == SidebarSection.SETTINGS) {
                state = state.copy(isSettingsVisible = true)
                appRunner()?.focusManager()?.setFocus("settings-panel")
            } else if (section != state.navigation.activeSection) {
                state = state.copy(needsGraphicsClear = true, navigation = state.navigation.copy(pendingSection = section))
                if (section == SidebarSection.STATS) loadStats()
            }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

