package com.github.adriianh.cli.tui.handler.playback

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.handler.matchesAction
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyEvent

internal fun MeloScreen.playTrack(track: Track) {
    if (!state.isPlayable(track)) return

    val inQueue = state.player.queue.any { it.id == track.id }
    state = state.copy(
        player = state.player.copy(
            isLoadingAudio = true,
            audioError = null,
            isRadioMode = if (inQueue) state.player.isRadioMode else true,
        )
    )

    if (inQueue) playbackManager.playTrackInQueue(track)
    else playbackManager.playTrack(track)
}

internal fun MeloScreen.togglePlayPause() {
    if (state.player.nowPlaying == null || state.player.isLoadingAudio) return
    playbackManager.togglePlayPause()
}

internal fun MeloScreen.adjustVolume(delta: Int) {
    val newVol = (state.player.volume + delta).coerceIn(0, 100)
    playbackManager.setVolume(newVol / 100f)
}

internal fun MeloScreen.seekTo(progress: Double) {
    val duration = state.player.nowPlaying?.durationMs ?: return
    if (state.player.isLoadingAudio) return
    val clamped = progress.coerceIn(0.0, 1.0)
    val targetMs = (clamped * duration).toLong()
    playbackManager.seekTo(targetMs)
    if (settingsViewState.currentSettings.discordRpcEnabled) {
        discordRpcManager.updateActivity(state.player.nowPlaying, state.player.isPlaying, targetMs)
    }
}

internal fun MeloScreen.seekBackward() {
    if (state.player.isLoadingAudio) return
    playbackManager.playPrevious()
}

internal fun MeloScreen.seekForward() {
    val queue = playbackManager.queueState.value.tracks
    val currentIndex = playbackManager.queueState.value.currentIndex
    val isAtLast = queue.isNotEmpty() && currentIndex + 1 >= queue.size
    if (state.player.isLoadingAudio && !isAtLast) return
    if (queue.isEmpty()) return

    playbackManager.playNext()
}

internal fun MeloScreen.playList(tracks: List<Track>, startIndex: Int) {
    if (tracks.isEmpty() || startIndex !in tracks.indices) return
    val targetTrack = tracks[startIndex]
    if (!state.isPlayable(targetTrack)) return

    val playableTracks = tracks.filter { state.isPlayable(it) }
    val newStartIndex = playableTracks.indexOfFirst { it.id == targetTrack.id }
    if (newStartIndex < 0) return

    state = state.copy(
        player = state.player.copy(
            isLoadingAudio = true,
            isRadioMode = false,
            audioError = null,
        )
    )
    playbackManager.setQueue(playableTracks, newStartIndex)
}

internal fun MeloScreen.playFromQueue(index: Int) {
    val track = playbackManager.queueState.value.tracks.getOrNull(index) ?: return
    state = state.copy(
        player = state.player.copy(isLoadingAudio = true, audioError = null)
    )
    playbackManager.playTrackInQueue(track)
}

internal fun MeloScreen.handlePlayerBarKey(event: KeyEvent): EventResult {
    if (state.isSettingsVisible || state.trackOptions.isVisible) return EventResult.HANDLED
    val settings = settingsViewState.currentSettings
    when {
        event.matches(Actions.MOVE_LEFT) -> {
            seekTo(state.player.progress - 0.05); return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_RIGHT) -> {
            seekTo(state.player.progress + 0.05); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.PREVIOUS, settings) -> {
            seekBackward(); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.NEXT, settings) -> {
            seekForward(); return EventResult.HANDLED
        }

        event.isChar('<') || event.isChar(',') -> {
            seekTo(state.player.progress - 0.05); return EventResult.HANDLED
        }

        event.isChar('>') || event.isChar('.') -> {
            seekTo(state.player.progress + 0.05); return EventResult.HANDLED
        }

        event.matchesAction(
            MeloAction.PLAY_PAUSE,
            settings
        ) || event.isChar(' ') -> {
            togglePlayPause(); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.TOGGLE_QUEUE, settings) -> {
            toggleQueue(); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.REPEAT, settings) -> {
            cycleRepeat(); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.SHUFFLE, settings) -> {
            toggleShuffle(); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.VOLUME_UP, settings) -> {
            adjustVolume(5); return EventResult.HANDLED
        }

        event.matchesAction(MeloAction.VOLUME_DOWN, settings) -> {
            adjustVolume(-5); return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}