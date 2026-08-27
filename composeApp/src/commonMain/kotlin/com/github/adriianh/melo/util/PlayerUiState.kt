package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.Color
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.core.domain.player.PlaybackState
import com.github.adriianh.core.domain.player.QueueState
import com.github.adriianh.core.domain.player.RepeatMode

data class PlayerUiState(
    val currentTrack: Track? = null,
    val title: String = "",
    val artist: String = "",
    val albumArt: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val progressFraction: Float = 0f,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val elapsedLabel: String = "0:00",
    val remainingLabel: String = "0:00",
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val accentColor: Color = Color.Transparent,
    val lyrics: String? = null,
    val trackLyrics: TrackLyrics? = null,
    val activeLyricIndex: Int = -1,
    val isLyricsLoading: Boolean = false,
    val showTranslation: Boolean = false,
    val isTranslating: Boolean = false,
    val targetLanguage: String = "es",
    val isFavorite: Boolean = false,
) {
    val hasTrack: Boolean get() = title.isNotEmpty()

    companion object {
        fun from(
            playback: PlaybackState,
            queue: QueueState,
            accentColor: Color = Color.Transparent,
        ): PlayerUiState {
            val track = queue.currentTrack ?: playback.currentTrack
            val durationMs =
                if (playback.durationMs > 0) playback.durationMs else (track?.durationMs ?: 0L)
            val fraction = if (durationMs > 0) {
                (playback.progressMs.toFloat() / durationMs).coerceIn(0f, 1f)
            } else 0f

            val isBuffering = playback.isBuffering ||
                    (track != null && !playback.isPlaying && (playback.currentTrack == null || playback.currentTrack?.id != track.id))

            return PlayerUiState(
                currentTrack = track,
                title = track?.title ?: "",
                artist = track?.artist ?: "",
                albumArt = track?.artworkUrl,
                isPlaying = playback.isPlaying,
                isBuffering = isBuffering,
                progressFraction = fraction,
                progressMs = playback.progressMs,
                durationMs = durationMs,
                elapsedLabel = formatTime(playback.progressMs),
                remainingLabel = formatTime((durationMs - playback.progressMs).coerceAtLeast(0L)),
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