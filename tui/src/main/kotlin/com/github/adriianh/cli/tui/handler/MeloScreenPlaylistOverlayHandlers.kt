package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.util.ToastKind
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch

/**
 * Handles the playlist creation/rename input overlay and the playlist picker overlay.
 */
internal fun MeloScreen.handlePlaylistInput(event: KeyEvent): EventResult {
    val interaction = state.playlistInteraction
    when {
        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(
                playlistInteraction = interaction.copy(
                    playlistInputMode = PlaylistInputMode.NONE,
                    playlistInput = "",
                    playlistPickerTrack = null,
                    playlistPickerTracks = emptyList()
                )
            )
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            val name = interaction.playlistInput.trim()
            if (name.isNotBlank()) {
                when (interaction.playlistInputMode) {
                    PlaylistInputMode.CREATE -> scope.launch {
                        val id = createPlaylist(name)
                        val tracksToAdd = interaction.playlistPickerTracks.ifEmpty {
                            listOfNotNull(interaction.playlistPickerTrack)
                        }
                        tracksToAdd.forEach { track ->
                            addTrackToPlaylist(id, track)
                        }
                        appRunner()?.runOnRenderThread {
                            val summary = buildString {
                                append("Playlist created: '")
                                append(name)
                                append("'")
                                if (tracksToAdd.isNotEmpty()) {
                                    append(" (${tracksToAdd.size} added)")
                                }
                            }
                            showToast(summary, ToastKind.SUCCESS)
                        }
                    }

                    PlaylistInputMode.RENAME -> {
                        val pl = state.collections.playlists.getOrNull(playlistsList.selected())
                        if (pl != null) scope.launch {
                            renamePlaylist(pl.id, name)
                            appRunner()?.runOnRenderThread {
                                showToast("Playlist renamed: '$name'", ToastKind.SUCCESS)
                            }
                        }
                    }

                    PlaylistInputMode.PICKER, PlaylistInputMode.NONE -> {}
                }
            }
            state = state.copy(
                selection = state.selection.clear(),
                playlistInteraction = interaction.copy(
                    playlistInputMode = PlaylistInputMode.NONE,
                    playlistInput = "",
                    playlistPickerTrack = null,
                    playlistPickerTracks = emptyList()
                )
            )
            return EventResult.HANDLED
        }

        event.code() == KeyCode.BACKSPACE -> {
            state = state.copy(
                playlistInteraction = interaction.copy(
                    playlistInput = interaction.playlistInput.dropLast(1)
                )
            )
            return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && !event.modifiers().ctrl() && !event.modifiers().alt() -> {
            val str = event.string()
            if (str.isNotBlank() || str == " ") {
                state =
                    state.copy(playlistInteraction = interaction.copy(playlistInput = interaction.playlistInput + str))
            }
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}

internal fun MeloScreen.handlePlaylistPicker(event: KeyEvent): EventResult {
    val interaction = state.playlistInteraction
    val playlists = state.collections.playlists
    when {
        event.code() == KeyCode.ESCAPE -> {
            state = state.copy(
                playlistInteraction = interaction.copy(
                    playlistInputMode = PlaylistInputMode.NONE,
                    playlistPickerTrack = null,
                    playlistPickerTracks = emptyList()
                )
            )
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_DOWN) -> {
            state = state.copy(
                playlistInteraction = interaction.copy(
                    playlistPickerCursor = minOf(
                        playlists.lastIndex,
                        interaction.playlistPickerCursor + 1
                    )
                )
            )
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            state = state.copy(
                playlistInteraction = interaction.copy(
                    playlistPickerCursor = maxOf(
                        0,
                        interaction.playlistPickerCursor - 1
                    )
                )
            )
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            val pl = playlists.getOrNull(interaction.playlistPickerCursor)
                ?: return handleGlobalShortcuts(event)
            val tracksToAdd = interaction.playlistPickerTracks.ifEmpty {
                listOfNotNull(interaction.playlistPickerTrack)
            }
            if (tracksToAdd.isNotEmpty()) {
                scope.launch {
                    tracksToAdd.forEach { track ->
                        addTrackToPlaylist(pl.id, track)
                    }
                    appRunner()?.runOnRenderThread {
                        val summary = if (tracksToAdd.size == 1) {
                            "Added to '${pl.name}'"
                        } else {
                            "${tracksToAdd.size} tracks added to '${pl.name}'"
                        }
                        showToast(summary)
                    }
                }
            }
            state = state.copy(
                selection = state.selection.clear(),
                playlistInteraction = interaction.copy(
                    playlistInputMode = PlaylistInputMode.NONE,
                    playlistPickerTrack = null,
                    playlistPickerTracks = emptyList()
                )
            )
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}

internal fun MeloScreen.openPlaylistPicker(tracks: List<Track>) {
    val playlists = state.collections.playlists

    state = if (playlists.isEmpty()) {
        state.copy(
            playlistInteraction = state.playlistInteraction.copy(
                playlistInputMode = PlaylistInputMode.CREATE,
                playlistInput = "",
                playlistPickerTrack = tracks.firstOrNull(),
                playlistPickerTracks = tracks
            )
        )
    } else {
        state.copy(
            playlistInteraction = state.playlistInteraction.copy(
                playlistInputMode = PlaylistInputMode.PICKER,
                playlistPickerTrack = tracks.firstOrNull(),
                playlistPickerTracks = tracks,
                playlistPickerCursor = 0
            )
        )
    }
}

internal fun MeloScreen.openPlaylistPicker(track: Track) {
    openPlaylistPicker(listOf(track))
}