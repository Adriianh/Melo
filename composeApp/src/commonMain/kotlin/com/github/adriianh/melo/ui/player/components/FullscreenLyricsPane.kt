@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.LyricsActiveLineColor
import com.github.adriianh.core.domain.model.LyricsAlignment
import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_current_lyric_line
import com.github.adriianh.melo.resources.a11y_instrumental_break
import com.github.adriianh.melo.resources.player_lyrics_loading
import com.github.adriianh.melo.util.LocalWindowActivity
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerProgressState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

private const val USER_SCROLL_RESUME_DELAY_MS = 3_500L

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
    progressFlow: StateFlow<PlayerProgressState>? = null,
    style: FullscreenLyricsStyle = FullscreenLyricsStyle.Expanded,
    lyricsConfig: NowPlayingLyricsConfig = NowPlayingLyricsConfig(),
    showControlsOnHoverOnly: Boolean = false,
    isIdleHidden: Boolean = false,
    reducedMotion: Boolean = false,
    showFloatingControls: Boolean = true,
) {
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    var hasFocus by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    val resolvedStyle = remember(style, lyricsConfig) { resolveFullscreenLyricsStyle(style, lyricsConfig) }

    val hasLyrics = trackLyrics.hasDisplayableLyrics()
    val controlsVisible =
        shouldShowLyricsFloatingControls(
            showFloatingControls = showFloatingControls,
            hasLyrics = hasLyrics,
            showControlsOnHoverOnly = showControlsOnHoverOnly,
            isHovered = isHovered,
            hasFocus = hasFocus,
            showLanguageMenu = showLanguageMenu,
            isIdleHidden = isIdleHidden,
        )
    val controlsAlpha by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        animationSpec =
            tween(
                durationMillis =
                    when {
                        reducedMotion -> 0
                        controlsVisible -> 220
                        else -> 650
                    },
                easing = FastOutSlowInEasing,
            ),
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
            progressFlow = progressFlow,
            isLoading = isLoading,
            showTranslation = showTranslation,
            activeAccent = activeAccent,
            onSeekTo = onSeekTo,
            style = resolvedStyle,
            lyricsConfig = lyricsConfig,
            reducedMotion = reducedMotion,
        )

        if (showFloatingControls && hasLyrics && controlsAlpha > 0.01f) {
            FullscreenLyricsFloatingControls(
                showSourceBadge = lyricsConfig.showSourceBadge,
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
    progressFlow: StateFlow<PlayerProgressState>?,
    isLoading: Boolean,
    showTranslation: Boolean,
    activeAccent: Color,
    onSeekTo: (Long) -> Unit,
    style: FullscreenLyricsStyle,
    lyricsConfig: NowPlayingLyricsConfig,
    reducedMotion: Boolean,
) {
    when {
        isLoading -> FullscreenLyricsLoading(activeAccent = activeAccent)

        trackLyrics != null && trackLyrics.hasSync -> {
            FullscreenSyncedLyricsList(
                trackLyrics = trackLyrics,
                positionMs = positionMs,
                progressFlow = progressFlow,
                showTranslation = showTranslation,
                activeAccent = activeAccent,
                onSeekTo = onSeekTo,
                style = style,
                lyricsConfig = lyricsConfig,
                reducedMotion = reducedMotion,
                modifier = Modifier.fillMaxSize(),
            )
        }

        trackLyrics != null && !trackLyrics.plainLyrics.isNullOrBlank() -> {
            FullscreenPlainLyricsView(
                text = trackLyrics.plainLyrics.orEmpty(),
                style = style,
                alignment = lyricsConfig.alignment,
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
    showSourceBadge: Boolean,
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
        if (showSourceBadge) {
            LyricsSourceBadge()
        }

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
    progressFlow: StateFlow<PlayerProgressState>?,
    showTranslation: Boolean,
    activeAccent: Color,
    onSeekTo: (Long) -> Unit,
    style: FullscreenLyricsStyle,
    lyricsConfig: NowPlayingLyricsConfig,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val entries = remember(trackLyrics.syncedLyrics) { buildFullscreenLyricEntries(trackLyrics.syncedLyrics) }
    val activeEntryIndex =
        rememberActiveFullscreenEntryIndex(
            entries = entries,
            positionMs = positionMs,
            progressFlow = progressFlow,
            syncOffsetMs = lyricsConfig.syncOffsetMs,
        )

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    var manualWheelTick by remember { mutableIntStateOf(0) }
    var userScrolledRecently by remember { mutableStateOf(false) }

    LaunchedEffect(isUserDragging, manualWheelTick) {
        if (isUserDragging || manualWheelTick > 0) {
            userScrolledRecently = true
            if (!isUserDragging) {
                delay(USER_SCROLL_RESUME_DELAY_MS.milliseconds)
                userScrolledRecently = false
                manualWheelTick = 0
            }
        }
    }

    val anchorFraction = lyricsConfig.anchorPosition.viewportFraction
    val estimatedActiveHeightPx = with(density) { style.activeLineHeight.toPx().toInt() }
    AutoCenterActiveLyricEffect(
        listState = listState,
        activeEntryIndex = activeEntryIndex,
        entryCount = entries.size,
        viewportHeightPx = viewportHeightPx,
        estimatedActiveHeightPx = estimatedActiveHeightPx,
        anchorFraction = anchorFraction,
        reducedMotion = reducedMotion,
        userScrolledRecently = userScrolledRecently,
    )

    val topAnchorPadding =
        if (viewportHeightPx > 0) {
            with(density) { (viewportHeightPx * anchorFraction).toInt().toDp() }
        } else {
            160.dp
        }
    val bottomAnchorPadding =
        if (viewportHeightPx > 0) {
            with(density) { (viewportHeightPx * (1f - anchorFraction)).toInt().toDp() }
        } else {
            160.dp
        }

    LazyColumn(
        state = listState,
        contentPadding =
            PaddingValues(
                top = topAnchorPadding,
                bottom = bottomAnchorPadding,
                start = style.horizontalPadding,
                end = style.horizontalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(style.itemSpacing),
        modifier =
            modifier
                .onSizeChanged { viewportHeightPx = it.height }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Scroll) {
                                manualWheelTick++
                            }
                        }
                    }
                }.verticalEdgeFadeMask(),
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
                activeAccent = activeAccent,
                style = style,
                lyricsConfig = lyricsConfig,
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
    activeAccent: Color,
    style: FullscreenLyricsStyle,
    lyricsConfig: NowPlayingLyricsConfig,
    reducedMotion: Boolean,
    onSeekTo: () -> Unit,
) {
    val targetAlpha = lyricAlphaForDistance(distance, lyricsConfig.inactiveOpacity)
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

    val blurRadius = if (reducedMotion || !lyricsConfig.blurEnabled) 0.dp else lyricBlurForDistance(distance)
    val currentLineLabel = stringResource(Res.string.a11y_current_lyric_line)
    val rowAlignment =
        if (lyricsConfig.alignment == LyricsAlignment.CENTER) {
            Alignment.CenterHorizontally
        } else {
            Alignment.Start
        }
    val textAlign =
        if (lyricsConfig.alignment == LyricsAlignment.CENTER) {
            TextAlign.Center
        } else {
            TextAlign.Start
        }
    val lineColor =
        if (isActive && lyricsConfig.activeLineColor == LyricsActiveLineColor.ACCENT) {
            activeAccent
        } else {
            Color.White
        }

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
        horizontalAlignment = rowAlignment,
    ) {
        when (entry) {
            is FullscreenLyricEntry.LyricLine -> {
                FullscreenLyricLineContent(
                    entry = entry,
                    isActive = isActive,
                    showTranslation = showTranslation,
                    style = style,
                    activeFontWeight = lyricsConfig.activeLineWeight.toComposeFontWeight(),
                    textColor = lineColor,
                    textAlign = textAlign,
                )
            }

            is FullscreenLyricEntry.InstrumentalGap -> {
                val instrumentalLabel = stringResource(Res.string.a11y_instrumental_break)
                DancingMelodyIndicator(
                    isActive = isActive && !reducedMotion,
                    tint = lineColor,
                    baseFontSize = if (isActive) 28.sp else 20.sp,
                    modifier =
                        Modifier
                            .padding(vertical = 8.dp)
                            .semantics { contentDescription = instrumentalLabel },
                )
            }
        }
    }
}

@Composable
private fun AutoCenterActiveLyricEffect(
    listState: LazyListState,
    activeEntryIndex: Int,
    entryCount: Int,
    viewportHeightPx: Int,
    estimatedActiveHeightPx: Int,
    anchorFraction: Float,
    reducedMotion: Boolean,
    userScrolledRecently: Boolean,
) {
    LaunchedEffect(activeEntryIndex, viewportHeightPx, anchorFraction, reducedMotion, userScrolledRecently) {
        val canScrollToActive = activeEntryIndex in 0 until entryCount && viewportHeightPx > 0
        if (canScrollToActive && !userScrolledRecently) {
            val activeItemHeightPx =
                listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == activeEntryIndex }
                    ?.size
                    ?: estimatedActiveHeightPx
            val centerOffset = activeItemHeightPx / 2
            val firstVisible = listState.firstVisibleItemIndex
            if (reducedMotion || abs(activeEntryIndex - firstVisible) > 8) {
                listState.scrollToItem(index = activeEntryIndex, scrollOffset = centerOffset)
            } else {
                listState.animateScrollToItem(index = activeEntryIndex, scrollOffset = centerOffset)
            }
        }
    }
}

