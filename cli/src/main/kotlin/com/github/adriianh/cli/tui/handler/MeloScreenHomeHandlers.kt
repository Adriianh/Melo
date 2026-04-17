package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.HomeSection
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.core.domain.model.MeloAction
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

/**
 * Handles key events for the Home screen, including Recent and Favorites sections.
 */
internal fun MeloScreen.handleHomeKey(event: KeyEvent): EventResult {
    val s = state.screen as? ScreenState.Home ?: return handleGlobalShortcuts(event)
    val focusedId = appRunner()?.focusManager()?.focusedId()
    val isFocused = focusedId == "home-panel"
            || focusedId == "home-recent-panel"
            || focusedId == "home-favorites-panel"
    if (!isFocused) return handleGlobalShortcuts(event)

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
                    updateScreen<ScreenState.Home> {
                        it.copy(
                            homeRecentCursor = minOf(
                                maxIndex,
                                it.homeRecentCursor + 1
                            )
                        )
                    }
                    return EventResult.HANDLED
                }

                event.matches(Actions.MOVE_UP) -> {
                    updateScreen<ScreenState.Home> { it.copy(homeRecentCursor = maxOf(0, it.homeRecentCursor - 1)) }
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
                    updateScreen<ScreenState.Home> {
                        it.copy(
                            homeFavoritesCursor = minOf(
                                maxIndex,
                                it.homeFavoritesCursor + 1
                            )
                        )
                    }
                    return EventResult.HANDLED
                }

                event.matches(Actions.MOVE_UP) -> {
                    updateScreen<ScreenState.Home> {
                        it.copy(
                            homeFavoritesCursor = maxOf(
                                0,
                                it.homeFavoritesCursor - 1
                            )
                        )
                    }
                    return EventResult.HANDLED
                }

                event.code() == KeyCode.ENTER -> {
                    val track =
                        state.collections.favorites.getOrNull(s.homeFavoritesCursor)
                            ?: return handleGlobalShortcuts(event)
                    playTrack(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings) -> {
                    val track =
                        state.collections.favorites.getOrNull(s.homeFavoritesCursor)
                            ?: return handleGlobalShortcuts(event)
                    addToQueue(track)
                    return EventResult.HANDLED
                }

                event.matchesAction(MeloAction.FAVORITE, settingsViewState.currentSettings) -> {
                    val track =
                        state.collections.favorites.getOrNull(s.homeFavoritesCursor)
                            ?: return handleGlobalShortcuts(event)
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

    return handleGlobalShortcuts(event)
}
