package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.SyncedLine
import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun LiveLyricsPreviewCard(
    trackLyrics: TrackLyrics?,
    activeLyricIndex: Int,
    showTranslation: Boolean,
    activeAccent: Color,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLine =
        if (trackLyrics != null && trackLyrics.hasSync && activeLyricIndex in trackLyrics.syncedLyrics.indices) {
            trackLyrics.syncedLyrics[activeLyricIndex]
        } else null

    val nextLine =
        if (trackLyrics != null && trackLyrics.hasSync && activeLyricIndex + 1 in trackLyrics.syncedLyrics.indices) {
            trackLyrics.syncedLyrics[activeLyricIndex + 1]
        } else null

    val cardShape = RoundedCornerShape(18.dp)
    val glassBg = Brush.verticalGradient(
        colors = listOf(
            activeAccent.copy(alpha = 0.18f),
            MeloColors.glassFill.copy(alpha = 0.50f)
        )
    )
    val glassBorder = Brush.horizontalGradient(
        colors = listOf(
            activeAccent.copy(alpha = 0.40f),
            MeloColors.glassBorder.copy(alpha = 0.25f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(122.dp)
            .clip(cardShape)
            .background(glassBg)
            .border(width = 1.dp, brush = glassBorder, shape = cardShape)
            .clickable { onExpand() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LyricsPreviewHeader(activeAccent)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (trackLyrics != null && trackLyrics.hasSync) {
                    AnimatedContent(
                        targetState = activeLyricIndex to currentLine,
                        transitionSpec = {
                            (slideInVertically(
                                animationSpec = tween(280, easing = FastOutSlowInEasing),
                                initialOffsetY = { it / 2 }
                            ) + fadeIn(tween(280))).togetherWith(
                                slideOutVertically(
                                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                                    targetOffsetY = { -it / 2 }
                                ) + fadeOut(tween(200))
                            )
                        },
                        label = "LiveLyricsTransition"
                    ) { (_, line) ->
                        LyricsPreviewLines(
                            line = line,
                            nextLine = nextLine,
                            showTranslation = showTranslation,
                            activeAccent = activeAccent
                        )
                    }
                } else if (trackLyrics != null && !trackLyrics.plainLyrics.isNullOrBlank()) {
                    val plain = trackLyrics.plainLyrics ?: ""
                    val previewSnippet =
                        plain.lines().filter { it.isNotBlank() }.take(2).joinToString("\n")
                    Text(
                        text = previewSnippet,
                        style = MeloType.body.copy(
                            fontSize = 13.5.sp,
                            lineHeight = 18.sp
                        ),
                        color = MeloColors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    LyricsUnavailableMessage(
                        error = trackLyrics?.error,
                        activeAccent = activeAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricsPreviewHeader(activeAccent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = activeAccent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "LETRA",
                style = MeloType.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = activeAccent
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "EXPANDIR",
                style = MeloType.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = MeloColors.textSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "Expandir letra",
                tint = MeloColors.textSecondary,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun LyricsPreviewLines(
    line: SyncedLine?,
    nextLine: SyncedLine?,
    showTranslation: Boolean,
    activeAccent: Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        if (line != null && line.text.isNotBlank()) {
            Text(
                text = line.text,
                style = MeloType.titleMedium.copy(
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 21.sp
                ),
                color = MeloColors.textPrimary,
                maxLines = if (showTranslation && !line.translation.isNullOrBlank()) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )

            val translation = line.translation
            if (showTranslation && !translation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = translation,
                    style = MeloType.body.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    ),
                    color = activeAccent.copy(alpha = 0.90f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (nextLine != null && (!showTranslation || translation.isNullOrBlank())) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = nextLine.text.ifBlank { "♪ ♫ ♪" },
                    style = MeloType.body.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 17.sp
                    ),
                    color = MeloColors.textMuted.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            DancingMelodyIndicator(
                isActive = true,
                tint = activeAccent,
                baseFontSize = 17.sp
            )
        }
    }
}

@Composable
private fun LyricsUnavailableMessage(
    error: String?,
    activeAccent: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = activeAccent.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = error ?: "Letra no disponible para esta canción",
            style = MeloType.body.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MeloColors.textMuted
        )
    }
}