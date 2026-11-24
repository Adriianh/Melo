@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_next
import com.github.adriianh.melo.resources.a11y_pause
import com.github.adriianh.melo.resources.a11y_play
import com.github.adriianh.melo.resources.a11y_previous
import com.github.adriianh.melo.resources.a11y_repeat
import com.github.adriianh.melo.resources.a11y_shuffle
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.PlayerUiState
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlayerTransportControls(
    state: PlayerUiState,
    activeAccent: Color,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
) {
    PlayerTransportControls(
        isPlaying = state.isPlaying,
        isBuffering = state.isBuffering,
        shuffleEnabled = state.shuffleEnabled,
        repeatMode = state.repeatMode,
        hasPrevious = state.hasPrevious,
        hasNext = state.hasNext,
        activeAccent = activeAccent,
        onToggleShuffle = viewModel::toggleShuffle,
        onPlayPrevious = viewModel::playPrevious,
        onTogglePlayPause = viewModel::togglePlayPause,
        onPlayNext = viewModel::playNext,
        onToggleRepeat = viewModel::toggleRepeat,
        modifier = modifier,
    )
}

@Composable
fun PlayerTransportControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    hasPrevious: Boolean,
    hasNext: Boolean,
    activeAccent: Color,
    onToggleShuffle: () -> Unit,
    onPlayPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    playButtonSize: Dp = 68.dp,
    primaryIconTint: Color = Color.White,
    mutedIconTint: Color = MeloColors.textMuted,
) {
    val repeatActive = repeatMode != RepeatMode.NONE
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onToggleShuffle,
            modifier =
                Modifier
                    .size(48.dp)
                    .semantics { selected = shuffleEnabled },
        ) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = stringResource(Res.string.a11y_shuffle),
                tint = if (shuffleEnabled) activeAccent else mutedIconTint,
                modifier = Modifier.size(24.dp),
            )
        }

        IconButton(
            onClick = onPlayPrevious,
            enabled = hasPrevious,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = stringResource(Res.string.a11y_previous),
                tint = if (hasPrevious) primaryIconTint else mutedIconTint.copy(alpha = 0.4f),
                modifier = Modifier.size(34.dp),
            )
        }

        Surface(
            shape = CircleShape,
            color = activeAccent,
            modifier =
                Modifier
                    .size(playButtonSize)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onTogglePlayPause),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = Color.White,
                        strokeWidth = 3.dp,
                    )
                } else {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription =
                            stringResource(
                                if (isPlaying) Res.string.a11y_pause else Res.string.a11y_play,
                            ),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }

        IconButton(
            onClick = onPlayNext,
            enabled = hasNext,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = stringResource(Res.string.a11y_next),
                tint = if (hasNext) primaryIconTint else mutedIconTint.copy(alpha = 0.4f),
                modifier = Modifier.size(34.dp),
            )
        }

        IconButton(
            onClick = onToggleRepeat,
            modifier =
                Modifier
                    .size(48.dp)
                    .semantics { selected = repeatActive },
        ) {
            val icon =
                when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
            Icon(
                icon,
                contentDescription = stringResource(Res.string.a11y_repeat),
                tint = if (repeatActive) activeAccent else mutedIconTint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
