package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent

class JvmMeloPlayer : MeloPlayer {
    private val playerComponent = AudioPlayerComponent()
    private val mediaPlayer = playerComponent.mediaPlayer()
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var progressJob: Job? = null

    init {
        mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer?) {
                _state.update { it.copy(isPlaying = true) }
                startProgressUpdate()
            }

            override fun paused(mediaPlayer: MediaPlayer?) {
                _state.update { it.copy(isPlaying = false) }
                stopProgressUpdate()
            }

            override fun stopped(mediaPlayer: MediaPlayer?) {
                _state.update { it.copy(isPlaying = false) }
                stopProgressUpdate()
            }

            override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) {
                _state.update { it.copy(durationMs = newLength) }
            }
        })
    }

    override fun load(url: String, track: Track) {
        // Stop current playback if any
        mediaPlayer.controls().stop()

        // Update state to indicate loading/buffering
        _state.update {
            it.copy(
                currentTrack = track,
                progressMs = 0,
                durationMs = track.durationMs,
                isBuffering = true,
                error = null
            )
        }

        // Play with User-Agent option (important for YouTube)
        val userAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        mediaPlayer.media().play(url, ":http-user-agent=$userAgent")
    }

    override fun play() {
        mediaPlayer.controls().play()
    }

    override fun pause() {
        mediaPlayer.controls().pause()
    }

    override fun stop() {
        mediaPlayer.controls().stop()
    }

    override fun seekTo(positionMs: Long) {
        mediaPlayer.controls().setTime(positionMs)
    }

    override fun release() {
        stopProgressUpdate()
        playerComponent.release()
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                _state.update { it.copy(progressMs = mediaPlayer.status().time()) }
                delay(1000)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }
}
