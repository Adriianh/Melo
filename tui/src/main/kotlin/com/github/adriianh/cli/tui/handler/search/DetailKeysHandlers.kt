package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.handleGlobalShortcuts
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

internal fun MeloScreen.returnFocusFromDetail() {
    val target = when (val curScreen = state.screen) {
        is ScreenState.Search -> "results-panel"
        is ScreenState.Home -> "home-panel"
        is ScreenState.Library -> "library-panel"
        is ScreenState.Offline -> "offline-panel"
        is ScreenState.Stats -> "stats-panel"
        is ScreenState.EntityDetail -> {
            if (curScreen.entity is SearchResult.Artist) "artist-dashboard-list" else "entity-tracks-list"
        }

        else -> "sidebar-nav"
    }
    appRunner()?.focusManager()?.setFocus(target)
}

/** Carga las letras de la canción seleccionada si aún no están disponibles. */
internal fun MeloScreen.loadLyricsIfNeeded() {
    val track = state.detail.selectedTrack ?: state.player.nowPlaying
    if (track != null && state.detail.lyrics == null && state.detail.syncedLyrics.isEmpty() && !state.detail.isLoadingLyrics) {
        loadLyrics()
    }
}

/** Carga las canciones similares de la canción seleccionada si aún no están disponibles. */
internal fun MeloScreen.loadSimilarIfNeeded() {
    val track = state.detail.selectedTrack ?: state.player.nowPlaying
    if (track != null && state.detail.similarTracks.isEmpty() && !state.detail.isLoadingSimilar) {
        loadTrackDetails(track.id, track)
    }
}

/** Cambios de pestaña y devolución de foco del panel de detalle. */
internal fun MeloScreen.handleDetailTabNavigationKey(event: KeyEvent): EventResult {
    when {
        event.code() == KeyCode.ESCAPE || event.code() == KeyCode.TAB -> {
            returnFocusFromDetail()
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('i') -> {
            state = state.copy(detail = state.detail.copy(detailTab = DetailTab.INFO))
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('l') -> {
            state = state.copy(
                detail = state.detail.copy(
                    detailTab = DetailTab.LYRICS,
                    lyricsScrollOffset = 0,
                    isAutoScrollLyrics = true
                )
            )
            loadLyricsIfNeeded()
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('s') -> {
            state = state.copy(detail = state.detail.copy(detailTab = DetailTab.SIMILAR))
            loadSimilarIfNeeded()
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_LEFT) -> {
            val tabs = DetailTab.entries
            val prevOrdinal = (state.detail.detailTab.ordinal - 1 + tabs.size) % tabs.size
            val nextTab = tabs[prevOrdinal]
            state = state.copy(
                detail = state.detail.copy(
                    detailTab = nextTab,
                    lyricsScrollOffset = 0,
                    isAutoScrollLyrics = true
                )
            )
            if (nextTab == DetailTab.LYRICS) {
                loadLyricsIfNeeded()
            } else if (nextTab == DetailTab.SIMILAR) {
                loadSimilarIfNeeded()
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_RIGHT) -> {
            val tabs = DetailTab.entries
            val nextOrdinal = (state.detail.detailTab.ordinal + 1) % tabs.size
            val nextTab = tabs[nextOrdinal]
            state = state.copy(
                detail = state.detail.copy(
                    detailTab = nextTab,
                    lyricsScrollOffset = 0,
                    isAutoScrollLyrics = true
                )
            )
            if (nextTab == DetailTab.LYRICS) {
                loadLyricsIfNeeded()
            } else if (nextTab == DetailTab.SIMILAR) {
                loadSimilarIfNeeded()
            }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handleDetailKey(event: KeyEvent): EventResult {
    if (state.languagePicker.isVisible) return handleLanguagePickerKey(event)
    val focusedId = appRunner()?.focusManager()?.focusedId()
    if (focusedId != "detail-panel") {
        return handleGlobalShortcuts(event)
    }

    if (handleDetailTabNavigationKey(event) == EventResult.HANDLED) return EventResult.HANDLED

    val handled = when (state.detail.detailTab) {
        DetailTab.LYRICS -> handleLyricsTabKey(event)
        DetailTab.SIMILAR -> handleSimilarTabKey(event)
        DetailTab.INFO -> handleInfoTabKey(event)
    }
    return if (handled == EventResult.HANDLED) handled else handleGlobalShortcuts(event)
}