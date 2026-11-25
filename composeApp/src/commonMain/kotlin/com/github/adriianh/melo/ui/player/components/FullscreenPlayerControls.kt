@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.player.AudioOutputDevice
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_collapse
import com.github.adriianh.melo.resources.a11y_favorite
import com.github.adriianh.melo.resources.a11y_lyrics
import com.github.adriianh.melo.resources.a11y_more_options
import com.github.adriianh.melo.resources.a11y_mute
import com.github.adriianh.melo.resources.a11y_panel_queue
import com.github.adriianh.melo.resources.a11y_unmute
import com.github.adriianh.melo.resources.a11y_volume
import com.github.adriianh.melo.ui.components.controls.MeloSlider
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloType
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val ARTWORK_CORNER_RADIUS = 20.dp

@Composable
internal fun FullscreenArtworkCard(
    albumArt: String?,
    title: String,
    isBuffering: Boolean,
    activeAccent: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = ARTWORK_CORNER_RADIUS,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier =
            modifier
                .shadow(28.dp, shape)
                .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        val imageModifier =
            if (onClick != null) {
                Modifier.fillMaxSize().clickable(onClick = onClick)
            } else {
                Modifier.fillMaxSize()
            }
        MeloAsyncImage(
            url = albumArt,
            contentDescription = title,
            modifier = imageModifier,
            shape = shape,
        )
        if (isBuffering) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = activeAccent)
            }
        }
    }
}

