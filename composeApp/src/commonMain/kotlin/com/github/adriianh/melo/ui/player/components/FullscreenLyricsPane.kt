@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_current_lyric_line
import com.github.adriianh.melo.resources.a11y_instrumental_break
import com.github.adriianh.melo.resources.player_lyrics_loading
import com.github.adriianh.melo.util.LocalWindowActivity
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.rememberSharedAnimationTime
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private const val ANCHOR_VIEWPORT_FRACTION = 0.35f
private const val USER_SCROLL_RESUME_DELAY_MS = 3_500L
private const val DOT_CYCLE_SECONDS = 1.25f
private const val DOT_TWO_PI = (2.0 * PI).toFloat()

internal data class FullscreenLyricsStyle(
    val activeFontSize: TextUnit,
    val activeLineHeight: TextUnit,
    val neighborFontSize: TextUnit,
    val neighborLineHeight: TextUnit,
    val plainFontSize: TextUnit,
    val plainLineHeight: TextUnit,
    val translationFontSize: TextUnit,
    val translationLineHeight: TextUnit,
    val itemSpacing: Dp,
    val horizontalPadding: Dp,
) {
    companion object {
        val Expanded =
            FullscreenLyricsStyle(
                activeFontSize = 42.sp,
                activeLineHeight = 52.sp,
                neighborFontSize = 32.sp,
                neighborLineHeight = 42.sp,
                plainFontSize = 24.sp,
                plainLineHeight = 36.sp,
                translationFontSize = 18.sp,
                translationLineHeight = 25.sp,
                itemSpacing = 20.dp,
                horizontalPadding = 16.dp,
            )

        val Compact =
            FullscreenLyricsStyle(
                activeFontSize = 30.sp,
                activeLineHeight = 38.sp,
                neighborFontSize = 23.sp,
                neighborLineHeight = 31.sp,
                plainFontSize = 19.sp,
                plainLineHeight = 29.sp,
                translationFontSize = 15.sp,
                translationLineHeight = 21.sp,
                itemSpacing = 16.dp,
                horizontalPadding = 8.dp,
            )
    }
}

@Composable
internal fun FullscreenLyricsPane(
    trackLyrics: TrackLyrics?,
    positionMs: Long,
    isLoading: Boolean,
    showTranslation: Boolean,
    isTranslating: Boolean,
    targetLanguage: String,
    activeAccent: Color,
    onSeekTo: (Long) -> Unit,
    onToggleTranslation: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: FullscreenLyricsStyle = FullscreenLyricsStyle.Expanded,
    showControlsOnHoverOnly: Boolean = false,
    reducedMotion: Boolean = false,
    showFloatingControls: Boolean = true,
) {
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    var hasFocus by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val hasLyrics = trackLyrics.hasDisplayableLyrics()
    val controlsRevealed = !showControlsOnHoverOnly || isHovered || hasFocus || showLanguageMenu
    val controlsVisible = showFloatingControls && hasLyrics && controlsRevealed
    val controlsAlpha by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 180),
        label = "FullscreenLyricsControlsAlpha",
    )

    Box(
        modifier =
            modifier
                .hoverable(hoverSource)
                .onFocusChanged { hasFocus = it.hasFocus },
    ) {
        FullscreenLyricsBody(
            trackLyrics = trackLyrics,
            positionMs = positionMs,
            isLoading = isLoading,
            showTranslation = showTranslation,
            activeAccent = activeAccent,
            onSeekTo = onSeekTo,
            style = style,
            reducedMotion = reducedMotion,
        )

        if (showFloatingControls && hasLyrics && controlsAlpha > 0.01f) {
            FullscreenLyricsFloatingControls(
                showLanguageMenu = showLanguageMenu,
                onShowLanguageMenuChange = { showLanguageMenu = it },
                showTranslation = showTranslation,
                isTranslating = isTranslating,
                targetLanguage = targetLanguage,
                activeAccent = activeAccent,
                onToggleTranslation = onToggleTranslation,
                onSelectLanguage = onSelectLanguage,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = style.horizontalPadding)
                        .alpha(controlsAlpha)
                        .focusGroup(),
            )
        }
    }
}

