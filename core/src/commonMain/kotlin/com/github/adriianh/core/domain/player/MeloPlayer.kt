package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface MeloPlayer {
    val state: StateFlow<PlaybackState>

    fun load(url: String, track: Track, initialPositionMs: Long = 0)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun release()
    fun setIdleTrack(track: Track, initialPositionMs: Long = 0)
}
