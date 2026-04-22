package com.github.adriianh.melo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.player.MeloPlayer
import com.github.adriianh.core.domain.player.PlaybackState
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val meloPlayer: MeloPlayer,
    private val getStreamUseCase: GetStreamUseCase
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = meloPlayer.state

    fun playTrack(track: Track) {
        viewModelScope.launch {
            val url = getStreamUseCase(track)
            if (url != null) {
                meloPlayer.load(url, track)
            }
        }
    }

    fun togglePlayPause() {
        if (playbackState.value.isPlaying) {
            meloPlayer.pause()
        } else if (playbackState.value.currentTrack != null) {
            meloPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        meloPlayer.seekTo(positionMs)
    }

    override fun onCleared() {
        super.onCleared()
        meloPlayer.release()
    }
}
