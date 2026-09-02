package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.SyncedLine
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
internal fun SyncedLyricsList(
    lines: List<SyncedLine>,
    activeIndex: Int,
    showTranslation: Boolean,
    activeAccent: Color,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < lines.size) {
            val targetIndex = (activeIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(
                index = targetIndex,
                scrollOffset = 0
            )
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 20.dp, bottom = 80.dp, start = 18.dp, end = 18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        itemsIndexed(lines) { index, line ->
            val isActive = index == activeIndex
            val alpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.45f,
                animationSpec = spring(stiffness = 320f),
                label = "LyricAlphaAnim"
            )
            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.02f else 1.0f,
                animationSpec = spring(stiffness = 320f),
                label = "LyricScaleAnim"
            )
            val textColor by animateColorAsState(
                targetValue = if (isActive) activeAccent else MeloColors.textPrimary,
                animationSpec = tween(220),
                label = "LyricColorAnim"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSeekTo(line.timeMs) }
                    )
                    .padding(vertical = 4.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if (line.text.isNotBlank()) {
                    Text(
                        text = line.text,
                        style = MeloType.body.copy(
                            fontSize = if (isActive) 18.sp else 15.5.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            lineHeight = if (isActive) 24.sp else 21.sp
                        ),
                        color = textColor
                    )
                } else {
                    DancingMelodyIndicator(
                        isActive = isActive,
                        tint = textColor,
                        baseFontSize = if (isActive) 18.sp else 15.5.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                val translation = line.translation
                if (showTranslation && !translation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = translation,
                        style = MeloType.body.copy(
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        ),
                        color = if (isActive) activeAccent.copy(alpha = 0.88f) else MeloColors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlainLyricsView(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = text,
            style = MeloType.body.copy(
                fontSize = 14.5.sp,
                lineHeight = 22.sp
            ),
            color = MeloColors.textPrimary
        )
    }
}