package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.HomeFeedFocus
import com.github.adriianh.cli.tui.HomeTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.allLibraryFavorites
import com.github.adriianh.cli.tui.allRecentTracks
import com.github.adriianh.cli.tui.enrichActiveSectionTracks
import com.github.adriianh.cli.tui.loadHomeFeed
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openBatchOptions
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

/** Acciones comunes de una lista de tracks con cursor (pestañas RECENT y FAVORITES de Home). */
private fun MeloScreen.handleHomeCursorTrackListKey(
    event: KeyEvent,
    tracks: List<Track>,
    cursorIndex: Int,
    moveCursor: (Int) -> Unit
): EventResult {
    when {
        event.matches(Actions.MOVE_DOWN) -> {
            val maxIndex = (tracks.size - 1).coerceAtLeast(0)
            moveCursor(minOf(maxIndex, cursorIndex + 1))
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            val maxIndex = (tracks.size - 1).coerceAtLeast(0)
            moveCursor(maxOf(0, cursorIndex - 1))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            val track = tracks.getOrNull(cursorIndex) ?: return handleGlobalShortcuts(event)
            playTrack(track)
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
            val track = tracks.getOrNull(cursorIndex) ?: return handleGlobalShortcuts(event)
            addToQueue(track)
            return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
            val track = tracks.getOrNull(cursorIndex) ?: return handleGlobalShortcuts(event)
            toggleFavorite(track)
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('v') || event.matchesAction(
            MeloAction.TOGGLE_SELECTION,
            settingsViewState.currentSettings
        ) || (state.selection.isNotEmpty && event.isChar(' ')) -> {
            val track = tracks.getOrNull(cursorIndex)
            if (track != null) {
                state = state.copy(selection = state.selection.toggle(track))
                return EventResult.HANDLED
            }
        }

        event.isCtrlA() -> {
            if (tracks.isNotEmpty()) {
                state = state.copy(selection = state.selection.selectAll(tracks))
                return EventResult.HANDLED
            }
        }

        event.matchesAction(MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings) -> {
            if (state.selection.isNotEmpty) {
                openPlaylistPicker(state.selection.tracks())
            } else {
                val track = tracks.getOrNull(cursorIndex)
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
            } else {
                val track = tracks.getOrNull(cursorIndex)
                if (track != null) openTrackOptions(track)
            }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

private fun MeloScreen.handleHomeTabRecentKey(
    s: ScreenState.Home,
    event: KeyEvent
): EventResult {
    val tracks = state.allRecentTracks().map { it.track }
    return handleHomeCursorTrackListKey(
        event = event,
        tracks = tracks,
        cursorIndex = s.homeRecentCursor,
        moveCursor = { value ->
            updateScreen<ScreenState.Home> { current -> current.copy(homeRecentCursor = value) }
        }
    )
}

private fun MeloScreen.handleHomeTabFavoritesKey(
    s: ScreenState.Home,
    event: KeyEvent
): EventResult {
    val tracks = state.allLibraryFavorites().map { it.track }
    return handleHomeCursorTrackListKey(
        event = event,
        tracks = tracks,
        cursorIndex = s.homeFavoritesCursor,
        moveCursor = { value ->
            updateScreen<ScreenState.Home> { current -> current.copy(homeFavoritesCursor = value) }
        }
    )
}

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
            val handled = handleHomeFeedKey(s, event)
            if (handled == EventResult.HANDLED) return handled
        }

        HomeTab.RECENT -> {
            val handled = handleHomeTabRecentKey(s, event)
            if (handled == EventResult.HANDLED) return handled
        }

        HomeTab.FAVORITES -> {
            val handled = handleHomeTabFavoritesKey(s, event)
            if (handled == EventResult.HANDLED) return handled
        }
    }

    return handleGlobalShortcuts(event)
}