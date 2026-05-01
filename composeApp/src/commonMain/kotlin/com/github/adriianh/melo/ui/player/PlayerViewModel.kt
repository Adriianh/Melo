package com.github.adriianh.melo.ui.player

import androidx.lifecycle.ViewModel
import com.github.adriianh.core.domain.player.PlaybackManager

class PlayerViewModel(private val manager: PlaybackManager) : ViewModel() {
    val playbackState = manager.playbackState

    fun togglePlayPause() = manager.togglePlayPause()

    fun seekTo(positionMs: Long) = manager.seekTo(positionMs)

    fun playNext() = manager.playNext()

    fun playPrevious() = manager.playPrevious()
}