@Composable
internal fun FullscreenTrackTitleRow(
    title: String,
    artist: String,
    isFavorite: Boolean,
    activeAccent: Color,
    onArtistClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MeloType.titleLarge.copy(fontSize = 24.sp, lineHeight = 30.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MeloType.titleMedium,
                color = Color.White.copy(alpha = 0.74f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onArtistClick),
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier =
                Modifier
                    .size(48.dp)
                    .semantics { selected = isFavorite },
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = stringResource(Res.string.a11y_favorite),
                tint = if (isFavorite) activeAccent else Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
internal fun CompactTrackHeader(
    modifier: Modifier = Modifier,
    albumArt: String?,
    title: String,
    artist: String,
    isFavorite: Boolean,
    activeAccent: Color,
    onCollapse: () -> Unit,
    onArtistClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoreClick: ((SongMenuAnchor?) -> Unit)? = null,
    showActions: Boolean = true,
    reducedMotion: Boolean = false,
) {
    var moreButtonBounds by remember { mutableStateOf<Rect?>(null) }
    val enterMs = if (reducedMotion) 0 else 240
    val exitMs = if (reducedMotion) 0 else 380

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = showActions,
            enter =
                expandHorizontally(
                    expandFrom = Alignment.Start,
                    animationSpec = tween(enterMs, easing = FastOutSlowInEasing),
                ) + fadeIn(tween(enterMs, easing = FastOutSlowInEasing)),
            exit =
                shrinkHorizontally(
                    shrinkTowards = Alignment.Start,
                    animationSpec = tween(exitMs, easing = FastOutSlowInEasing),
                ) + fadeOut(tween(exitMs, easing = FastOutSlowInEasing)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(Res.string.a11y_collapse),
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        MeloAsyncImage(
            url = albumArt,
            contentDescription = title,
            size = 48.dp,
            shape = RoundedCornerShape(10.dp),
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MeloType.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MeloType.body,
                color = Color.White.copy(alpha = 0.74f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onArtistClick),
            )
        }

        AnimatedVisibility(
            visible = showActions,
            enter =
                expandHorizontally(
                    expandFrom = Alignment.End,
                    animationSpec = tween(enterMs, easing = FastOutSlowInEasing),
                ) + fadeIn(tween(enterMs, easing = FastOutSlowInEasing)),
            exit =
                shrinkHorizontally(
                    shrinkTowards = Alignment.End,
                    animationSpec = tween(exitMs, easing = FastOutSlowInEasing),
                ) + fadeOut(tween(exitMs, easing = FastOutSlowInEasing)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onToggleFavorite,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .semantics { selected = isFavorite },
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = stringResource(Res.string.a11y_favorite),
                        tint = if (isFavorite) activeAccent else Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(
                    onClick = {
                        onMoreClick?.invoke(moreButtonBounds?.let { SongMenuAnchor.Bounds(it) })
                    },
                    enabled = onMoreClick != null,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .onGloballyPositioned { moreButtonBounds = it.boundsInWindow() },
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(Res.string.a11y_more_options),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ExpandedUtilityRow(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    hasLyrics: Boolean,
    isLyricsActive: Boolean,
    isQueueActive: Boolean,
    activeAccent: Color,
    onToggleLyrics: () -> Unit,
    onToggleQueue: () -> Unit,
    modifier: Modifier = Modifier,
    audioOutputs: List<AudioOutputDevice> = emptyList(),
    selectedAudioOutputId: String? = null,
    onRefreshAudioOutputs: () -> Unit = {},
    onSelectAudioOutput: (String?) -> Unit = {},
    onAudioOutputMenuOpenChanged: (Boolean) -> Unit = {},
) {
    val volumeIcon =
        when {
            volume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeOff
            volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
            else -> Icons.AutoMirrored.Filled.VolumeUp
        }
    val volumePercent = "${(volume.coerceIn(0f, 1f) * 100f).roundToInt()}%"

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = onToggleMute,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = volumeIcon,
                contentDescription =
                    stringResource(
                        if (volume <= 0.01f) Res.string.a11y_unmute else Res.string.a11y_mute,
                    ),
                tint = Color.White.copy(alpha = if (volume <= 0.01f) 0.5f else 0.85f),
                modifier = Modifier.size(22.dp),
            )
        }

        MeloSlider(
            value = volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            activeColor = activeAccent,
            inactiveColor = Color.White.copy(alpha = 0.22f),
            thumbColor = Color.White,
            trackHeight = 4.dp,
            hoverTrackHeight = 6.dp,
            thumbRadius = 6.dp,
            hitTargetHeight = 32.dp,
            a11yLabel = stringResource(Res.string.a11y_volume),
            a11yValueDescription = volumePercent,
            modifier = Modifier.weight(1f),
        )

        if (audioOutputs.isNotEmpty()) {
            AudioOutputSelectorButton(
                devices = audioOutputs,
                selectedDeviceId = selectedAudioOutputId,
                activeAccent = activeAccent,
                onRefresh = onRefreshAudioOutputs,
                onSelect = onSelectAudioOutput,
                onMenuOpenChanged = onAudioOutputMenuOpenChanged,
                tint = Color.White.copy(alpha = 0.78f),
                buttonSize = 48.dp,
                iconSize = 22.dp,
            )
        }

        IconButton(
            onClick = onToggleLyrics,
            enabled = hasLyrics,
            modifier =
                Modifier
                    .size(48.dp)
                    .semantics { selected = hasLyrics && isLyricsActive },
        ) {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = stringResource(Res.string.a11y_lyrics),
                tint =
                    when {
                        !hasLyrics -> Color.White.copy(alpha = 0.30f)
                        isLyricsActive -> activeAccent
                        else -> Color.White.copy(alpha = 0.78f)
                    },
                modifier = Modifier.size(22.dp),
            )
        }

        IconButton(
            onClick = onToggleQueue,
            modifier =
                Modifier
                    .size(48.dp)
                    .semantics { selected = isQueueActive },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = stringResource(Res.string.a11y_panel_queue),
                tint = if (isQueueActive) activeAccent else Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
internal fun CompactUtilityRow(
    hasLyrics: Boolean,
    isLyricsActive: Boolean,
    isQueueActive: Boolean,
    showTranslation: Boolean,
    isTranslating: Boolean,
    targetLanguage: String,
    activeAccent: Color,
    onToggleLyrics: () -> Unit,
    onToggleTranslation: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onToggleQueue: () -> Unit,
    modifier: Modifier = Modifier,
    audioOutputs: List<AudioOutputDevice> = emptyList(),
    selectedAudioOutputId: String? = null,
    onRefreshAudioOutputs: () -> Unit = {},
    onSelectAudioOutput: (String?) -> Unit = {},
) {
    var showLanguageMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onToggleLyrics,
            enabled = hasLyrics,
            modifier =
                Modifier
                    .size(48.dp)
                    .semantics { selected = hasLyrics && isLyricsActive },
        ) {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = stringResource(Res.string.a11y_lyrics),
                tint =
                    when {
                        !hasLyrics -> Color.White.copy(alpha = 0.30f)
                        isLyricsActive -> activeAccent
                        else -> Color.White.copy(alpha = 0.78f)
                    },
                modifier = Modifier.size(22.dp),
            )
        }

        if (hasLyrics) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LyricsTranslationChip(
                    showLanguageMenu = showLanguageMenu,
                    onShowLanguageMenuChange = { showLanguageMenu = it },
                    showTranslation = showTranslation,
                    isTranslating = isTranslating,
                    targetLanguage = targetLanguage,
                    activeAccent = activeAccent,
                    onToggleTranslation = onToggleTranslation,
                    onSelectLanguage = onSelectLanguage,
                    useAccessibleTouchTarget = true,
                    inactiveContentColor = Color.White.copy(alpha = 0.85f),
                    inactiveFillColor = Color.White.copy(alpha = 0.12f),
                    inactiveBorderColor = Color.White.copy(alpha = 0.16f),
                )
                LyricsLanguagePickerButton(
                    onClick = { showLanguageMenu = true },
                    tint = Color.White.copy(alpha = 0.78f),
                    useAccessibleTouchTarget = true,
                )
            }
        }

        if (audioOutputs.size > 1) {
            AudioOutputSelectorButton(
                devices = audioOutputs,
                selectedDeviceId = selectedAudioOutputId,
                activeAccent = activeAccent,
                onRefresh = onRefreshAudioOutputs,
                onSelect = onSelectAudioOutput,
                tint = Color.White.copy(alpha = 0.78f),
                buttonSize = 48.dp,
                iconSize = 22.dp,
            )
        }

        IconButton(
            onClick = onToggleQueue,
            modifier =
                Modifier
                    .size(48.dp)
                    .semantics { selected = isQueueActive },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = stringResource(Res.string.a11y_panel_queue),
                tint = if (isQueueActive) activeAccent else Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
