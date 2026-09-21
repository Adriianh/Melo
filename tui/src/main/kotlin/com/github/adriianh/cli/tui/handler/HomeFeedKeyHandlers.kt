package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.HomeFeedFocus
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openBatchOptions
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.cli.tui.handler.search.openEntityDetails
import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

/** Salta a la sección anterior (direction < 0) o siguiente del feed y actualiza el foco. */
private fun MeloScreen.jumpHomeFeedSection(s: ScreenState.Home, direction: Int): EventResult {
    if (s.feedSections.isNotEmpty()) {
        val nextSection = if (direction < 0) {
            if (s.selectedSectionIndex <= 0) s.feedSections.size - 1 else s.selectedSectionIndex - 1
        } else {
            (s.selectedSectionIndex + 1) % s.feedSections.size
        }
        updateScreen<ScreenState.Home> {
            it.copy(selectedSectionIndex = nextSection, selectedItemIndex = 0)
        }
        enrichActiveSectionTracks()
        if (direction > 0 && nextSection >= s.feedSections.size - 2 && s.feedContinuation != null) {
            loadMoreHomeSections()
        }
    }
    return EventResult.HANDLED
}

private fun MeloScreen.handleHomeFeedChipsKey(
    s: ScreenState.Home,
    event: KeyEvent
): EventResult {
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
    return EventResult.UNHANDLED
}

private fun MeloScreen.handleHomeFeedSectionsKey(
    s: ScreenState.Home,
    event: KeyEvent
): EventResult {
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
    return EventResult.UNHANDLED
}

private fun MeloScreen.handleHomeFeedItemsKey(
    s: ScreenState.Home,
    event: KeyEvent,
    currentSection: HomeSection?
): EventResult {
    val itemsCount = currentSection?.items?.size ?: 0
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
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handleHomeFeedKey(
    s: ScreenState.Home,
    event: KeyEvent
): EventResult {
    // Direct section jumps with [ and ] work anywhere in feed
    if (event.isChar('[')) return jumpHomeFeedSection(s, -1)
    if (event.isChar(']')) return jumpHomeFeedSection(s, +1)

    val currentSection = s.feedSections.getOrNull(s.selectedSectionIndex)

    return when (s.feedFocus) {
        HomeFeedFocus.CHIPS -> handleHomeFeedChipsKey(s, event)
        HomeFeedFocus.SECTIONS -> handleHomeFeedSectionsKey(s, event)
        HomeFeedFocus.ITEMS -> handleHomeFeedItemsKey(s, event, currentSection)
    }
}