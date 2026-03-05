package com.github.adriianh.cli.tui.handler
import com.github.adriianh.cli.tui.*

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
    )
    marqueeTick = 0
    audioPlayer.stop()
    loadTrackDetails(track.id, track)
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
        }
    }
    checkIsFavorite(track.id)
}

internal fun MeloScreen.togglePlayPause() {
    if (state.nowPlaying == null || state.isLoadingAudio) return
    if (state.isPlaying) { audioPlayer.pause(); state = state.copy(isPlaying = false) }
    else { audioPlayer.resume(); state = state.copy(isPlaying = true) }
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

internal fun MeloScreen.addToQueue(track: Track) {
    val newQueue = state.queue + track
    val newIndex = if (state.queueIndex < 0 && state.nowPlaying == null) 0 else state.queueIndex
    state = state.copy(queue = newQueue, queueIndex = newIndex, isRadioMode = false)
    if (state.nowPlaying == null && !state.isLoadingAudio) playFromQueue(0)
}

internal fun MeloScreen.removeFromQueue(index: Int) {
    if (index < 0 || index >= state.queue.size) return
    val removingPlaying = index == state.queueIndex
    val newQueue = state.queue.toMutableList().also { it.removeAt(index) }
    val newIndex = when {
        newQueue.isEmpty() -> -1
        index < state.queueIndex -> state.queueIndex - 1
        index == state.queueIndex -> minOf(index, newQueue.lastIndex)
        else -> state.queueIndex
    }
    state = state.copy(
        queue = newQueue,
        queueIndex = newIndex,
        queueCursor = minOf(state.queueCursor, (newQueue.size - 1).coerceAtLeast(0)),
    )
    if (removingPlaying) {
        audioPlayer.stop()
        if (newQueue.isEmpty()) state = state.copy(nowPlaying = null, isPlaying = false, progress = 0.0, isRadioMode = false)
        else playFromQueue(if (newIndex >= 0 && newIndex < newQueue.size) newIndex else 0)
    }
}

internal fun MeloScreen.clearQueue() {
    audioPlayer.stop()
    state = state.copy(queue = emptyList(), queueIndex = -1, queueCursor = 0,
        nowPlaying = null, isPlaying = false, progress = 0.0, isRadioMode = false)
}

internal fun MeloScreen.toggleQueue() { state = state.copy(isQueueVisible = !state.isQueueVisible) }
internal fun MeloScreen.toggleShuffle() { state = state.copy(shuffleEnabled = !state.shuffleEnabled) }
internal fun MeloScreen.cycleRepeat() {
    state = state.copy(repeatMode = when (state.repeatMode) {
        RepeatMode.OFF -> RepeatMode.ALL
        RepeatMode.ALL -> RepeatMode.ONE
        RepeatMode.ONE -> RepeatMode.OFF
    })
}

internal fun MeloScreen.loadSimilarAndPlay() {
    val seed = state.nowPlaying ?: return
    val alreadyPlayed = state.queue.map { it.id }.toSet()
    audioPlayer.stop()
    state = state.copy(isPlaying = false, isLoadingAudio = true, isRadioMode = true, progress = 0.0)
    scope.launch {
        try {
            val similar = getSimilarTracks(seed.artist, seed.title)
            if (similar.isEmpty()) {
                appRunner()?.runOnRenderThread { state = state.copy(isLoadingAudio = false, isRadioMode = false) }
                return@launch
            }
            val resolved = coroutineScope {
                similar.shuffled().take(15)
                    .map { st -> async { runCatching { searchTracks("${st.artist} ${st.title}").firstOrNull() }.getOrNull() } }
                    .awaitAll()
            }.filterNotNull().filter { it.id !in alreadyPlayed }.distinctBy { it.id }.take(10)

            if (resolved.isEmpty()) {
                appRunner()?.runOnRenderThread { state = state.copy(isLoadingAudio = false, isRadioMode = false) }
                return@launch
            }
            appRunner()?.runOnRenderThread {
                state = state.copy(queue = resolved, queueIndex = -1, queueCursor = 0, isLoadingAudio = false, isRadioMode = true)
                playFromQueue(0)
            }
        } catch (e: Exception) {
            appRunner()?.runOnRenderThread {
                state = state.copy(isPlaying = false, isLoadingAudio = false, audioError = e.message, isRadioMode = false)
            }
        }
    }
}

