package com.github.adriianh.cli.tui.handler
import com.github.adriianh.cli.tui.*

import com.github.adriianh.core.domain.model.Track
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch

internal fun MeloScreen.handlePlaylistsKey(event: KeyEvent): EventResult {
    when {
        event.matches(Actions.MOVE_DOWN) -> {
            playlistsList.selected(minOf(state.playlists.lastIndex.coerceAtLeast(0), playlistsList.selected() + 1))
            return EventResult.HANDLED
        }
        event.matches(Actions.MOVE_UP) -> {
            playlistsList.selected(maxOf(0, playlistsList.selected() - 1))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.ENTER -> {
            openPlaylistDetail(playlistsList.selected())
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'n' -> {
            state = state.copy(playlistInputMode = PlaylistInputMode.CREATE, playlistInput = "")
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'r' -> {
            val pl = state.playlists.getOrNull(playlistsList.selected()) ?: return EventResult.UNHANDLED
            state = state.copy(playlistInputMode = PlaylistInputMode.RENAME, playlistInput = pl.name)
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'd' || event.code() == KeyCode.DELETE -> {
            val pl = state.playlists.getOrNull(playlistsList.selected()) ?: return EventResult.UNHANDLED
            scope.launch { deletePlaylist(pl.id) }
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'p' -> {
            openPlaylistDetail(playlistsList.selected(), autoPlay = true)
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handlePlaylistDetailKey(event: KeyEvent): EventResult {
    when {
        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(isInPlaylistDetail = false, selectedPlaylist = null, playlistTracks = emptyList())
            playlistTracksJob?.cancel()
            return EventResult.HANDLED
        }
        event.matches(Actions.MOVE_DOWN) -> {
            playlistTracksList.selected(minOf(state.playlistTracks.lastIndex.coerceAtLeast(0), playlistTracksList.selected() + 1))
            return EventResult.HANDLED
        }
        event.matches(Actions.MOVE_UP) -> {
            playlistTracksList.selected(maxOf(0, playlistTracksList.selected() - 1))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.ENTER -> {
            state.playlistTracks.getOrNull(playlistTracksList.selected())?.let { playTrack(it) }
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'q' -> {
            state.playlistTracks.getOrNull(playlistTracksList.selected())?.let { addToQueue(it) }
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'd' || event.code() == KeyCode.DELETE -> {
            val pl = state.selectedPlaylist ?: return EventResult.UNHANDLED
            val track = state.playlistTracks.getOrNull(playlistTracksList.selected()) ?: return EventResult.UNHANDLED
            scope.launch { removeTrackFromPlaylist(pl.id, track.id) }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handlePlaylistInput(event: KeyEvent): EventResult {
    when {
        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(playlistInputMode = PlaylistInputMode.NONE, playlistInput = "")
            return EventResult.HANDLED
        }
        event.code() == KeyCode.ENTER -> {
            val name = state.playlistInput.trim()
            if (name.isNotBlank()) {
                when (state.playlistInputMode) {
                    PlaylistInputMode.CREATE -> scope.launch { createPlaylist(name) }
                    PlaylistInputMode.RENAME -> {
                        val pl = state.playlists.getOrNull(playlistsList.selected())
                        if (pl != null) scope.launch { renamePlaylist(pl.id, name) }
                    }
                    PlaylistInputMode.PICKER, PlaylistInputMode.NONE -> {}
                }
            }
            state = state.copy(playlistInputMode = PlaylistInputMode.NONE, playlistInput = "")
            return EventResult.HANDLED
        }
        event.code() == KeyCode.BACKSPACE -> {
            state = state.copy(playlistInput = state.playlistInput.dropLast(1))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR -> {
            state = state.copy(playlistInput = state.playlistInput + event.character())
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handlePlaylistPicker(event: KeyEvent): EventResult {
    when {
        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(playlistInputMode = PlaylistInputMode.NONE, playlistPickerTrack = null)
            return EventResult.HANDLED
        }
        event.matches(Actions.MOVE_DOWN) -> {
            state = state.copy(playlistPickerCursor = minOf(state.playlists.lastIndex, state.playlistPickerCursor + 1))
            return EventResult.HANDLED
        }
        event.matches(Actions.MOVE_UP) -> {
            state = state.copy(playlistPickerCursor = maxOf(0, state.playlistPickerCursor - 1))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.ENTER -> {
            val pl = state.playlists.getOrNull(state.playlistPickerCursor) ?: return EventResult.UNHANDLED
            val track = state.playlistPickerTrack ?: return EventResult.UNHANDLED
            scope.launch { addTrackToPlaylist(pl.id, track) }
            state = state.copy(playlistInputMode = PlaylistInputMode.NONE, playlistPickerTrack = null)
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

// ─── Playlist Actions ─────────────────────────────────────────────────────────

internal fun MeloScreen.openPlaylistDetail(index: Int, autoPlay: Boolean = false) {
    val pl = state.playlists.getOrNull(index) ?: return
    state = state.copy(selectedPlaylist = pl, isInPlaylistDetail = true, playlistTracks = emptyList())
    playlistTracksJob?.cancel()
    playlistTracksJob = scope.launch {
        getPlaylistTracks(pl.id).collect { tracks ->
            appRunner()?.runOnRenderThread {
                state = state.copy(playlistTracks = tracks)
                if (autoPlay && tracks.isNotEmpty()) {
                    state = state.copy(queue = tracks, queueIndex = -1, isRadioMode = false)
                    playFromQueue(0)
                }
            }
        }
    }
}

internal fun MeloScreen.openPlaylistPicker(track: Track) {
    state = if (state.playlists.isEmpty()) {
        state.copy(playlistInputMode = PlaylistInputMode.CREATE, playlistInput = "", playlistPickerTrack = track)
    } else {
        state.copy(playlistInputMode = PlaylistInputMode.PICKER, playlistPickerTrack = track, playlistPickerCursor = 0)
    }
}