@Composable
private fun rememberActiveFullscreenEntryIndex(
    entries: List<FullscreenLyricEntry>,
    positionMs: Long,
    progressFlow: StateFlow<PlayerProgressState>?,
    syncOffsetMs: Int,
): Int {
    val isWindowVisible = LocalWindowActivity.current.isVisible
    return if (progressFlow != null && isWindowVisible) {
        remember(progressFlow, entries, syncOffsetMs) {
            progressFlow
                .map { resolveActiveFullscreenEntryIndex(entries, it.progressMs, syncOffsetMs = syncOffsetMs) }
                .distinctUntilChanged()
        }.collectAsState(
            initial =
                resolveActiveFullscreenEntryIndex(
                    entries = entries,
                    positionMs = progressFlow.value.progressMs,
                    syncOffsetMs = syncOffsetMs,
                ),
        ).value
    } else {
        remember(entries, positionMs, syncOffsetMs) {
            derivedStateOf { resolveActiveFullscreenEntryIndex(entries, positionMs, syncOffsetMs = syncOffsetMs) }
        }.value
    }
}

@Composable
private fun FullscreenLyricLineContent(
    entry: FullscreenLyricEntry.LyricLine,
    isActive: Boolean,
    showTranslation: Boolean,
    style: FullscreenLyricsStyle,
    activeFontWeight: FontWeight,
    textColor: Color,
    textAlign: TextAlign,
) {
    val inactiveFontWeight = if (activeFontWeight < FontWeight.SemiBold) FontWeight.Normal else FontWeight.SemiBold
    Text(
        text = entry.line.text,
        style =
            MeloType.body.copy(
                fontSize = if (isActive) style.activeFontSize else style.neighborFontSize,
                lineHeight = if (isActive) style.activeLineHeight else style.neighborLineHeight,
                fontWeight = if (isActive) activeFontWeight else inactiveFontWeight,
            ),
        color = textColor,
        textAlign = textAlign,
        modifier = Modifier.fillMaxWidth(),
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
            color = textColor.copy(alpha = if (isActive) 0.84f else 0.72f),
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FullscreenPlainLyricsView(
    text: String,
    style: FullscreenLyricsStyle,
    alignment: LyricsAlignment,
    modifier: Modifier = Modifier,
) {
    val isCentered = alignment == LyricsAlignment.CENTER
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
        contentAlignment = if (isCentered) Alignment.TopCenter else Alignment.TopStart,
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
            textAlign = if (isCentered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
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
