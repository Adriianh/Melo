package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.*
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch

internal fun MeloScreen.handlePlaylistsKey(event: KeyEvent): EventResult {
    val screen = state.screen as? ScreenState.Library ?: return EventResult.UNHANDLED
    val playlists = state.collections.playlists
    when {
        event.matches(Actions.MOVE_DOWN) -> {
            playlistsList.selected(minOf(playlists.lastIndex.coerceAtLeast(0), playlistsList.selected() + 1))
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
            state = state.copy(playlistInteraction = state.playlistInteraction.copy(playlistInputMode = PlaylistInputMode.CREATE, playlistInput = ""))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'r' -> {
            val pl = playlists.getOrNull(playlistsList.selected()) ?: return EventResult.UNHANDLED
            state = state.copy(playlistInteraction = state.playlistInteraction.copy(playlistInputMode = PlaylistInputMode.RENAME, playlistInput = pl.name))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'd' || event.code() == KeyCode.DELETE -> {
            val pl = playlists.getOrNull(playlistsList.selected()) ?: return EventResult.UNHANDLED
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
    val screen = state.screen as? ScreenState.Library ?: return EventResult.UNHANDLED
    when {
        event.code() == KeyCode.ESCAPE -> {
            updateScreen<ScreenState.Library> { it.copy(isInPlaylistDetail = false, selectedPlaylist = null, playlistTracks = emptyList()) }
            playlistTracksJob?.cancel()
            return EventResult.HANDLED
        }
        event.matches(Actions.MOVE_DOWN) -> {
            playlistTracksList.selected(minOf(screen.playlistTracks.lastIndex.coerceAtLeast(0), playlistTracksList.selected() + 1))
            return EventResult.HANDLED
        }
        event.matches(Actions.MOVE_UP) -> {
            playlistTracksList.selected(maxOf(0, playlistTracksList.selected() - 1))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.ENTER -> {
            val tracks = screen.playlistTracks
            val idx = playlistTracksList.selected()
            if (idx in tracks.indices) playList(tracks, idx)
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'q' -> {
            screen.playlistTracks.getOrNull(playlistTracksList.selected())?.let { addToQueue(it) }
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'd' || event.code() == KeyCode.DELETE -> {
            val pl = screen.selectedPlaylist ?: return EventResult.UNHANDLED
            val track = screen.playlistTracks.getOrNull(playlistTracksList.selected()) ?: return EventResult.UNHANDLED
            scope.launch { removeTrackFromPlaylist(pl.id, track.id) }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handlePlaylistInput(event: KeyEvent): EventResult {
    val interaction = state.playlistInteraction
    when {
        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(playlistInteraction = interaction.copy(playlistInputMode = PlaylistInputMode.NONE, playlistInput = ""))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.ENTER -> {
            val name = interaction.playlistInput.trim()
            if (name.isNotBlank()) {
                when (interaction.playlistInputMode) {
                    PlaylistInputMode.CREATE -> scope.launch { createPlaylist(name) }
                    PlaylistInputMode.RENAME -> {
                        val pl = state.collections.playlists.getOrNull(playlistsList.selected())
                        if (pl != null) scope.launch { renamePlaylist(pl.id, name) }
                    }
                    PlaylistInputMode.PICKER, PlaylistInputMode.NONE -> {}
                }
            }
            state = state.copy(playlistInteraction = interaction.copy(playlistInputMode = PlaylistInputMode.NONE, playlistInput = ""))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.BACKSPACE -> {
            state = state.copy(playlistInteraction = interaction.copy(playlistInput = interaction.playlistInput.dropLast(1)))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR -> {
            state = state.copy(playlistInteraction = interaction.copy(playlistInput = interaction.playlistInput + event.character()))
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.handlePlaylistPicker(event: KeyEvent): EventResult {
    val interaction = state.playlistInteraction
    val playlists = state.collections.playlists
    when {
        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(playlistInteraction = interaction.copy(playlistInputMode = PlaylistInputMode.NONE, playlistPickerTrack = null))
            return EventResult.HANDLED
        }
        event.matches(Actions.MOVE_DOWN) -> {
            state = state.copy(playlistInteraction = interaction.copy(playlistPickerCursor = minOf(playlists.lastIndex, interaction.playlistPickerCursor + 1)))
            return EventResult.HANDLED
        }
        event.matches(Actions.MOVE_UP) -> {
            state = state.copy(playlistInteraction = interaction.copy(playlistPickerCursor = maxOf(0, interaction.playlistPickerCursor - 1)))
            return EventResult.HANDLED
        }
        event.code() == KeyCode.ENTER -> {
            val pl = playlists.getOrNull(interaction.playlistPickerCursor) ?: return EventResult.UNHANDLED
            val track = interaction.playlistPickerTrack ?: return EventResult.UNHANDLED
            scope.launch { addTrackToPlaylist(pl.id, track) }
            state = state.copy(playlistInteraction = interaction.copy(playlistInputMode = PlaylistInputMode.NONE, playlistPickerTrack = null))
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

// ─── Playlist Actions ─────────────────────────────────────────────────────────

internal fun MeloScreen.openPlaylistDetail(index: Int, autoPlay: Boolean = false) {
    val pl = state.collections.playlists.getOrNull(index) ?: return
    updateScreen<ScreenState.Library> { it.copy(selectedPlaylist = pl, isInPlaylistDetail = true, playlistTracks = emptyList()) }
    playlistTracksJob?.cancel()
    playlistTracksJob = scope.launch {
        getPlaylistTracks(pl.id).collect { tracks ->
            appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Library> { it.copy(playlistTracks = tracks) }
                if (autoPlay && tracks.isNotEmpty()) {
                    state = state.copy(player = state.player.copy(queue = tracks, queueIndex = -1, isRadioMode = false))
                    playFromQueue(0)
                }
            }
        }
    }
}

internal fun MeloScreen.openPlaylistPicker(track: Track) {
    val playlists = state.collections.playlists
    
    state = if (playlists.isEmpty()) {
        state.copy(playlistInteraction = state.playlistInteraction.copy(playlistInputMode = PlaylistInputMode.CREATE, playlistInput = "", playlistPickerTrack = track))
    } else {
        state.copy(playlistInteraction = state.playlistInteraction.copy(playlistInputMode = PlaylistInputMode.PICKER, playlistPickerTrack = track, playlistPickerCursor = 0))
    }
}