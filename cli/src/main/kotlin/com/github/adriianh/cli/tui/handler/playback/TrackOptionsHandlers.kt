package com.github.adriianh.cli.tui.handler.playback

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.handler.loadMoreSimilar
import com.github.adriianh.cli.tui.handler.openPlaylistPicker
import com.github.adriianh.cli.tui.handler.toggleFavorite
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

internal fun MeloScreen.handleTrackOptionsKey(event: KeyEvent): EventResult {
    val optionsCount = 6
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
            val track = state.trackOptions.track ?: return EventResult.UNHANDLED
            val actionIndex = state.trackOptions.selectedIndex
            state = state.copy(trackOptions = state.trackOptions.copy(isVisible = false))

            when (actionIndex) {
                0 -> playTrack(track)
                1 -> addToQueue(track)
                2 -> toggleFavorite(track)
                3 -> openPlaylistPicker(track)
                4 -> {
                    val offlineTrack = state.collections.offlineTracks.find { it.track.id == track.id }
                    if (offlineTrack?.downloadStatus == DownloadStatus.COMPLETED && offlineTrack.downloadType == DownloadType.MANUAL) {
                        this@handleTrackOptionsKey.deleteDownloadedTrack(track.id)
                    } else {
                        downloadTrack(track, DownloadType.MANUAL)
                    }
                }

                5 -> {
                    state = state.copy(detail = state.detail.copy(selectedTrack = track, detailTab = DetailTab.SIMILAR))
                    appRunner()?.focusManager()?.setFocus("similar-area")
                    loadMoreSimilar()
                }
            }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.openTrackOptions(track: Track) {
    state = state.copy(trackOptions = state.trackOptions.copy(track = track, selectedIndex = 0, isVisible = true))
    appRunner()?.focusManager()?.setFocus("track-options-panel")
}