package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface MeloPlayer {
    val state: StateFlow<PlaybackState>

    fun load(url: String, track: Track)
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun release()
}
