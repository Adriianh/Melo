package com.github.adriianh.data.player

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.QueueState
import com.github.adriianh.core.domain.player.RepeatMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns [QueueState] and all pure queue mutations (inserts, removals, reordering,
 * shuffle, repeat, and manual-queue accounting).
 */
internal class QueueController {
    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    val current: QueueState get() = _queueState.value

    fun update(transform: (QueueState) -> QueueState) {
        _queueState.update(transform)
    }

    fun setQueue(
        tracks: List<Track>,
        startIndex: Int,
    ): Int {
        val validIndex = if (tracks.isEmpty()) -1 else startIndex.coerceIn(0, tracks.lastIndex)
        _queueState.update {
            it.copy(tracks = tracks, currentIndex = validIndex, userQueueCount = 0)
        }
        return validIndex
    }

    fun selectTrackInQueue(trackId: String): Int {
        val index = _queueState.value.tracks.indexOfFirst { it.id == trackId }
        if (index != -1) {
            _queueState.update { state ->
                val manualStart = state.currentIndex + 1
                val manualEnd = state.currentIndex + state.userQueueCount
                val newCount =
                    when (index) {
                        state.currentIndex -> state.userQueueCount
                        in manualStart..manualEnd -> state.userQueueCount - (index - state.currentIndex)
                        else -> 0
                    }
                state.copy(currentIndex = index, userQueueCount = newCount)
            }
        }
        return index
    }

    fun addToQueue(
        track: Track,
        queueEnded: Boolean,
    ): Boolean {
        val q = _queueState.value
        val shouldAutoStart = q.currentIndex < 0 || q.tracks.isEmpty() || queueEnded
        _queueState.update { state ->
            when {
                state.currentIndex < 0 || state.tracks.isEmpty() -> {
                    state.copy(tracks = listOf(track), currentIndex = 0, userQueueCount = 0)
                }

                queueEnded -> {
                    val newTracks = state.tracks.toMutableList().apply { add(track) }
                    state.copy(tracks = newTracks, currentIndex = newTracks.lastIndex, userQueueCount = 0)
                }

                else -> {
                    val insertIndex =
                        (state.currentIndex + state.userQueueCount + 1).coerceAtMost(state.tracks.size)
                    val newTracks = state.tracks.toMutableList().apply { add(insertIndex, track) }
                    state.copy(tracks = newTracks, userQueueCount = state.userQueueCount + 1)
                }
            }
        }
        return shouldAutoStart
    }

    fun insertToQueue(
        track: Track,
        index: Int,
        queueEnded: Boolean,
    ): Boolean {
        val q = _queueState.value
        val autoStartNext = queueEnded && index > q.currentIndex
        val shouldAutoStart = q.currentIndex < 0 || q.tracks.isEmpty() || autoStartNext
        _queueState.update { state ->
            insertTrackIntoState(state, track, index, autoStartNext)
        }
        return shouldAutoStart
    }

    fun removeFromQueue(index: Int): Boolean? {
        val q = _queueState.value
        if (index !in q.tracks.indices) return null
        val wasCurrent = index == q.currentIndex
        _queueState.update { state ->
            val newTracks = state.tracks.toMutableList().apply { removeAt(index) }
            val newIndex =
                when {
                    index < state.currentIndex -> state.currentIndex - 1
                    index == state.currentIndex -> state.currentIndex.coerceAtMost(newTracks.lastIndex)
                    else -> state.currentIndex
                }
            val manualStart = state.currentIndex + 1
            val manualEnd = state.currentIndex + state.userQueueCount
            val newCount =
                if (index in manualStart..manualEnd && state.userQueueCount > 0) {
                    state.userQueueCount - 1
                } else {
                    state.userQueueCount
                }
            state.copy(tracks = newTracks, currentIndex = newIndex, userQueueCount = newCount)
        }
        return wasCurrent
    }

