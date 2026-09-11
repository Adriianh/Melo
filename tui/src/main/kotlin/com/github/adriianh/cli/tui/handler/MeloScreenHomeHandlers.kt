package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.HomeTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SidebarSection
import com.github.adriianh.cli.tui.handler.playback.addToQueue
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
        loadHomeFeed()
        return EventResult.HANDLED
    }

    if (event.code() == KeyCode.TAB) {
        val nextTab = when (s.homeTab) {
            HomeTab.FEED -> HomeTab.RECENT
            HomeTab.RECENT -> HomeTab.FAVORITES
            HomeTab.FAVORITES -> HomeTab.FEED
        }
        updateScreen<ScreenState.Home> { it.copy(homeTab = nextTab) }
        return EventResult.HANDLED
    }

    when (s.homeTab) {
        HomeTab.FEED -> {
            val currentSection = s.feedSections.getOrNull(s.selectedSectionIndex)
            val itemsCount = currentSection?.items?.size ?: 0

            when {
                event.matches(Actions.MOVE_LEFT) || event.isChar('[') -> {
                    if (s.feedSections.isNotEmpty()) {
                        val prevSection =
                            if (s.selectedSectionIndex <= 0) s.feedSections.size - 1 else s.selectedSectionIndex - 1
                        updateScreen<ScreenState.Home> {
                            it.copy(selectedSectionIndex = prevSection, selectedItemIndex = 0)
                        }
                    }
                    return EventResult.HANDLED
                }

                event.matches(Actions.MOVE_RIGHT) || event.isChar(']') -> {
                    if (s.feedSections.isNotEmpty()) {
                        val nextSection = (s.selectedSectionIndex + 1) % s.feedSections.size
                        updateScreen<ScreenState.Home> {
                            it.copy(selectedSectionIndex = nextSection, selectedItemIndex = 0)
                        }
                    }
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
                            val searchState =
                                (state.screen as? ScreenState.Search) ?: ScreenState.Search()
                            state = state.copy(
                                navigation = state.navigation.copy(activeSection = SidebarSection.SEARCH),
                                screen = searchState
                            )
                            openEntityDetails(item)
                            return EventResult.HANDLED
                        }
                    }
                }

                event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
                    val item = currentSection?.items?.getOrNull(s.selectedItemIndex)
                    if (item is SearchResult.Song) {
                        addToQueue(item.track)
                        return EventResult.HANDLED
                    }
                }

                event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
                    val item = currentSection?.items?.getOrNull(s.selectedItemIndex)
                    if (item is SearchResult.Song) {
                        toggleFavorite(item.track)
                        return EventResult.HANDLED
                    }
                }

                event.isCharIgnoreCase('m') || event.isCharIgnoreCase('o') -> {
                    val item = currentSection?.items?.getOrNull(s.selectedItemIndex)
                    if (item is SearchResult.Song) {
                        openTrackOptions(item.track)
                        return EventResult.HANDLED
                    }
                }
            }
        }

        HomeTab.RECENT -> {
            val maxIndex = (state.collections.recentTracks.size - 1).coerceAtLeast(0)
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
                    val track = state.collections.recentTracks.getOrNull(s.homeRecentCursor)?.track
                        ?: return handleGlobalShortcuts(event)
                    playTrack(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
                    val track = state.collections.recentTracks.getOrNull(s.homeRecentCursor)?.track
                        ?: return handleGlobalShortcuts(event)
                    addToQueue(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
                    val track = state.collections.recentTracks.getOrNull(s.homeRecentCursor)?.track
                        ?: return handleGlobalShortcuts(event)
                    toggleFavorite(track)
                    return EventResult.HANDLED
                }

                event.isCharIgnoreCase('m') || event.isCharIgnoreCase('o') -> {
                    val track = state.collections.recentTracks.getOrNull(s.homeRecentCursor)?.track
                    if (track != null) openTrackOptions(track)
                    return EventResult.HANDLED
                }
            }
        }

        HomeTab.FAVORITES -> {
            val maxIndex = (state.collections.favorites.size - 1).coerceAtLeast(0)
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
                    val track = state.collections.favorites.getOrNull(s.homeFavoritesCursor)
                        ?: return handleGlobalShortcuts(event)
                    playTrack(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
                    val track = state.collections.favorites.getOrNull(s.homeFavoritesCursor)
                        ?: return handleGlobalShortcuts(event)
                    addToQueue(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
                    val track = state.collections.favorites.getOrNull(s.homeFavoritesCursor)
                        ?: return handleGlobalShortcuts(event)
                    toggleFavorite(track)
                    return EventResult.HANDLED
                }

                event.isCharIgnoreCase('m') || event.isCharIgnoreCase('o') -> {
                    val track = state.collections.favorites.getOrNull(s.homeFavoritesCursor)
                    if (track != null) openTrackOptions(track)
                    return EventResult.HANDLED
                }
            }
        }
    }

    return handleGlobalShortcuts(event)
}