package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.volume
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalForeignApi::class)
class IosMeloPlayer : MeloPlayer {
    private val player = AVPlayer()
    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    override fun load(url: String, track: Track, initialPositionMs: Long) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        val playerItem = AVPlayerItem.playerItemWithURL(nsUrl)
        player.replaceCurrentItemWithPlayerItem(playerItem)
        if (initialPositionMs > 0) {
            val time = CMTimeMakeWithSeconds(initialPositionMs / 1000.0, 1000)
            player.seekToTime(time)
        }
        _state.update {
            it.copy(
                currentTrack = track,
                progressMs = initialPositionMs,
                durationMs = track.durationMs
            )
        }
        play()
        startProgressUpdate()
    }

    override fun play() {
        player.play()
        _state.update { it.copy(isPlaying = true) }
    }

    override fun pause() {
        player.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    override fun stop() {
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        _state.update { it.copy(isPlaying = false, currentTrack = null) }
    }

    override fun seekTo(positionMs: Long) {
        val time = CMTimeMakeWithSeconds(positionMs / 1000.0, 1000)
        player.seekToTime(time)
    }

    override fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    override fun release() {
        stopProgressUpdate()
        stop()
    }

    override fun setIdleTrack(track: Track, initialPositionMs: Long) {
        _state.update {
            it.copy(
                currentTrack = track,
                progressMs = initialPositionMs,
                durationMs = track.durationMs,
                isPlaying = false,
                isBuffering = false,
                isFinished = false,
                error = null
            )
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val currentItem = player.currentItem
                if (currentItem != null) {
                    val currentTime = CMTimeGetSeconds(player.currentTime())
                    if (!currentTime.isNaN()) {
                        _state.update { it.copy(progressMs = (currentTime * 1000).toLong()) }
                    }
                }
                delay(1000.milliseconds)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }
}