    fun moveItem(
        fromIndex: Int,
        toIndex: Int,
    ): Boolean {
        val q = _queueState.value
        val invalidIndices =
            fromIndex !in q.tracks.indices || toIndex !in q.tracks.indices || fromIndex == toIndex
        if (invalidIndices) return false

        val manualStart = q.currentIndex + 1
        val manualEnd = q.currentIndex + q.userQueueCount
        val fromInBlock = fromIndex in manualStart..manualEnd
        val toInBlock = toIndex in manualStart..manualEnd
        val newCount =
            when {
                fromInBlock && toInBlock -> q.userQueueCount
                fromInBlock -> (q.userQueueCount - 1).coerceAtLeast(0)
                toInBlock -> q.userQueueCount + 1
                else -> q.userQueueCount
            }

        _queueState.update { state ->
            val newTracks = state.tracks.toMutableList()
            val item = newTracks.removeAt(fromIndex)
            newTracks.add(toIndex, item)

            val newCurrentIndex =
                when (state.currentIndex) {
                    fromIndex -> toIndex
                    in (fromIndex + 1)..toIndex -> state.currentIndex - 1
                    in toIndex..<fromIndex -> state.currentIndex + 1
                    else -> state.currentIndex
                }
            state.copy(
                tracks = newTracks,
                currentIndex = newCurrentIndex,
                userQueueCount = newCount,
            )
        }
        return true
    }

    fun advanceNext(): Boolean {
        val q = _queueState.value
        if (!q.hasNext) return false
        val nextIndex =
            when (q.repeatMode) {
                RepeatMode.ONE -> q.currentIndex
                RepeatMode.ALL -> (q.currentIndex + 1) % q.tracks.size
                RepeatMode.NONE -> q.currentIndex + 1
            }
        _queueState.update { state ->
            val diff = nextIndex - state.currentIndex
            val newCount =
                when {
                    diff == 1 && state.userQueueCount > 0 -> state.userQueueCount - 1
                    diff != 0 -> 0
                    else -> state.userQueueCount
                }
            state.copy(currentIndex = nextIndex, userQueueCount = newCount)
        }
        return true
    }

    fun stepPrevious(): Boolean {
        val q = _queueState.value
        if (!q.hasPrevious) return false
        _queueState.update { state ->
            state.copy(currentIndex = state.currentIndex - 1, userQueueCount = 0)
        }
        return true
    }

    fun toggleShuffle() {
        _queueState.update { q ->
            if (q.shuffleEnabled) {
                q.copy(shuffleEnabled = false)
            } else {
                val shuffled = q.tracks.toMutableList()
                val current = q.currentTrack
                shuffled.shuffle()
                if (current != null) {
                    shuffled.remove(current)
                    shuffled.add(0, current)
                }
                q.copy(
                    tracks = shuffled,
                    currentIndex = 0,
                    shuffleEnabled = true,
                    userQueueCount = 0,
                )
            }
        }
    }

    fun toggleRepeat() {
        _queueState.update {
            it.copy(
                repeatMode =
                    when (it.repeatMode) {
                        RepeatMode.NONE -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.NONE
                    },
            )
        }
    }
}

private fun insertTrackIntoState(
    current: QueueState,
    track: Track,
    index: Int,
    autoStartNext: Boolean,
): QueueState =
    if (current.currentIndex < 0 || current.tracks.isEmpty()) {
        current.copy(tracks = listOf(track), currentIndex = 0, userQueueCount = 0)
    } else {
        val safeIndex = index.coerceIn(0, current.tracks.size)
        val newTracks = current.tracks.toMutableList().apply { add(safeIndex, track) }
        if (autoStartNext) {
            current.copy(tracks = newTracks, currentIndex = safeIndex, userQueueCount = 0)
        } else {
            val newIndex =
                if (safeIndex <= current.currentIndex) current.currentIndex + 1 else current.currentIndex
            val manualRange = (current.currentIndex + 1)..(current.currentIndex + current.userQueueCount + 1)
            val newCount = if (safeIndex in manualRange) current.userQueueCount + 1 else current.userQueueCount
            current.copy(tracks = newTracks, currentIndex = newIndex, userQueueCount = newCount)
        }
    }
