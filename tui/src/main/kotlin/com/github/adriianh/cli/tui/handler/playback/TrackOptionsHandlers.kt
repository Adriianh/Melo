package com.github.adriianh.cli.tui.handler.playback

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.component.TrackMenuAction
import com.github.adriianh.cli.tui.component.resolveTrackMenuItems
import com.github.adriianh.cli.tui.handler.handleGlobalShortcuts
import com.github.adriianh.cli.tui.handler.loadMoreSimilar
import com.github.adriianh.cli.tui.handler.openPlaylistPicker
import com.github.adriianh.cli.tui.handler.removeFavoriteTrack
import com.github.adriianh.cli.tui.handler.toggleFavorite
import com.github.adriianh.cli.tui.util.ToastKind
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch

internal fun MeloScreen.handleTrackOptionsKey(event: KeyEvent): EventResult {
    val items = resolveTrackMenuItems(state)
    val optionsCount = items.size
    if (optionsCount == 0) {
        state = state.copy(trackOptions = state.trackOptions.copy(isVisible = false))
        return EventResult.HANDLED
    }

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
            val newIndex =
                if (state.trackOptions.selectedIndex <= 0) optionsCount - 1 else state.trackOptions.selectedIndex - 1
            state = state.copy(trackOptions = state.trackOptions.copy(selectedIndex = newIndex))
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            val safeIndex = state.trackOptions.selectedIndex.coerceIn(0, optionsCount - 1)
            val selectedItem = items.getOrNull(safeIndex) ?: return EventResult.HANDLED
            state = state.copy(trackOptions = state.trackOptions.copy(isVisible = false))
            executeTrackMenuAction(selectedItem.action)
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}

internal fun MeloScreen.executeTrackMenuAction(action: TrackMenuAction) {
    val isBatch = state.trackOptions.isBatch
    val batch = state.trackOptions.batchTracks
    val track = state.trackOptions.track

    when (action) {
        TrackMenuAction.PLAY -> {
            if (isBatch) {
                if (batch.isNotEmpty()) {
                    playList(batch, 0)
                    state = state.copy(selection = state.selection.clear())
                }
            } else if (track != null) {
                playTrack(track)
            }
        }

        TrackMenuAction.ADD_TO_QUEUE -> {
            if (isBatch) {
                batch.forEach { addToQueue(it, showConfirmation = false) }
                state = state.copy(selection = state.selection.clear())
                showToast("${batch.size} tracks added to queue")
            } else if (track != null) {
                addToQueue(track)
            }
        }

        TrackMenuAction.ADD_TO_PLAYLIST -> {
            if (isBatch) {
                openPlaylistPicker(batch)
            } else if (track != null) {
                openPlaylistPicker(track)
            }
        }

        TrackMenuAction.ADD_TO_FAVORITES, TrackMenuAction.TOGGLE_FAVORITE -> {
            if (isBatch) {
                batch.forEach { toggleFavorite(it, showConfirmation = false) }
                state = state.copy(selection = state.selection.clear())
                showToast("${batch.size} tracks added to favorites", ToastKind.HEART)
            } else if (track != null) {
                toggleFavorite(track)
            }
        }

        TrackMenuAction.REMOVE_FROM_FAVORITES -> {
            if (isBatch) {
                batch.forEach { removeFavoriteTrack(it, showConfirmation = false) }
                state = state.copy(selection = state.selection.clear())
                showToast("${batch.size} tracks removed from favorites", ToastKind.HEART)
            } else if (track != null) {
                removeFavoriteTrack(track)
            }
        }

        TrackMenuAction.REMOVE_FROM_PLAYLIST -> {
            val libraryScreen = state.screen as? ScreenState.Library
            val pl = libraryScreen?.selectedPlaylist
            if (pl != null) {
                if (isBatch) {
                    val count = batch.size
                    scope.launch {
                        batch.forEach { removeTrackFromPlaylist(pl.id, it.id) }
                        appRunner()?.runOnRenderThread {
                            showToast("$count tracks removed from '${pl.name}'")
                        }
                    }
                    state = state.copy(selection = state.selection.clear())
                } else if (track != null) {
                    scope.launch {
                        removeTrackFromPlaylist(pl.id, track.id)
                        appRunner()?.runOnRenderThread {
                            showToast("Removed from '${pl.name}'")
                        }
                    }
                }
            }
        }

        TrackMenuAction.DOWNLOAD_OFFLINE -> {
            if (isBatch) {
                batch.forEach { downloadTrack(it, DownloadType.MANUAL) }
                state = state.copy(selection = state.selection.clear())
                showToast("${batch.size} tracks downloading")
            } else if (track != null) {
                downloadTrack(track, DownloadType.MANUAL)
                showToast("Downloading: ${track.title}")
            }
        }

        TrackMenuAction.DELETE_DOWNLOAD -> {
            if (isBatch) {
                batch.forEach { deleteDownloadedTrack(it.id) }
                state = state.copy(selection = state.selection.clear())
                showToast("${batch.size} downloads removed")
            } else if (track != null) {
                deleteDownloadedTrack(track.id)
                showToast("Download removed")
            }
        }

        TrackMenuAction.VIEW_SIMILAR -> {
            if (track != null) {
                state = state.copy(
                    detail = state.detail.copy(
                        selectedTrack = track,
                        detailTab = DetailTab.SIMILAR
                    )
                )
                appRunner()?.focusManager()?.setFocus("similar-area")
                loadMoreSimilar()
            }
        }

        TrackMenuAction.CLEAR_SELECTION -> {
            state = state.copy(selection = state.selection.clear())
        }
    }
}

internal fun MeloScreen.openTrackOptions(track: Track) {
    if (state.selection.isNotEmpty) {
        openBatchOptions(state.selection.tracks())
        return
    }
    state = state.copy(
        trackOptions = state.trackOptions.copy(
            track = track,
            batchTracks = emptyList(),
            selectedIndex = 0,
            isVisible = true
        )
    )
    appRunner()?.focusManager()?.setFocus("track-options-panel")
}

internal fun MeloScreen.openBatchOptions(tracks: List<Track>) {
    state = state.copy(
        trackOptions = state.trackOptions.copy(
            track = null,
            batchTracks = tracks,
            selectedIndex = 0,
            isVisible = true
        )
    )
    appRunner()?.focusManager()?.setFocus("track-options-panel")
}