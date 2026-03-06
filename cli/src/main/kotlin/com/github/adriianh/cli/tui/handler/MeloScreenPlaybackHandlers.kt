package com.github.adriianh.cli.tui.handler

import  com.github.adriianh.cli.tui.*
import com.github.adriianh.cli.tui.util.LrcParser
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.*

internal fun MeloScreen.playTrack(track: Track) {
    val existingIndex = state.queue.indexOfFirst { it.id == track.id }
    val (newQueue, newIndex, newRadioMode) = when {
        state.isRadioMode && existingIndex < 0 -> Triple(listOf(track), 0, false)
        existingIndex >= 0 -> Triple(state.queue, existingIndex, state.isRadioMode)
        else -> {
            val insertAt = (state.queueIndex + 1).coerceAtLeast(0)
            val q = state.queue.toMutableList().also { it.add(insertAt, track) }
            Triple(q, insertAt, false)
        }
    }
    state = state.copy(
        selectedTrack = track, nowPlaying = track,
        isPlaying = false, isLoadingAudio = true,
        audioError = null, progress = 0.0, marqueeOffset = 0,
        queue = newQueue, queueIndex = newIndex, isRadioMode = newRadioMode,
        syncedLyrics = emptyList(), isLoadingSyncedLyrics = true, nowPlayingPositionMs = 0L,
        nowPlayingArtwork = null,
    )
    marqueeTick = 0
    audioPlayer.stop()
    loadTrackDetails(track.id, track)
    scope.launch {
        val artworkUrl = track.artworkUrl ?: getTrack(track.id)?.artworkUrl
        val artwork = artworkUrl?.let { artworkRenderer.load(it) }
        appRunner()?.runOnRenderThread {
            if (state.nowPlaying?.id == track.id) {
                state = state.copy(nowPlayingArtwork = artwork)
            }
        }
    }
    scope.launch {
        recordPlay(track)
        val url = getStream(track)
        appRunner()?.runOnRenderThread {
            if (url == null) {
                state = state.copy(isLoadingAudio = false, audioError = "Stream not available")
                return@runOnRenderThread
            }
            state = state.copy(isPlaying = true, isLoadingAudio = false)
            audioPlayer.play(url)
            mediaSession.updateTrack(track, track.durationMs)
        }
        val lrc = getSyncedLyrics(track.artist, track.title)
        appRunner()?.runOnRenderThread {
            state = state.copy(
                syncedLyrics = if (lrc != null) LrcParser.parse(lrc) else emptyList(),
                isLoadingSyncedLyrics = false,
            )
        }
    }
    checkIsFavorite(track.id)
}

internal fun MeloScreen.togglePlayPause() {
    if (state.nowPlaying == null || state.isLoadingAudio) return
    if (state.isPlaying) {
        audioPlayer.pause()
        state = state.copy(isPlaying = false)
        mediaSession.notifyPaused()
    } else {
        audioPlayer.resume()
        state = state.copy(isPlaying = true)
        mediaSession.notifyResumed()
    }
}

internal fun MeloScreen.adjustVolume(delta: Int) {
    val newVol = (state.volume + delta).coerceIn(0, 100)
    state = state.copy(volume = newVol)
    audioPlayer.setVolume(newVol)
}

internal fun MeloScreen.seekTo(progress: Double) {
    val duration = state.nowPlaying?.durationMs ?: return
    if (state.isLoadingAudio) return
    val clamped = progress.coerceIn(0.0, 1.0)
    state = state.copy(progress = clamped)
    audioPlayer.seek((clamped * duration).toLong())
}

internal fun MeloScreen.seekBackward() {
    if (state.isLoadingAudio) return
    val elapsedMs = (state.progress * (state.nowPlaying?.durationMs ?: 0L)).toLong()
    if (elapsedMs > 3000L || state.queueIndex <= 0) state.nowPlaying?.let { playTrack(it) }
    else playFromQueue(state.queueIndex - 1)
}

internal fun MeloScreen.seekForward() {
    val queue = state.queue
    val isAtLast = queue.isNotEmpty() && state.queueIndex + 1 >= queue.size
    if (state.isLoadingAudio && !isAtLast) return
    if (queue.isEmpty()) return

    val nextIndex = when {
        state.repeatMode == RepeatMode.ONE -> state.queueIndex
        state.repeatMode == RepeatMode.ALL -> (state.queueIndex + 1) % queue.size
        state.shuffleEnabled && queue.size > 1 -> queue.indices.filter { it != state.queueIndex }.random()
        else -> {
            val next = state.queueIndex + 1
            if (next >= queue.size) { loadSimilarAndPlay(); return }
            next
        }
    }
    playFromQueue(nextIndex)
}

internal fun MeloScreen.playFromQueue(index: Int) {
    val track = state.queue.getOrNull(index) ?: return
    state = state.copy(queueIndex = index)
    playTrack(track)
}
