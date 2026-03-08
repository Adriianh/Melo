package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.*
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

internal fun MeloScreen.handleSearchBarKey(event: KeyEvent): EventResult {
    if (state.results.isNotEmpty() &&
        (event.matches(Actions.MOVE_DOWN) || event.matches(Actions.MOVE_UP))
    ) {
        return handleResultsKey(event)
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handleHomeKey(event: KeyEvent): EventResult {
    val focusedId = appRunner()?.focusManager()?.focusedId()
    val isFocused = focusedId == "home-panel"
            || focusedId == "home-recent-panel"
            || focusedId == "home-favorites-panel"
    if (!isFocused) return EventResult.UNHANDLED

    if (focusedId == "home-recent-panel" && state.homeSection != HomeSection.RECENT) {
        state = state.copy(homeSection = HomeSection.RECENT)
    } else if (focusedId == "home-favorites-panel" && state.homeSection != HomeSection.FAVORITES) {
        state = state.copy(homeSection = HomeSection.FAVORITES)
    }

    if (event.code() == KeyCode.TAB) {
        val next = if (state.homeSection == HomeSection.RECENT) HomeSection.FAVORITES else HomeSection.RECENT
        state = state.copy(homeSection = next)
        val nextFocusId = if (next == HomeSection.RECENT) "home-recent-panel" else "home-favorites-panel"
        appRunner()?.focusManager()?.setFocus(nextFocusId)
        return EventResult.HANDLED
    }

    when (state.homeSection) {
        HomeSection.RECENT -> {
            val maxIndex = (state.recentTracks.size - 1).coerceAtLeast(0)
            when {
                event.matches(Actions.MOVE_DOWN) -> {
                    state = state.copy(homeRecentCursor = minOf(maxIndex, state.homeRecentCursor + 1))
                    return EventResult.HANDLED
                }

                event.matches(Actions.MOVE_UP) -> {
                    state = state.copy(homeRecentCursor = maxOf(0, state.homeRecentCursor - 1))
                    return EventResult.HANDLED
                }

                event.code() == KeyCode.ENTER -> {
                    val track =
                        state.recentTracks.getOrNull(state.homeRecentCursor)?.track ?: return EventResult.UNHANDLED
                    playTrack(track)
                    return EventResult.HANDLED
                }

                event.code() == KeyCode.CHAR && event.character() == 'q' -> {
                    val track =
                        state.recentTracks.getOrNull(state.homeRecentCursor)?.track ?: return EventResult.UNHANDLED
                    addToQueue(track)
                    return EventResult.HANDLED
                }

                event.code() == KeyCode.CHAR && event.character() == 'f' -> {
                    val track =
                        state.recentTracks.getOrNull(state.homeRecentCursor)?.track ?: return EventResult.UNHANDLED
                    toggleFavorite(track)
                    return EventResult.HANDLED
                }
            }
        }

        HomeSection.FAVORITES -> {
            val maxIndex = (state.favorites.size - 1).coerceAtLeast(0)
            when {
                event.matches(Actions.MOVE_DOWN) -> {
                    state = state.copy(homeFavoritesCursor = minOf(maxIndex, state.homeFavoritesCursor + 1))
                    return EventResult.HANDLED
                }

                event.matches(Actions.MOVE_UP) -> {
                    state = state.copy(homeFavoritesCursor = maxOf(0, state.homeFavoritesCursor - 1))
                    return EventResult.HANDLED
                }

                event.code() == KeyCode.ENTER -> {
                    val track = state.favorites.getOrNull(state.homeFavoritesCursor) ?: return EventResult.UNHANDLED
                    playTrack(track)
                    return EventResult.HANDLED
                }

                event.code() == KeyCode.CHAR && event.character() == 'q' -> {
                    val track = state.favorites.getOrNull(state.homeFavoritesCursor) ?: return EventResult.UNHANDLED
                    addToQueue(track)
                    return EventResult.HANDLED
                }

                event.code() == KeyCode.CHAR && event.character() == 'f' -> {
                    val track = state.favorites.getOrNull(state.homeFavoritesCursor) ?: return EventResult.UNHANDLED
                    toggleFavorite(track)
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
)

internal fun MeloScreen.handleSidebarKey(event: KeyEvent): EventResult {
    val isFocused = appRunner()?.focusManager()?.focusedId() == "sidebar-panel"
    if (!isFocused) return EventResult.UNHANDLED

    when {
        event.matches(Actions.MOVE_UP) -> {
            if (state.sidebarInUtil) {
                val idx = sidebarUtilList.selected()
                if (idx > 0) {
                    sidebarUtilList.selected(idx - 1)
                } else {
                    sidebarNavList.selected(NAV_SECTIONS.lastIndex)
                    sidebarUtilList.selected(-1)
                    state = state.copy(sidebarInUtil = false)
                }
            } else {
                sidebarNavList.selected(maxOf(0, sidebarNavList.selected() - 1))
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) -> {
            if (!state.sidebarInUtil) {
                val idx = sidebarNavList.selected()
                if (idx < NAV_SECTIONS.lastIndex) {
                    sidebarNavList.selected(idx + 1)
                } else {
                    sidebarUtilList.selected(0)
                    sidebarNavList.selected(-1)
                    state = state.copy(sidebarInUtil = true)
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
    val section = if (state.sidebarInUtil) {
        UTIL_SECTIONS.getOrNull(sidebarUtilList.selected())
    } else {
        NAV_SECTIONS.getOrNull(sidebarNavList.selected())
    }
    if (section != null && section != state.activeSection) {
        state = state.copy(needsGraphicsClear = true, pendingSection = section)
        if (section == SidebarSection.STATS) loadStats()
    }
    return EventResult.HANDLED
}

internal fun MeloScreen.handleResultsKey(event: KeyEvent): EventResult {
    // Overlay intercepts keys from any screen
    when (state.playlistInputMode) {
        PlaylistInputMode.CREATE,
        PlaylistInputMode.RENAME -> return handlePlaylistInput(event)

        PlaylistInputMode.PICKER -> return handlePlaylistPicker(event)
        PlaylistInputMode.NONE -> {}
    }

    if (state.results.isEmpty()) return EventResult.UNHANDLED
    val isFocused = appRunner()?.focusManager()?.focusedId() == "results-panel"
    when {
        event.matches(Actions.MOVE_DOWN) -> {
            if (!isFocused) return EventResult.UNHANDLED
            val newIndex = minOf(state.results.lastIndex, state.selectedIndex + 1)
            resultList.selected(newIndex)
            state.results.getOrNull(newIndex)?.let { track ->
                state = state.copy(selectedIndex = newIndex, selectedTrack = track, marqueeOffset = 0)
                marqueeTick = 0
                debouncedLoadDetails(track)
            }
            if (newIndex >= state.results.size - 5 && !state.isLoadingMore && state.hasMore) loadMore()
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            if (!isFocused) return EventResult.UNHANDLED
            val newIndex = maxOf(0, state.selectedIndex - 1)
            resultList.selected(newIndex)
            state.results.getOrNull(newIndex)?.let { track ->
                state = state.copy(selectedIndex = newIndex, selectedTrack = track, marqueeOffset = 0)
                marqueeTick = 0
                debouncedLoadDetails(track)
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            if (!isFocused) return EventResult.UNHANDLED
            val selected = state.results.getOrNull(resultList.selected()) ?: return EventResult.UNHANDLED
            playTrack(selected)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'f' -> {
            state.results.getOrNull(state.selectedIndex)?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'q' -> {
            state.results.getOrNull(state.selectedIndex)?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'a' -> {
            val track = state.results.getOrNull(state.selectedIndex)
            if (track != null) openPlaylistPicker(track)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'l' -> {
            loadLyrics()
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handleDetailKey(event: KeyEvent): EventResult {
    when {
        event.code() == KeyCode.CHAR && event.character() == '1' -> {
            state = state.copy(detailTab = DetailTab.INFO)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == '2' -> {
            state = state.copy(detailTab = DetailTab.LYRICS)
            if (state.lyrics == null && !state.isLoadingLyrics) loadLyrics()
            appRunner()?.focusManager()?.setFocus("lyrics-area")
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == '3' -> {
            state = state.copy(detailTab = DetailTab.SIMILAR)
            appRunner()?.focusManager()?.setFocus("similar-area")
            return EventResult.HANDLED
        }

        event.matches(Actions.SELECT) && state.detailTab == DetailTab.LYRICS -> {
            if (state.lyrics == null) loadLyrics()
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) && state.detailTab == DetailTab.SIMILAR -> {
            val maxIndex = (state.similarTracks.size - 1).coerceAtLeast(0)
            val newCursor = minOf(maxIndex, state.similarCursor + 1)
            state = state.copy(similarCursor = newCursor)
            if (newCursor >= state.similarTracks.size - 3 && state.hasMoreSimilar && !state.isLoadingMoreSimilar) {
                loadMoreSimilar()
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) && state.detailTab == DetailTab.SIMILAR -> {
            state = state.copy(similarCursor = maxOf(0, state.similarCursor - 1))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER && state.detailTab == DetailTab.SIMILAR -> {
            state.similarTracks.getOrNull(state.similarCursor)?.let { playTrack(it) }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handleLibraryKey(event: KeyEvent): EventResult {
    when (state.playlistInputMode) {
        PlaylistInputMode.CREATE,
        PlaylistInputMode.RENAME -> return handlePlaylistInput(event)

        PlaylistInputMode.PICKER -> return handlePlaylistPicker(event)
        PlaylistInputMode.NONE -> {}
    }

    val isFocused = appRunner()?.focusManager()?.focusedId() == "library-panel"
    if (!isFocused) return EventResult.UNHANDLED

    if (event.code() == KeyCode.CHAR && event.character() == '1') {
        state = state.copy(libraryTab = LibraryTab.FAVORITES)
        return EventResult.HANDLED
    }
    if (event.code() == KeyCode.CHAR && event.character() == '2') {
        state = state.copy(libraryTab = LibraryTab.PLAYLISTS, isInPlaylistDetail = false)
        return EventResult.HANDLED
    }

    return when (state.libraryTab) {
        LibraryTab.FAVORITES -> handleFavoritesKey(event)
        LibraryTab.PLAYLISTS -> if (state.isInPlaylistDetail) handlePlaylistDetailKey(event) else handlePlaylistsKey(
            event
        )
    }
}

internal fun MeloScreen.handleFavoritesKey(event: KeyEvent): EventResult {
    when {
        event.matches(Actions.MOVE_DOWN) -> {
            favoritesList.selected(minOf(state.favorites.lastIndex.coerceAtLeast(0), favoritesList.selected() + 1))
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            favoritesList.selected(maxOf(0, favoritesList.selected() - 1))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            state.favorites.getOrNull(favoritesList.selected())?.let { playTrack(it) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'f' -> {
            state.favorites.getOrNull(favoritesList.selected())?.let { removeFavoriteTrack(it) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'q' -> {
            state.favorites.getOrNull(favoritesList.selected())?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'a' -> {
            val track = state.favorites.getOrNull(favoritesList.selected())
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
            state = state.copy(isQueueVisible = false)
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) -> {
            if (!isFocused) return EventResult.UNHANDLED
            val newCursor = minOf(state.queue.lastIndex, state.queueCursor + 1)
            state = state.copy(queueCursor = newCursor)

            if (state.isRadioMode && !state.isLoadingMoreRadio && newCursor >= state.queue.size - 5) {
                loadMoreRadioTracks()
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            if (!isFocused) return EventResult.UNHANDLED
            state = state.copy(queueCursor = maxOf(0, state.queueCursor - 1))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            if (!isFocused) return EventResult.UNHANDLED
            if (state.queue.getOrNull(state.queueCursor) != null) playFromQueue(state.queueCursor)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.DELETE || (event.code() == KeyCode.CHAR && event.character() == 'd') -> {
            if (!isFocused) return EventResult.UNHANDLED
            removeFromQueue(state.queueCursor)
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'c' -> {
            clearQueue()
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handlePlayerBarKey(event: KeyEvent): EventResult {
    when {
        event.matches(Actions.MOVE_LEFT) -> {
            seekTo(state.progress - 0.05); return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_RIGHT) -> {
            seekTo(state.progress + 0.05); return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'p' -> {
            seekBackward(); return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'n' -> {
            seekForward(); return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == ' ' -> {
            togglePlayPause(); return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'q' -> {
            toggleQueue(); return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 'r' -> {
            cycleRepeat(); return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == 's' -> {
            toggleShuffle(); return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == '+' -> {
            adjustVolume(5); return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && event.character() == '-' -> {
            adjustVolume(-5); return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

