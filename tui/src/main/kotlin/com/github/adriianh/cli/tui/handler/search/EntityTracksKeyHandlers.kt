package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.handleGlobalShortcuts
import com.github.adriianh.cli.tui.handler.isCtrlA
import com.github.adriianh.cli.tui.handler.isCtrlF
import com.github.adriianh.cli.tui.handler.matchesAction
import com.github.adriianh.cli.tui.handler.openPlaylistPicker
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openBatchOptions
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playList
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.cli.tui.handler.toggleFavorite
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.filterAndSortTracks
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

internal fun MeloScreen.handleEntityTracksKey(
    actualDetail: ScreenState.EntityDetail,
    event: KeyEvent
): EventResult {
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
            return exitEntityDetailToReturnScreen(actualDetail)
        }

        event.modifiers().alt() && event.code() == KeyCode.LEFT -> {
            return exitEntityDetailToReturnScreen(actualDetail)
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

    return EventResult.UNHANDLED
}