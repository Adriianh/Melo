package com.github.adriianh.melo.ui.player

import androidx.lifecycle.ViewModel
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.PlaybackManager

class QueueViewModel(private val manager: PlaybackManager) : ViewModel() {
    val queueState = manager.queueState

    fun playTrack(track: Track) = manager.playTrack(track)

    fun addToQueue(track: Track) = manager.addToQueue(track)

    fun playTrackInQueue(track: Track) = manager.playTrackInQueue(track)

    fun toggleShuffle() = manager.toggleShuffle()

    fun toggleRepeat() = manager.toggleRepeat()
}