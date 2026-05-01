package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface PlaybackManager {
    val playbackState: StateFlow<PlaybackState>
    val queueState: StateFlow<QueueState>

    fun playTrack(track: Track)
    fun playTrackInQueue(track: Track)
    fun setQueue(tracks: List<Track>, startIndex: Int = 0)
    fun addToQueue(track: Track)
    fun playNext()
    fun playPrevious()
    fun togglePlayPause()
    fun toggleShuffle()
    fun toggleRepeat()
    fun seekTo(positionMs: Long)
    fun release()
}