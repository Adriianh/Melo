package com.github.adriianh.cli.tui.handler.search

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.handler.handleGlobalShortcuts
import com.github.adriianh.cli.tui.handler.matchesAction
import com.github.adriianh.cli.tui.handler.openPlaylistPicker
import com.github.adriianh.cli.tui.handler.playback.addToQueue
import com.github.adriianh.cli.tui.handler.playback.openTrackOptions
import com.github.adriianh.cli.tui.handler.playback.playTrack
import com.github.adriianh.cli.tui.handler.toggleFavorite
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

/**
 * Resuelve el item seleccionado en el dashboard de artista, descendiendo
 * dentro de la sección actual si la selección apunta a un [SearchResult.ArtistSection].
 * Devuelve null si la selección está vacía o el item de la sección no existe.
 */
private fun MeloScreen.selectedArtistDashboardItem(
    actualDetail: ScreenState.EntityDetail
): Any? {
    val arrayItem = actualDetail.artistDashboardItems.getOrNull(artistDashboardList.selected())
    var item = if (arrayItem is Pair<*, *>) {
        if (actualDetail.artistDashboardX == 1) arrayItem.second else arrayItem.first
    } else arrayItem
    if (item is SearchResult.ArtistSection) {
        val currentY = actualDetail.artistDashboardPositions[item.title] ?: 0
        item = item.items.getOrNull(currentY)
    }
    return item
}

internal fun MeloScreen.handleArtistDashboardKey(
    actualDetail: ScreenState.EntityDetail,
    entity: SearchResult.Artist,
    event: KeyEvent
): EventResult {
    val items = actualDetail.artistDashboardItems
    val listSize = items.size

    when {
        event.code() == KeyCode.ESCAPE || (event.modifiers()
            .alt() && event.code() == KeyCode.LEFT) -> {
            return exitEntityDetailToReturnScreen(actualDetail)
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
            val desc = entity.description
            if (!desc.isNullOrEmpty()) {
                appRunner()?.focusManager()?.setFocus("desc-area")
                return EventResult.HANDLED
            }
        }

        listSize > 0 && (event.isCharIgnoreCase('m') || event.matchesAction(
            MeloAction.TRACK_OPTIONS,
            settingsViewState.currentSettings
        )) -> {
            (selectedArtistDashboardItem(actualDetail) as? Track
                ?: (selectedArtistDashboardItem(actualDetail) as? SearchResult.Song)?.track)
                ?.let { openTrackOptions(it) }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER && listSize > 0 -> {
            when (val targetItem = selectedArtistDashboardItem(actualDetail)) {
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
            (selectedArtistDashboardItem(actualDetail) as? Track
                ?: (selectedArtistDashboardItem(actualDetail) as? SearchResult.Song)?.track)
                ?.let { toggleFavorite(it) }
            return EventResult.HANDLED
        }

        listSize > 0 && event.matchesAction(
            MeloAction.ADD_TO_QUEUE, settingsViewState.currentSettings
        ) -> {
            (selectedArtistDashboardItem(actualDetail) as? Track
                ?: (selectedArtistDashboardItem(actualDetail) as? SearchResult.Song)?.track)
                ?.let { addToQueue(it) }
            return EventResult.HANDLED
        }

        listSize > 0 && event.matchesAction(
            MeloAction.ADD_PLAYLIST, settingsViewState.currentSettings
        ) -> {
            (selectedArtistDashboardItem(actualDetail) as? Track
                ?: (selectedArtistDashboardItem(actualDetail) as? SearchResult.Song)?.track)
                ?.let { openPlaylistPicker(it) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('r') -> {
            val seedTrack = entity.topSongs?.firstOrNull()
                ?: items.filterIsInstance<Track>().firstOrNull()
                ?: items.filterIsInstance<SearchResult.Song>().firstOrNull()?.track
            if (seedTrack != null) {
                downloadTrack(seedTrack, DownloadType.PREFETCH)
                playTrack(seedTrack)
                return EventResult.HANDLED
            }
        }
    }

    return EventResult.UNHANDLED
}