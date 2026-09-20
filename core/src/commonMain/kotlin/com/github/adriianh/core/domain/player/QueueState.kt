package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track

enum class RepeatMode {
    NONE,
    ONE,
    ALL
}

data class QueueState(
    val tracks: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,

    /**
     * Number of manually queued ("play next") tracks sitting right after the
     * current track. Radio expansions are inserted behind this block so the
     * user's manual picks play before the radio continuation.
     */
    val userQueueCount: Int = 0,
) {
    val currentTrack: Track? get() = tracks.getOrNull(currentIndex)

    val hasNext: Boolean
        get() = when (repeatMode) {
            RepeatMode.NONE -> currentIndex < tracks.lastIndex
            RepeatMode.ONE -> true
            RepeatMode.ALL -> tracks.isNotEmpty()
        }

    val hasPrevious: Boolean get() = currentIndex > 0
}