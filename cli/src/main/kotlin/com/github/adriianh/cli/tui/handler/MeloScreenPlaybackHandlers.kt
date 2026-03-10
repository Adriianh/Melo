package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.*
import com.github.adriianh.cli.tui.util.LrcParser
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal fun MeloScreen.playTrack(track: Track) {
    val existingIndex = state.player.queue.indexOfFirst { it.id == track.id }
    val (newQueue, newIndex, newRadioMode) = when {
        state.player.isRadioMode && existingIndex < 0 -> Triple(listOf(track), 0, false)
        existingIndex >= 0 -> Triple(state.player.queue, existingIndex, state.player.isRadioMode)
        else -> {
            val insertAt = (state.player.queueIndex + 1).coerceAtLeast(0)
            val q = state.player.queue.toMutableList().also { it.add(insertAt, track) }
            Triple(q, insertAt, false)
        }
    }
    state = state.copy(
        player = state.player.copy(
            nowPlaying = track,
            isPlaying = false, isLoadingAudio = true,
            audioError = null,
            queue = newQueue, queueIndex = newIndex, isRadioMode = newRadioMode,
            syncedLyrics = emptyList(), isLoadingSyncedLyrics = true, nowPlayingPositionMs = 0L,
            nowPlayingArtwork = null,
            progress = 0.0, marqueeOffset = 0,
        )
    )
    marqueeTick = 0
    scrobbleSubmitted = false
    trackStartedAt = System.currentTimeMillis()
    audioPlayer.stop()
    loadNowPlayingMetadata(track)
    scope.launch {
        recordPlay(track)
        var url: String? = null
        var attempts = 0
        val maxAttempts = 3
        while (attempts < maxAttempts && url == null) {
            url = getStream(track)
            if (url == null) delay(700L)
            attempts++
        }
        appRunner()?.runOnRenderThread {
            if (url == null) {
                state = state.copy(player = state.player.copy(isLoadingAudio = false, audioError = "Stream not available, skipping..."))
                seekForward()
                return@runOnRenderThread
            }
            state = state.copy(player = state.player.copy(isPlaying = true, isLoadingAudio = false))
            audioPlayer.play(url)
            mediaSession.updateTrack(track, track.durationMs)
            onTrackStarted(track)
        }
        val lrc = getSyncedLyrics(track.artist, track.title)
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
    } else {
        audioPlayer.resume()
        state = state.copy(player = state.player.copy(isPlaying = true))
        mediaSession.notifyResumed()
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
            if (next >= queue.size) { loadSimilarAndPlay(); return }
            next
        }
    }
    playFromQueue(nextIndex)
}

internal fun MeloScreen.playFromQueue(index: Int) {
    val track = state.player.queue.getOrNull(index) ?: return
    state = state.copy(player = state.player.copy(queueIndex = index))
    playTrack(track)
}

internal fun MeloScreen.addToQueue(track: Track) {
    val newQueue = state.player.queue + track
    val newIndex = if (state.player.queueIndex < 0 && state.player.nowPlaying == null) 0 else state.player.queueIndex
    state = state.copy(player = state.player.copy(queue = newQueue, queueIndex = newIndex, isRadioMode = false))
    if (state.player.nowPlaying == null && !state.player.isLoadingAudio) playFromQueue(0)
}

internal fun MeloScreen.removeFromQueue(index: Int) {
    if (index < 0 || index >= state.player.queue.size) return
    val removingPlaying = index == state.player.queueIndex
    val newQueue = state.player.queue.toMutableList().also { it.removeAt(index) }
    val newIndex = when {
        newQueue.isEmpty() -> -1
        index < state.player.queueIndex -> state.player.queueIndex - 1
        index == state.player.queueIndex -> minOf(index, newQueue.lastIndex)
        else -> state.player.queueIndex
    }
    state = state.copy(
        player = state.player.copy(
            queue = newQueue,
            queueIndex = newIndex,
            queueCursor = minOf(state.player.queueCursor, (newQueue.size - 1).coerceAtLeast(0)),
        )
    )
    if (removingPlaying) {
        audioPlayer.stop()
        if (newQueue.isEmpty()) state = state.copy(player = state.player.copy(nowPlaying = null, isPlaying = false, isRadioMode = false, progress = 0.0))
        else playFromQueue(if (newIndex >= 0 && newIndex < newQueue.size) newIndex else 0)
    }
}

