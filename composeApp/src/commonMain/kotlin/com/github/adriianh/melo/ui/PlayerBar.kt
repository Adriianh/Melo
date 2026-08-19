package com.github.adriianh.melo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.melo.ui.components.AnimatedEqualizerBars
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DesktopPlayerBar(
    onOpenNowPlaying: () -> Unit = {},
    onToggleLyrics: () -> Unit = {},
    onToggleQueue: () -> Unit = {},
    onToggleDockedPane: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val targetAccent =
        if (state.accentColor != MeloColors.textMuted) state.accentColor else MaterialTheme.colorScheme.primary
    val activeAccent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "DesktopPlayerBarAccent"
    )

    if (!state.hasTrack) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .shadow(8.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(MeloColors.playerBarFill)
                .border(0.5.dp, MeloColors.playerBarBorder, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .width(260.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleDockedPane)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        MeloAsyncImage(
                            url = state.albumArt,
                            contentDescription = state.title,
                            size = 52.dp,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                state.title,
                                style = MeloType.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MeloColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.isPlaying) {
                                AnimatedEqualizerBars(accentColor = activeAccent)
                            }
                        }
                        Text(
                            state.artist,
                            style = MeloType.labelSmall,
                            color = MeloColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = viewModel::toggleShuffle) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (state.shuffleEnabled) activeAccent else MeloColors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = viewModel::playPrevious, enabled = state.hasPrevious) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = if (state.hasPrevious) MeloColors.textPrimary else MeloColors.textMuted.copy(
                                    alpha = 0.4f
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        FilledIconButton(
                            onClick = viewModel::togglePlayPause,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = activeAccent
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        IconButton(onClick = viewModel::playNext, enabled = state.hasNext) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = if (state.hasNext) MeloColors.textPrimary else MeloColors.textMuted.copy(
                                    alpha = 0.4f
                                ),
                                modifier = Modifier.size(24.dp)
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
                            Icon(
                                icon,
                                contentDescription = "Repeat",
                                tint = tint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.width(220.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleLyrics) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = "Letras en panel",
                            tint = MeloColors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onToggleQueue) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Cola en panel",
                            tint = MeloColors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onOpenNowPlaying) {
                        Icon(
                            Icons.Default.OpenInFull,
                            contentDescription = "Expandir pantalla completa",
                            tint = MeloColors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        HoverableProgressBar(
            progressFraction = state.progressFraction,
            accentColor = activeAccent,
            elapsedLabel = state.elapsedLabel,
            onSeek = { fraction ->
                if (state.durationMs > 0) {
                    viewModel.seekTo((fraction * state.durationMs).toLong())
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            lineCenterY = 10.5.dp,
            containerHeight = 21.dp
        )
    }
}

@Composable
internal fun MobilePlayerBar(
    onOpenNowPlaying: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val targetAccent =
        if (state.accentColor != MeloColors.textMuted) state.accentColor else MaterialTheme.colorScheme.primary
    val activeAccent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "MobilePlayerBarAccent"
    )

    if (!state.hasTrack) return

    Box(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(horizontal = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .shadow(6.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(MeloColors.playerBarFill)
                .border(0.5.dp, MeloColors.playerBarBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenNowPlaying)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MeloAsyncImage(
                    url = state.albumArt,
                    contentDescription = state.title,
                    size = 40.dp,
                    shape = RoundedCornerShape(6.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.title,
                            style = MeloType.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MeloColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.isPlaying) {
                            AnimatedEqualizerBars(accentColor = activeAccent)
                        }
                    }
                    Text(
                        state.artist,
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = viewModel::togglePlayPause) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = activeAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = viewModel::playNext, enabled = state.hasNext) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = if (state.hasNext) MeloColors.textPrimary else MeloColors.textMuted.copy(
                            alpha = 0.4f
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        HoverableProgressBar(
            progressFraction = state.progressFraction,
            accentColor = activeAccent,
            elapsedLabel = state.elapsedLabel,
            onSeek = { fraction ->
                if (state.durationMs > 0) {
                    viewModel.seekTo((fraction * state.durationMs).toLong())
                }
            },
            cornerRadius = 12.dp,
            lineCenterY = 12.5.dp,
            containerHeight = 25.dp,
            showHoverControls = false,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )
    }
}