@Composable
private fun FullscreenLyricsBody(
    trackLyrics: TrackLyrics?,
    positionMs: Long,
    isLoading: Boolean,
    showTranslation: Boolean,
    activeAccent: Color,
    onSeekTo: (Long) -> Unit,
    style: FullscreenLyricsStyle,
    reducedMotion: Boolean,
) {
    when {
        isLoading -> FullscreenLyricsLoading(activeAccent = activeAccent)

        trackLyrics != null && trackLyrics.hasSync -> {
            FullscreenSyncedLyricsList(
                trackLyrics = trackLyrics,
                positionMs = positionMs,
                showTranslation = showTranslation,
                onSeekTo = onSeekTo,
                style = style,
                reducedMotion = reducedMotion,
                modifier = Modifier.fillMaxSize(),
            )
        }

        trackLyrics != null && !trackLyrics.plainLyrics.isNullOrBlank() -> {
            FullscreenPlainLyricsView(
                text = trackLyrics.plainLyrics.orEmpty(),
                style = style,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun FullscreenLyricsLoading(activeAccent: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = activeAccent,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.player_lyrics_loading),
                style = MeloType.body,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun FullscreenLyricsFloatingControls(
    showLanguageMenu: Boolean,
    onShowLanguageMenuChange: (Boolean) -> Unit,
    showTranslation: Boolean,
    isTranslating: Boolean,
    targetLanguage: String,
    activeAccent: Color,
    onToggleTranslation: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LyricsSourceBadge()

        LyricsTranslationChip(
            showLanguageMenu = showLanguageMenu,
            onShowLanguageMenuChange = onShowLanguageMenuChange,
            showTranslation = showTranslation,
            isTranslating = isTranslating,
            targetLanguage = targetLanguage,
            activeAccent = activeAccent,
            onToggleTranslation = onToggleTranslation,
            onSelectLanguage = onSelectLanguage,
            useAccessibleTouchTarget = true,
            inactiveContentColor = Color.White.copy(alpha = 0.85f),
            inactiveFillColor = Color.Black.copy(alpha = 0.36f),
            inactiveBorderColor = Color.White.copy(alpha = 0.14f),
        )

        LyricsLanguagePickerButton(
            onClick = { onShowLanguageMenuChange(true) },
            tint = Color.White.copy(alpha = 0.85f),
            useAccessibleTouchTarget = true,
        )
    }
}

@Composable
private fun FullscreenSyncedLyricsList(
    trackLyrics: TrackLyrics,
    positionMs: Long,
    showTranslation: Boolean,
    onSeekTo: (Long) -> Unit,
    style: FullscreenLyricsStyle,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val entries = remember(trackLyrics.syncedLyrics) { buildFullscreenLyricEntries(trackLyrics.syncedLyrics) }
    val activeEntryIndex by remember(entries, positionMs) {
        derivedStateOf { resolveActiveFullscreenEntryIndex(entries, positionMs) }
    }

    val listState = rememberLazyListState()
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    var userScrolledRecently by remember { mutableStateOf(false) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userScrolledRecently = true
        } else if (userScrolledRecently) {
            delay(USER_SCROLL_RESUME_DELAY_MS.milliseconds)
            userScrolledRecently = false
        }
    }

    LaunchedEffect(activeEntryIndex, viewportHeightPx, reducedMotion, userScrolledRecently) {
        val canScrollToActive = activeEntryIndex in entries.indices && viewportHeightPx > 0
        if (canScrollToActive && !listState.isScrollInProgress && !userScrolledRecently) {
            val anchorOffset = -(viewportHeightPx * ANCHOR_VIEWPORT_FRACTION).toInt()
            val firstVisible = listState.firstVisibleItemIndex
            if (reducedMotion || abs(activeEntryIndex - firstVisible) > 8) {
                listState.scrollToItem(index = activeEntryIndex, scrollOffset = anchorOffset)
            } else {
                listState.animateScrollToItem(index = activeEntryIndex, scrollOffset = anchorOffset)
            }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding =
            PaddingValues(
                top = 72.dp,
                bottom = 220.dp,
                start = style.horizontalPadding,
                end = style.horizontalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(style.itemSpacing),
        modifier =
            modifier
                .onSizeChanged { viewportHeightPx = it.height }
                .verticalEdgeFadeMask(),
    ) {
        itemsIndexed(
            items = entries,
            key = { _, entry -> entry.key },
        ) { index, entry ->
            val distance =
                if (activeEntryIndex < 0) {
                    index + 1
                } else {
                    abs(index - activeEntryIndex)
                }
            val isActive = index == activeEntryIndex

            FullscreenLyricEntryRow(
                entry = entry,
                isActive = isActive,
                distance = distance,
                showTranslation = showTranslation,
                style = style,
                reducedMotion = reducedMotion,
                onSeekTo = {
                    userScrolledRecently = false
                    onSeekTo(entry.timeMs)
                },
            )
        }
    }
}

@Composable
private fun FullscreenLyricEntryRow(
    entry: FullscreenLyricEntry,
    isActive: Boolean,
    distance: Int,
    showTranslation: Boolean,
    style: FullscreenLyricsStyle,
    reducedMotion: Boolean,
    onSeekTo: () -> Unit,
) {
    val targetAlpha = lyricAlphaForDistance(distance)
    val alpha =
        if (reducedMotion) {
            targetAlpha
        } else {
            animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = tween(durationMillis = 260),
                label = "FullscreenLyricAlpha",
            ).value
        }

    val blurRadius = if (reducedMotion) 0.dp else lyricBlurForDistance(distance)
    val currentLineLabel = stringResource(Res.string.a11y_current_lyric_line)

    val blurModifier =
        if (blurRadius > 0.dp) {
            Modifier.blur(blurRadius)
        } else {
            Modifier
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    if (isActive) {
                        stateDescription = currentLineLabel
                    }
                }.clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onSeekTo,
                ).then(blurModifier)
                .alpha(alpha)
                .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        when (entry) {
            is FullscreenLyricEntry.LyricLine -> {
                Text(
                    text = entry.line.text,
                    style =
                        MeloType.body.copy(
                            fontSize = if (isActive) style.activeFontSize else style.neighborFontSize,
                            lineHeight = if (isActive) style.activeLineHeight else style.neighborLineHeight,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                    color = Color.White,
                )

                val translation = entry.line.translation
                if (showTranslation && !translation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = translation,
                        style =
                            MeloType.body.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = style.translationFontSize,
                                lineHeight = style.translationLineHeight,
                                fontWeight = FontWeight.Normal,
                            ),
                        color = Color.White.copy(alpha = if (isActive) 0.84f else 0.72f),
                    )
                }
            }

            is FullscreenLyricEntry.InstrumentalGap -> {
                InstrumentalDotsIndicator(
                    isActive = isActive,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
internal fun InstrumentalDotsIndicator(
    isActive: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val a11yLabel = stringResource(Res.string.a11y_instrumental_break)
    val shouldAnimate = isActive && !reducedMotion && LocalWindowActivity.current.isVisible
    val elapsed = if (shouldAnimate) rememberSharedAnimationTime(enabled = true) else null

    Row(
        modifier = modifier.semantics { contentDescription = a11yLabel },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (dotIndex in 0 until 3) {
            val dotModifier =
                if (elapsed != null) {
                    Modifier
                        .size(12.dp)
                        .graphicsLayer {
                            val phase = (elapsed.floatValue - dotIndex * 0.18f).coerceAtLeast(0f)
                            val wave = 0.5f + 0.5f * sin(phase * (DOT_TWO_PI / DOT_CYCLE_SECONDS))
                            val s = 0.78f + 0.36f * wave
                            scaleX = s
                            scaleY = s
                            alpha = 0.45f + 0.55f * wave
                        }.clip(CircleShape)
                        .background(Color.White)
                } else {
                    Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (isActive) 0.9f else 0.55f))
                }
            Box(modifier = dotModifier)
        }
    }
}

@Composable
private fun FullscreenPlainLyricsView(
    text: String,
    style: FullscreenLyricsStyle,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .verticalEdgeFadeMask()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = 56.dp,
                    bottom = 120.dp,
                    start = style.horizontalPadding,
                    end = style.horizontalPadding,
                ),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            text = text,
            style =
                MeloType.body.copy(
                    fontSize = style.plainFontSize,
                    lineHeight = style.plainLineHeight,
                    fontWeight = FontWeight.Medium,
                ),
            color = Color.White.copy(alpha = 0.92f),
        )
    }
}

private fun Modifier.verticalEdgeFadeMask(): Modifier =
    this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }.drawWithContent {
            drawContent()
            drawRect(
                brush =
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.10f to Color.Black,
                        0.84f to Color.Black,
                        1.0f to Color.Transparent,
                    ),
                blendMode = BlendMode.DstIn,
            )
        }
