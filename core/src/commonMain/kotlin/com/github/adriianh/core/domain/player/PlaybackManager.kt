package com.github.adriianh.core.domain.player

import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

private val EMPTY_MANAGER_AUDIO_OUTPUTS: StateFlow<List<AudioOutputDevice>> = MutableStateFlow(emptyList())
private val NULL_MANAGER_AUDIO_OUTPUT_ID: StateFlow<String?> = MutableStateFlow(null)

interface PlaybackManager {
    val playbackState: StateFlow<PlaybackState>
    val queueState: StateFlow<QueueState>
    val volume: StateFlow<Float>
    val availableAudioOutputs: StateFlow<List<AudioOutputDevice>>
        get() = EMPTY_MANAGER_AUDIO_OUTPUTS
    val selectedAudioOutputId: StateFlow<String?>
        get() = NULL_MANAGER_AUDIO_OUTPUT_ID

    /** One-shot events: play failures with skip, tracks that started, etc. */
    val events: SharedFlow<PlaybackEvent>

    fun playTrack(track: Track)

    fun playTrackInQueue(track: Track)

    fun setQueue(
        tracks: List<Track>,
        startIndex: Int = 0,
    )

    fun addToQueue(track: Track)

    fun insertToQueue(
        track: Track,
        index: Int,
    )

    fun removeFromQueue(index: Int)

    fun moveQueueItem(
        fromIndex: Int,
        toIndex: Int,
    )

    fun playNext()

    fun playPrevious()

    fun togglePlayPause()

    fun toggleShuffle()

    fun toggleRepeat()

    fun seekTo(positionMs: Long)

    fun setVolume(volume: Float)

    fun toggleMute()

    fun refreshAudioOutputs() = Unit

    fun selectAudioOutput(deviceId: String?) = Unit

    fun release()
}
