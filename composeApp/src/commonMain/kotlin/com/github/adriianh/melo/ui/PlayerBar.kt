package com.github.adriianh.melo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.player.RepeatMode
import com.github.adriianh.melo.ui.components.AnimatedEqualizerBars
import com.github.adriianh.melo.ui.components.MeloSlider
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

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
    val activeAccent by rememberAnimatedPlayerAccent(state)

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
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    PlaybackTrackInfo(
                        state = state,
                        activeAccent = activeAccent,
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleDockedPane)
                            .padding(4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = viewModel::toggleShuffle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (state.shuffleEnabled) activeAccent else MeloColors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = viewModel::playPrevious,
                        enabled = state.hasPrevious,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (state.hasPrevious) MeloColors.textPrimary else MeloColors.textMuted.copy(
                                alpha = 0.4f
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    FilledIconButton(
                        onClick = viewModel::togglePlayPause,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = activeAccent
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        if (state.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = viewModel::playNext,
                        enabled = state.hasNext,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (state.hasNext) MeloColors.textPrimary else MeloColors.textMuted.copy(
                                alpha = 0.4f
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = viewModel::toggleRepeat,
                        modifier = Modifier.size(32.dp)
                    ) {
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

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    val volume by viewModel.volume.collectAsState()
                    val volumeIcon = when {
                        volume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeOff
                        volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    }

                    var isVolumeHovered by remember { mutableStateOf(false) }
                    var isVolumeSliderActive by remember { mutableStateOf(false) }
                    var collapseJob by remember { mutableStateOf<Job?>(null) }
                    val scope = rememberCoroutineScope()
                    val isVolumeExpanded = isVolumeHovered || isVolumeSliderActive

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Initial)
                                            when (event.type) {
                                                PointerEventType.Enter, PointerEventType.Move -> {
                                                    collapseJob?.cancel()
                                                    isVolumeHovered = true
                                                }

                                                PointerEventType.Exit -> {
                                                    collapseJob?.cancel()
                                                    collapseJob = scope.launch {
                                                        delay(200.milliseconds)
                                                        isVolumeHovered = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = viewModel::toggleMute,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = volumeIcon,
                                    contentDescription = if (volume <= 0.01f) "Activar sonido" else "Silenciar",
                                    tint = if (volume <= 0.01f) MeloColors.textMuted else MeloColors.textPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = isVolumeExpanded,
                                enter = fadeIn(tween(150)) + expandHorizontally(
                                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                                    expandFrom = Alignment.Start
                                ),
                                exit = fadeOut(tween(150)) + shrinkHorizontally(
                                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                                    shrinkTowards = Alignment.Start
                                )
                            ) {
                                MeloSlider(
                                    value = volume,
                                    onValueChange = {
                                        isVolumeSliderActive = true
                                        viewModel.setVolume(it)
                                    },
                                    onValueChangeFinished = {
                                        isVolumeSliderActive = false
                                    },
                                    valueRange = 0f..1f,
                                    activeColor = activeAccent,
                                    inactiveColor = MeloColors.borderStrong,
                                    modifier = Modifier
                                        .width(82.dp)
                                        .padding(start = 2.dp, end = 2.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleLyrics,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Mic,
                                contentDescription = "Letras en panel",
                                tint = MeloColors.textMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleQueue,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Cola en panel",
                                tint = MeloColors.textMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenNowPlaying,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInFull,
                                contentDescription = "Expandir pantalla completa",
                                tint = MeloColors.textMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
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
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val activeAccent by rememberAnimatedPlayerAccent(state)

    if (!state.hasTrack) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
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
                PlaybackTrackInfo(
                    state = state,
                    activeAccent = activeAccent,
                    artworkSize = 40.dp,
                    artworkShape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = viewModel::togglePlayPause) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = activeAccent,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = activeAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
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

@Composable
private fun rememberAnimatedPlayerAccent(
    state: PlayerUiState,
): State<Color> {
    val targetAccent =
        if (state.accentColor != MeloColors.textMuted) state.accentColor else MaterialTheme.colorScheme.primary
    return animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "PlayerBarAccent"
    )
}

@Composable
private fun PlaybackTrackInfo(
    state: PlayerUiState,
    activeAccent: Color,
    modifier: Modifier = Modifier,
    artworkSize: Dp = 52.dp,
    artworkShape: Shape = RoundedCornerShape(8.dp),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MeloAsyncImage(
            url = state.albumArt,
            contentDescription = state.title,
            size = artworkSize,
            shape = artworkShape
        )
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
}