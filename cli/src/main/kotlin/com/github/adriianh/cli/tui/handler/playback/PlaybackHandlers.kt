package com.github.adriianh.cli.tui.handler.playback

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.RepeatMode
import com.github.adriianh.cli.tui.handler.checkIsFavorite
import com.github.adriianh.cli.tui.handler.matchesAction
import com.github.adriianh.cli.tui.handler.onTrackStarted
import com.github.adriianh.cli.tui.handler.search.loadNowPlayingMetadata
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.LrcParser
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal fun MeloScreen.playTrack(track: Track) {
    if (!state.isPlayable(track)) return

    val currentIndex = state.player.queueIndex
    val isCurrentTrack =
        currentIndex in state.player.queue.indices && state.player.queue[currentIndex].id == track.id
    val existingIndex =
        if (isCurrentTrack) currentIndex else state.player.queue.indexOfFirst { it.id == track.id }

    val (newQueue, newIndex, newRadioMode, newUserQueueCount) = when {
        existingIndex >= 0 -> listOf(state.player.queue, existingIndex, state.player.isRadioMode, state.player.userQueueCount)
        else -> listOf(listOf(track), 0, true, 0)
    }

    @Suppress("UNCHECKED_CAST")
    state = state.copy(
        player = state.player.copy(
            nowPlaying = track,
            isPlaying = false, isLoadingAudio = true,
            audioError = null,
            queue = newQueue as List<Track>,
            queueIndex = newIndex as Int,
            isRadioMode = newRadioMode as Boolean,
            userQueueCount = newUserQueueCount as Int,
            syncedLyrics = emptyList(), isLoadingSyncedLyrics = true, nowPlayingPositionMs = 0L,
            nowPlayingArtwork = null,
            progress = 0.0, marqueeOffset = 0,
        )
    )

    marqueeTick = 0
    scrobbleSubmitted = false
    playRecorded = false
    trackStartedAt = System.currentTimeMillis()
    audioPlayer.stop()

    if (state.player.isRadioMode && !state.player.isLoadingMoreRadio && state.player.queueIndex >= state.player.queue.size - 3) {
        loadMoreRadioTracks()
    }
    loadNowPlayingMetadata(track)

    resolveStreamJob?.cancel()
    resolveStreamJob = scope.launch {
        var resolvedTrack = track
        if (resolvedTrack.durationMs <= 0L) {
            try {
                getTrack(resolvedTrack.id)?.let { fetched ->
                    resolvedTrack = fetched
                    appRunner()?.runOnRenderThread {
                        val currentQ = state.player.queue.toMutableList()
                        if (state.player.queueIndex in currentQ.indices) {
                            currentQ[state.player.queueIndex] = fetched
                        }
                        if (state.player.nowPlaying?.id == fetched.id) {
                            state = state.copy(
                                player = state.player.copy(
                                    nowPlaying = fetched,
                                    queue = currentQ
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        val queue = state.player.queue
        val nextTracks =
            (1..2).mapNotNull { offset -> queue.getOrNull(state.player.queueIndex + offset) }

        markTrackAccessed(resolvedTrack.id)

        if (settingsViewState.currentSettings.autoDownload) {
            nextTracks.forEach { nextTrack -> launch(Dispatchers.IO) { downloadTrack(nextTrack) } }
        }

        nextTracks
            .filter { offlineRepository.getOfflineTrack(it.id)?.downloadStatus != DownloadStatus.COMPLETED }
            .forEach { nextTrack ->
                launch(Dispatchers.IO) {
                    getStream(nextTrack)
                }
            }

        var url: String? = null
        var attempts = 0
        val maxAttempts = 3
        while (attempts < maxAttempts && url == null) {
            url = getStream(resolvedTrack)
            if (url == null) delay(700L)
            attempts++
        }

        appRunner()?.runOnRenderThread {
            if (url == null) {
                state = state.copy(
                    player = state.player.copy(
                        isLoadingAudio = false,
                        audioError = "Stream not available, skipping..."
                    )
                )
                seekForward()
                return@runOnRenderThread
            }
            state = state.copy(player = state.player.copy(isPlaying = true, isLoadingAudio = false))
            audioPlayer.play(url)
            mediaSession.updateTrack(resolvedTrack, resolvedTrack.durationMs)
            if (settingsViewState.currentSettings.discordRpcEnabled) {
                discordRpcManager.updateActivity(resolvedTrack, true)
            }
            onTrackStarted(resolvedTrack)
        }

        val lrc = getSyncedLyrics(resolvedTrack.artist, resolvedTrack.title)
        appRunner()?.runOnRenderThread {
            state = state.copy(
                player = state.player.copy(
                    syncedLyrics = if (lrc != null) LrcParser.parse(lrc) else emptyList(),
                    isLoadingSyncedLyrics = false,
                )
            )
        }
    }

    checkIsFavorite(track.id)
}

internal fun MeloScreen.togglePlayPause() {
    if (state.player.nowPlaying == null || state.player.isLoadingAudio) return
    if (state.player.isPlaying) {
        audioPlayer.pause()
        state = state.copy(player = state.player.copy(isPlaying = false))
        mediaSession.notifyPaused()
        if (settingsViewState.currentSettings.discordRpcEnabled) {
            discordRpcManager.updateActivity(state.player.nowPlaying, false)
        }
    } else {
        audioPlayer.resume()
        state = state.copy(player = state.player.copy(isPlaying = true))
        mediaSession.notifyResumed()
        if (settingsViewState.currentSettings.discordRpcEnabled) {
            val elapsedMs = (state.player.progress * (state.player.nowPlaying?.durationMs ?: 0L)).toLong()
            discordRpcManager.updateActivity(state.player.nowPlaying, true, elapsedMs)
        }
    }
}

internal fun MeloScreen.adjustVolume(delta: Int) {
    val newVol = (state.player.volume + delta).coerceIn(0, 100)
    state = state.copy(player = state.player.copy(volume = newVol))
    audioPlayer.setVolume(newVol)
}

internal fun MeloScreen.seekTo(progress: Double) {
    val duration = state.player.nowPlaying?.durationMs ?: return
    if (state.player.isLoadingAudio) return
    val clamped = progress.coerceIn(0.0, 1.0)
    state = state.copy(player = state.player.copy(progress = clamped))
    audioPlayer.seek((clamped * duration).toLong())
    if (settingsViewState.currentSettings.discordRpcEnabled) {
        discordRpcManager.updateActivity(state.player.nowPlaying, state.player.isPlaying, (clamped * duration).toLong())
    }
}

internal fun MeloScreen.seekBackward() {
    if (state.player.isLoadingAudio) return
    val elapsedMs = (state.player.progress * (state.player.nowPlaying?.durationMs ?: 0L)).toLong()
    if (elapsedMs > 3000L || state.player.queueIndex <= 0) state.player.nowPlaying?.let { playTrack(it) }
    else playFromQueue(state.player.queueIndex - 1)
}

internal fun MeloScreen.seekForward() {
    val queue = state.player.queue
    val isAtLast = queue.isNotEmpty() && state.player.queueIndex + 1 >= queue.size
    if (state.player.isLoadingAudio && !isAtLast) return
    if (queue.isEmpty()) return

    val nextIndex = when {
        state.player.repeatMode == RepeatMode.ONE -> state.player.queueIndex
        state.player.repeatMode == RepeatMode.ALL -> (state.player.queueIndex + 1) % queue.size
        state.player.shuffleEnabled && queue.size > 1 -> queue.indices.filter { it != state.player.queueIndex }.random()
        else -> {
            val next = state.player.queueIndex + 1
            if (next >= queue.size) {
                if (state.player.isRadioMode) {
                    loadSimilarAndPlay()
                } else {
                    audioPlayer.stop()
                    state = state.copy(
                        player = state.player.copy(
                            nowPlaying = null,
                            isPlaying = false,
                            progress = 0.0,
                            syncedLyrics = emptyList(),
                            isLoadingSyncedLyrics = false
                        )
                    )
                }
                return
            }
            next
        }
    }

    val previousIndex = state.player.queueIndex
    val diff = nextIndex - previousIndex
    val newUserQueueCount = if (diff == 1 && state.player.userQueueCount > 0) {
        state.player.userQueueCount - 1
    } else if (diff != 0) {
        0
    } else {
        state.player.userQueueCount
    }

    state = state.copy(player = state.player.copy(userQueueCount = newUserQueueCount))

    playFromQueue(nextIndex)
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
            queue = playableTracks,
            queueIndex = -1,
            isRadioMode = false,
            userQueueCount = 0
        )
    )
    playFromQueue(newStartIndex)
}

internal fun MeloScreen.playFromQueue(index: Int) {
    val track = state.player.queue.getOrNull(index) ?: return
    state = state.copy(player = state.player.copy(queueIndex = index))
    playTrack(track)
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

        event.code() == KeyCode.CHAR && (event.character() == '<' || event.character() == ',') -> {
            seekTo(state.player.progress - 0.05); return EventResult.HANDLED
        }

        event.code() == KeyCode.CHAR && (event.character() == '>' || event.character() == '.') -> {
            seekTo(state.player.progress + 0.05); return EventResult.HANDLED
        }

        event.matchesAction(
            MeloAction.PLAY_PAUSE,
            settings
        ) || (event.code() == KeyCode.CHAR && event.character() == ' ') -> {
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