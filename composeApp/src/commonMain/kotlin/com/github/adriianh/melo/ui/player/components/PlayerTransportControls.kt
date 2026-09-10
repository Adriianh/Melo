package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.PlayerUiState

@Composable
fun PlayerTransportControls(
    state: PlayerUiState,
    activeAccent: Color,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = viewModel::toggleShuffle) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (state.shuffleEnabled) activeAccent else MeloColors.textMuted,
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(onClick = viewModel::playPrevious, enabled = state.hasPrevious) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = if (state.hasPrevious) MeloColors.textPrimary else MeloColors.textMuted.copy(
                    alpha = 0.4f
                ),
                modifier = Modifier.size(36.dp)
            )
        }

        Surface(
            shape = CircleShape,
            color = activeAccent,
            modifier = Modifier.size(64.dp).clickable(onClick = viewModel::togglePlayPause)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (state.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = Color.White,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        IconButton(onClick = viewModel::playNext, enabled = state.hasNext) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = if (state.hasNext) MeloColors.textPrimary else MeloColors.textMuted.copy(
                    alpha = 0.4f
                ),
                modifier = Modifier.size(36.dp)
            )
        }

        IconButton(onClick = viewModel::toggleRepeat) {
            val icon = when (state.repeatMode) {
                RepeatMode.ONE -> Icons.Default.RepeatOne
                else -> Icons.Default.Repeat
            }
            val tint = when (state.repeatMode) {
                RepeatMode.NONE -> MeloColors.textMuted
                else -> activeAccent
            }
            Icon(icon, contentDescription = "Repeat", tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}