internal fun MeloScreen.clearQueue() {
    audioPlayer.stop()
    state = state.copy(
        player = state.player.copy(
            queue = emptyList(), queueIndex = -1, queueCursor = 0,
            nowPlaying = null, isPlaying = false, isRadioMode = false,
            progress = 0.0,
        ),
    )
}

internal fun MeloScreen.toggleQueue() {
    val nowVisible = !state.player.isQueueVisible
    state = state.copy(player = state.player.copy(isQueueVisible = nowVisible))
    if (nowVisible) appRunner()?.focusManager()?.setFocus("queue-panel")
}

internal fun MeloScreen.toggleShuffle() { state = state.copy(player = state.player.copy(shuffleEnabled = !state.player.shuffleEnabled)) }

internal fun MeloScreen.cycleRepeat() {
    state = state.copy(
        player = state.player.copy(
            repeatMode = when (state.player.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
        )
    )
}

internal fun MeloScreen.loadSimilarAndPlay() {
    val seed = state.player.nowPlaying ?: return
    val alreadyPlayed = state.player.queue.map { it.id }.toSet()
    state = state.copy(player = state.player.copy(isPlaying = false, isLoadingAudio = true, isRadioMode = true, progress = 0.0))
    scope.launch {
        try {
            val related = resolveSimilarTracks(seed, limit = 15)
                .filter { it.id !in alreadyPlayed }
                .distinctBy { it.id }
                .shuffled()
                .take(10)

            if (related.isEmpty()) {
                appRunner()?.runOnRenderThread { state = state.copy(player = state.player.copy(isLoadingAudio = false, isRadioMode = false)) }
                return@launch
            }
            appRunner()?.runOnRenderThread {
                state = state.copy(player = state.player.copy(queue = related, queueIndex = -1, queueCursor = 0, isLoadingAudio = false, isRadioMode = true))
                playFromQueue(0)
            }
        } catch (e: Exception) {
            appRunner()?.runOnRenderThread {
                state = state.copy(player = state.player.copy(isPlaying = false, isLoadingAudio = false, audioError = e.message, isRadioMode = false))
            }
        }
    }
}

internal fun MeloScreen.loadMoreRadioTracks() {
    if (!state.player.isRadioMode || state.player.isLoadingMoreRadio || state.player.queue.isEmpty()) return
    
    val seed = state.player.queue.last()
    val alreadyPlayed = state.player.queue.map { it.id }.toSet()
    state = state.copy(player = state.player.copy(isLoadingMoreRadio = true))
    
    scope.launch {
        try {
            val related = resolveSimilarTracks(seed, limit = 10, offset = 0)
                .filter { it.id !in alreadyPlayed }
                .distinctBy { it.id }
                .shuffled()
                .take(10)
            
            if (isActive) appRunner()?.runOnRenderThread {
                if (related.isNotEmpty()) {
                    val combined = state.player.queue + related
                    val (pruned, newIdx) = pruneQueue(combined, state.player.queueIndex)
                    state = state.copy(
                        player = state.player.copy(
                            queue = pruned,
                            queueIndex = newIdx,
                            queueCursor = minOf(state.player.queueCursor, (pruned.size - 1).coerceAtLeast(0)),
                            isLoadingMoreRadio = false
                        )
                    )
                } else {
                    state = state.copy(player = state.player.copy(isLoadingMoreRadio = false))
                }
            }
        } catch (_: Exception) {
            if (isActive) appRunner()?.runOnRenderThread { 
                state = state.copy(player = state.player.copy(isLoadingMoreRadio = false)) 
            }
        }
    }
}

/** Max tracks to keep in the radio queue; older played tracks are evicted. */
private const val MAX_QUEUE_SIZE = 50

/**
 * Prunes already-played tracks from the front of the queue when it exceeds [MAX_QUEUE_SIZE].
 * Returns the pruned list and the adjusted queueIndex.
 */
private fun pruneQueue(queue: List<com.github.adriianh.core.domain.model.Track>, currentIndex: Int): Pair<List<com.github.adriianh.core.domain.model.Track>, Int> {
    if (queue.size <= MAX_QUEUE_SIZE || currentIndex <= 0) return queue to currentIndex
    // Keep a small buffer of ~5 played tracks for "previous" navigation
    val dropCount = (currentIndex - 5).coerceAtLeast(0)
    if (dropCount == 0) return queue to currentIndex
    return queue.drop(dropCount) to (currentIndex - dropCount)
}