package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.Color
import com.github.adriianh.core.domain.player.PlaybackState
import com.github.adriianh.core.domain.player.QueueState
import com.github.adriianh.core.domain.player.RepeatMode

data class PlayerUiState(
    val title: String = "",
    val artist: String = "",
    val albumArt: String? = null,
    val isPlaying: Boolean = false,
    val progressFraction: Float = 0f,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val elapsedLabel: String = "0:00",
    val remainingLabel: String = "0:00",
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val accentColor: Color = MeloColors.textMuted,
    val lyrics: String? = null,
) {
    val hasTrack: Boolean get() = title.isNotEmpty()

    companion object {
        fun from(
            playback: PlaybackState,
            queue: QueueState,
            accentColor: Color = MeloColors.textMuted,
        ): PlayerUiState {
            val track = playback.currentTrack
            val fraction = if (playback.durationMs > 0) {
                (playback.progressMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)
            } else 0f

            return PlayerUiState(
                title = track?.title ?: "",
                artist = track?.artist ?: "",
                albumArt = track?.artworkUrl,
                isPlaying = playback.isPlaying,
                progressFraction = fraction,
                progressMs = playback.progressMs,
                durationMs = playback.durationMs,
                elapsedLabel = formatTime(playback.progressMs),
                remainingLabel = formatTime(playback.durationMs - playback.progressMs),
                shuffleEnabled = queue.shuffleEnabled,
                repeatMode = queue.repeatMode,
                hasNext = queue.hasNext,
                hasPrevious = queue.hasPrevious,
                accentColor = accentColor,
            )
        }

        private fun formatTime(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "$hours:${minutes.toString().padStart(2, '0')}:${
                    seconds.toString().padStart(2, '0')
                }"
            } else {
                "$minutes:${seconds.toString().padStart(2, '0')}"
            }
        }
    }
}