package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private val EMPTY_PLAYER_AUDIO_OUTPUTS: StateFlow<List<AudioOutputDevice>> = MutableStateFlow(emptyList())
private val NULL_PLAYER_AUDIO_OUTPUT_ID: StateFlow<String?> = MutableStateFlow(null)

interface MeloPlayer {
    val state: StateFlow<PlaybackState>
    val availableAudioOutputs: StateFlow<List<AudioOutputDevice>>
        get() = EMPTY_PLAYER_AUDIO_OUTPUTS
    val selectedAudioOutputId: StateFlow<String?>
        get() = NULL_PLAYER_AUDIO_OUTPUT_ID

    fun load(
        url: String,
        track: Track,
        initialPositionMs: Long = 0,
    )

    fun play()

    fun pause()

    fun stop()

    fun seekTo(positionMs: Long)

    fun setVolume(volume: Float)

    fun refreshAudioOutputs() = Unit

    fun selectAudioOutput(deviceId: String?): Boolean = false

    fun release()

    fun setIdleTrack(
        track: Track,
        initialPositionMs: Long = 0,
    )
}
