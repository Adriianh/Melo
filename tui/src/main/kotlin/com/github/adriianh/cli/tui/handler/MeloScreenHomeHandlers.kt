package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.HomeFeedFocus
import com.github.adriianh.cli.tui.HomeTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.allLibraryFavorites
import com.github.adriianh.cli.tui.allRecentTracks
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openBatchOptions
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.cli.tui.handler.search.openEntityDetails
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

/**
 * Handles key events for the Home screen: Feed (recommendations), Recent, and Favorites.
 */
internal fun MeloScreen.handleHomeKey(event: KeyEvent): EventResult {
    val s = state.screen as? ScreenState.Home ?: return handleGlobalShortcuts(event)

    if (event.isChar('1')) {
        updateScreen<ScreenState.Home> { it.copy(homeTab = HomeTab.FEED) }
        return EventResult.HANDLED
    }
    if (event.isChar('2')) {
        updateScreen<ScreenState.Home> { it.copy(homeTab = HomeTab.RECENT) }
        return EventResult.HANDLED
    }
    if (event.isChar('3')) {
        updateScreen<ScreenState.Home> { it.copy(homeTab = HomeTab.FAVORITES) }
        return EventResult.HANDLED
    }
    if (event.isCharIgnoreCase('r')) {
        when (s.homeTab) {
            HomeTab.FEED -> loadHomeFeed()
            HomeTab.RECENT -> syncYouTubeHistory()
            HomeTab.FAVORITES -> syncYouTubeFavorites()
        }
        return EventResult.HANDLED
    }

    if (event.code() == KeyCode.TAB) {
        if (s.homeTab == HomeTab.FEED) {
            val nextFocus = when (s.feedFocus) {
                HomeFeedFocus.SECTIONS -> HomeFeedFocus.ITEMS
                HomeFeedFocus.ITEMS -> if (s.feedChips.isNotEmpty()) HomeFeedFocus.CHIPS else HomeFeedFocus.SECTIONS
                HomeFeedFocus.CHIPS -> HomeFeedFocus.SECTIONS
            }
            updateScreen<ScreenState.Home> { it.copy(feedFocus = nextFocus) }
            return EventResult.HANDLED
        } else {
            val nextTab = when (s.homeTab) {
                HomeTab.RECENT -> HomeTab.FAVORITES
                HomeTab.FAVORITES -> HomeTab.FEED
            }
            updateScreen<ScreenState.Home> { it.copy(homeTab = nextTab) }
            if (nextTab == HomeTab.FEED) {
                enrichActiveSectionTracks()
            }
            return EventResult.HANDLED
        }
    }

    when (s.homeTab) {
        HomeTab.FEED -> {
            val currentSection = s.feedSections.getOrNull(s.selectedSectionIndex)
            val itemsCount = currentSection?.items?.size ?: 0

            // Direct section jumps with [ and ] work anywhere in feed
            if (event.isChar('[')) {
                if (s.feedSections.isNotEmpty()) {
                    val prevSection =
                        if (s.selectedSectionIndex <= 0) s.feedSections.size - 1 else s.selectedSectionIndex - 1
                    updateScreen<ScreenState.Home> {
                        it.copy(selectedSectionIndex = prevSection, selectedItemIndex = 0)
                    }
                    enrichActiveSectionTracks()
                }
                return EventResult.HANDLED
            }
            if (event.isChar(']')) {
                if (s.feedSections.isNotEmpty()) {
                    val nextSection = (s.selectedSectionIndex + 1) % s.feedSections.size
                    updateScreen<ScreenState.Home> {
                        it.copy(selectedSectionIndex = nextSection, selectedItemIndex = 0)
                    }
                    enrichActiveSectionTracks()
                    if (nextSection >= s.feedSections.size - 2 && s.feedContinuation != null) {
                        loadMoreHomeSections()
                    }
                }
                return EventResult.HANDLED
            }

            when (s.feedFocus) {
                HomeFeedFocus.CHIPS -> {
                    val totalChips = s.feedChips.size + 1
                    when {
                        event.matches(Actions.MOVE_LEFT) -> {
                            val prev = (s.selectedChipIndex - 1 + totalChips) % totalChips
                            updateScreen<ScreenState.Home> { it.copy(selectedChipIndex = prev) }
                            return EventResult.HANDLED
                        }

                        event.matches(Actions.MOVE_RIGHT) -> {
                            val next = (s.selectedChipIndex + 1) % totalChips
                            updateScreen<ScreenState.Home> { it.copy(selectedChipIndex = next) }
                            return EventResult.HANDLED
                        }

                        event.code() == KeyCode.ENTER -> {
                            val chipParam =
                                if (s.selectedChipIndex == 0) null else s.feedChips.getOrNull(s.selectedChipIndex - 1)?.params
                            loadHomeFeed(chipParam, s.selectedChipIndex)
                            updateScreen<ScreenState.Home> { it.copy(feedFocus = HomeFeedFocus.ITEMS) }
                            return EventResult.HANDLED
                        }

                        event.matches(Actions.MOVE_DOWN) || event.code() == KeyCode.ESCAPE -> {
                            updateScreen<ScreenState.Home> { it.copy(feedFocus = HomeFeedFocus.SECTIONS) }
                            return EventResult.HANDLED
                        }
                    }
                }

                HomeFeedFocus.SECTIONS -> {
                    when {
                        event.matches(Actions.MOVE_DOWN) -> {
                            if (s.feedSections.isNotEmpty()) {
                                val next = (s.selectedSectionIndex + 1) % s.feedSections.size
                                updateScreen<ScreenState.Home> {
                                    it.copy(selectedSectionIndex = next, selectedItemIndex = 0)
                                }
                                enrichActiveSectionTracks()
                                if (next >= s.feedSections.size - 2 && s.feedContinuation != null) {
                                    loadMoreHomeSections()
                                }
                            }
                            return EventResult.HANDLED
                        }

                        event.matches(Actions.MOVE_UP) -> {
                            if (s.selectedSectionIndex == 0 && s.feedChips.isNotEmpty()) {
                                updateScreen<ScreenState.Home> { it.copy(feedFocus = HomeFeedFocus.CHIPS) }
                                return EventResult.HANDLED
                            }
                            if (s.feedSections.isNotEmpty()) {
                                val prev =
                                    (s.selectedSectionIndex - 1 + s.feedSections.size) % s.feedSections.size
                                updateScreen<ScreenState.Home> {
                                    it.copy(selectedSectionIndex = prev, selectedItemIndex = 0)
                                }
                                enrichActiveSectionTracks()
                            }
                            return EventResult.HANDLED
                        }

                        event.matches(Actions.MOVE_RIGHT) || event.code() == KeyCode.ENTER || event.isCharIgnoreCase(
                            'l'
                        ) -> {
                            updateScreen<ScreenState.Home> { it.copy(feedFocus = HomeFeedFocus.ITEMS) }
                            return EventResult.HANDLED
                        }
                    }
                }

                HomeFeedFocus.ITEMS -> {
                    when {
                        event.matches(Actions.MOVE_LEFT) || event.isCharIgnoreCase('h') || event.code() == KeyCode.ESCAPE -> {
                            updateScreen<ScreenState.Home> { it.copy(feedFocus = HomeFeedFocus.SECTIONS) }
                            return EventResult.HANDLED
                        }

                        event.matches(Actions.MOVE_DOWN) -> {
                            if (itemsCount > 0) {
                                updateScreen<ScreenState.Home> {
                                    it.copy(
                                        selectedItemIndex = minOf(
                                            itemsCount - 1,
                                            it.selectedItemIndex + 1
                                        )
                                    )
                                }
                            }
                            return EventResult.HANDLED
                        }

                        event.matches(Actions.MOVE_UP) -> {
                            if (itemsCount > 0) {
                                updateScreen<ScreenState.Home> {
                                    it.copy(selectedItemIndex = maxOf(0, it.selectedItemIndex - 1))
                                }
                            }
                            return EventResult.HANDLED
                        }

                        event.code() == KeyCode.ENTER -> {
                            val item = currentSection?.items?.getOrNull(s.selectedItemIndex)
                                ?: return handleGlobalShortcuts(event)
                            when (item) {
                                is SearchResult.Song -> {
                                    playTrack(item.track)
                                    return EventResult.HANDLED
                                }

                                is SearchResult.Album, is SearchResult.Playlist, is SearchResult.Artist -> {
                                    openEntityDetails(item)
                                    return EventResult.HANDLED
                                }
                            }
                        }

                        event.matchesAction(
                            MeloAction.ADD_TO_QUEUE,
                            settingsViewState.currentSettings
                        ) -> {
                            val item = currentSection?.items?.getOrNull(s.selectedItemIndex)
                            if (item is SearchResult.Song) {
                                addToQueue(item.track)
                                return EventResult.HANDLED
                            }
                        }

                        event.matchesAction(
                            MeloAction.FAVORITE,
                            settingsViewState.currentSettings
                        ) -> {
                            val item = currentSection?.items?.getOrNull(s.selectedItemIndex)
                            if (item is SearchResult.Song) {
                                toggleFavorite(item.track)
                                return EventResult.HANDLED
                            }
                        }

                        event.isCharIgnoreCase('v') || event.matchesAction(
                            MeloAction.TOGGLE_SELECTION,
                            settingsViewState.currentSettings
                        ) || (state.selection.isNotEmpty && event.isChar(' ')) -> {
                            val item = currentSection?.items?.getOrNull(s.selectedItemIndex)
                            if (item is SearchResult.Song) {
                                state = state.copy(selection = state.selection.toggle(item.track))
                                return EventResult.HANDLED
                            }
                        }

                        event.isCtrlA() -> {
                            val allSongs =
                                currentSection?.items?.filterIsInstance<SearchResult.Song>()
                                    ?.map { it.track }.orEmpty()
                            if (allSongs.isNotEmpty()) {
                                state = state.copy(selection = state.selection.selectAll(allSongs))
                                return EventResult.HANDLED
                            }
                        }

                        event.matchesAction(
                            MeloAction.ADD_PLAYLIST,
                            settingsViewState.currentSettings
                        ) -> {
                            if (state.selection.isNotEmpty) {
                                openPlaylistPicker(state.selection.tracks())
                                return EventResult.HANDLED
                            }
                            val item = currentSection?.items?.getOrNull(s.selectedItemIndex)
                            if (item is SearchResult.Song) {
                                openPlaylistPicker(item.track)
                                return EventResult.HANDLED
                            }
                        }

                        event.isCharIgnoreCase('m') || event.matchesAction(
                            MeloAction.TRACK_OPTIONS,
                            settingsViewState.currentSettings
                        ) -> {
                            if (state.selection.isNotEmpty) {
                                openBatchOptions(state.selection.tracks())
                                return EventResult.HANDLED
                            }
                            val item = currentSection?.items?.getOrNull(s.selectedItemIndex)
                            if (item is SearchResult.Song) {
                                openTrackOptions(item.track)
                                return EventResult.HANDLED
                            }
                        }
                    }
                }
            }
        }

        HomeTab.RECENT -> {
            val recent = state.allRecentTracks()
            val maxIndex = (recent.size - 1).coerceAtLeast(0)
            when {
                event.matches(Actions.MOVE_DOWN) -> {
                    updateScreen<ScreenState.Home> {
                        it.copy(homeRecentCursor = minOf(maxIndex, it.homeRecentCursor + 1))
                    }
                    return EventResult.HANDLED
                }

                event.matches(Actions.MOVE_UP) -> {
                    updateScreen<ScreenState.Home> {
                        it.copy(homeRecentCursor = maxOf(0, it.homeRecentCursor - 1))
                    }
                    return EventResult.HANDLED
                }

                event.code() == KeyCode.ENTER -> {
                    val track = recent.getOrNull(s.homeRecentCursor)?.track
                        ?: return handleGlobalShortcuts(event)
                    playTrack(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
                    val track = recent.getOrNull(s.homeRecentCursor)?.track
                        ?: return handleGlobalShortcuts(event)
                    addToQueue(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
                    val track = recent.getOrNull(s.homeRecentCursor)?.track
                        ?: return handleGlobalShortcuts(event)
                    toggleFavorite(track)
                    return EventResult.HANDLED
                }

                event.isCharIgnoreCase('v') || event.matchesAction(
                    MeloAction.TOGGLE_SELECTION,
                    settingsViewState.currentSettings
                ) || (state.selection.isNotEmpty && event.isChar(' ')) -> {
                    val track = recent.getOrNull(s.homeRecentCursor)?.track
                    if (track != null) {
                        state = state.copy(selection = state.selection.toggle(track))
                        return EventResult.HANDLED
                    }
                }

                event.isCtrlA() -> {
                    val tracks = recent.map { it.track }
                    if (tracks.isNotEmpty()) {
                        state = state.copy(selection = state.selection.selectAll(tracks))
                        return EventResult.HANDLED
                    }
                }

                event.matchesAction(
                    MeloAction.ADD_PLAYLIST,
                    settingsViewState.currentSettings
                ) -> {
                    if (state.selection.isNotEmpty) {
                        openPlaylistPicker(state.selection.tracks())
                    } else {
                        val track =
                            recent.getOrNull(s.homeRecentCursor)?.track
                        if (track != null) openPlaylistPicker(track)
                    }
                    return EventResult.HANDLED
                }

                event.isCharIgnoreCase('m') || event.matchesAction(
                    MeloAction.TRACK_OPTIONS,
                    settingsViewState.currentSettings
                ) -> {
                    if (state.selection.isNotEmpty) {
                        openBatchOptions(state.selection.tracks())
                        return EventResult.HANDLED
                    }
                    val track = recent.getOrNull(s.homeRecentCursor)?.track
                    if (track != null) openTrackOptions(track)
                    return EventResult.HANDLED
                }
            }
        }

        HomeTab.FAVORITES -> {
            val allFavorites = state.allLibraryFavorites()
            val maxIndex = (allFavorites.size - 1).coerceAtLeast(0)
            when {
                event.matches(Actions.MOVE_DOWN) -> {
                    updateScreen<ScreenState.Home> {
                        it.copy(homeFavoritesCursor = minOf(maxIndex, it.homeFavoritesCursor + 1))
                    }
                    return EventResult.HANDLED
                }

                event.matches(Actions.MOVE_UP) -> {
                    updateScreen<ScreenState.Home> {
                        it.copy(homeFavoritesCursor = maxOf(0, it.homeFavoritesCursor - 1))
                    }
                    return EventResult.HANDLED
                }

                event.code() == KeyCode.ENTER -> {
                    val track = allFavorites.getOrNull(s.homeFavoritesCursor)?.track
                        ?: return handleGlobalShortcuts(event)
                    playTrack(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
                    val track = allFavorites.getOrNull(s.homeFavoritesCursor)?.track
                        ?: return handleGlobalShortcuts(event)
                    addToQueue(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
                    val track = allFavorites.getOrNull(s.homeFavoritesCursor)?.track
                        ?: return handleGlobalShortcuts(event)
                    toggleFavorite(track)
                    return EventResult.HANDLED
                }

                event.isCharIgnoreCase('v') || event.matchesAction(
                    MeloAction.TOGGLE_SELECTION,
                    settingsViewState.currentSettings
                ) || (state.selection.isNotEmpty && event.isChar(' ')) -> {
                    val track = allFavorites.getOrNull(s.homeFavoritesCursor)?.track
                    if (track != null) {
                        state = state.copy(selection = state.selection.toggle(track))
                        return EventResult.HANDLED
                    }
                }

                event.isCtrlA() -> {
                    val tracks = allFavorites.map { it.track }
                    if (tracks.isNotEmpty()) {
                        state = state.copy(selection = state.selection.selectAll(tracks))
                        return EventResult.HANDLED
                    }
                }

                event.matchesAction(
                    MeloAction.ADD_PLAYLIST,
                    settingsViewState.currentSettings
                ) -> {
                    if (state.selection.isNotEmpty) {
                        openPlaylistPicker(state.selection.tracks())
                    } else {
                        val track = allFavorites.getOrNull(s.homeFavoritesCursor)?.track
                        if (track != null) openPlaylistPicker(track)
                    }
                    return EventResult.HANDLED
                }

                event.isCharIgnoreCase('m') || event.matchesAction(
                    MeloAction.TRACK_OPTIONS,
                    settingsViewState.currentSettings
                ) -> {
                    if (state.selection.isNotEmpty) {
                        openBatchOptions(state.selection.tracks())
                        return EventResult.HANDLED
                    }
                    val track = allFavorites.getOrNull(s.homeFavoritesCursor)?.track
                    if (track != null) openTrackOptions(track)
                    return EventResult.HANDLED
                }
            }
        }
    }

    return handleGlobalShortcuts(event)
}