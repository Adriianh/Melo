@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.ArtworkCornerStyle
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.player_lyrics_unavailable
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.MeloWindowSizeClass
import com.github.adriianh.melo.util.PlayerProgressState
import com.github.adriianh.melo.util.PlayerUiState
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

private val EXPANDED_CONTENT_MAX_WIDTH = 1280.dp
private val EXPANDED_LEFT_COLUMN_MAX_WIDTH = 560.dp
private val EXPANDED_CONTROLS_MAX_WIDTH = 420.dp
private val EXPANDED_ARTWORK_SIZE = 340.dp
private val CENTERED_ARTWORK_SIZE = 400.dp
private const val SWIPE_UP_LYRICS_THRESHOLD_PX = -72f

internal data class FullscreenPlayerActions(
    val onCollapse: () -> Unit,
    val onMoreClick: ((SongMenuAnchor?) -> Unit)? = null,
    val onArtistClick: () -> Unit = {},
    val onToggleFavorite: () -> Unit = {},
    val onSeekTo: (Long) -> Unit = {},
    val onTogglePlayPause: () -> Unit = {},
    val onPlayNext: () -> Unit = {},
    val onPlayPrevious: () -> Unit = {},
    val onToggleShuffle: () -> Unit = {},
    val onToggleRepeat: () -> Unit = {},
    val onVolumeChange: (Float) -> Unit = {},
    val onToggleMute: () -> Unit = {},
    val onToggleLyrics: () -> Unit = {},
    val onToggleQueue: () -> Unit = {},
    val onCloseQueue: () -> Unit = {},
    val onToggleTranslation: () -> Unit = {},
    val onSelectLanguage: (String) -> Unit = {},
    val onToggleShowRemainingTime: () -> Unit = {},
)

@Composable
internal fun NowPlayingFullscreenContent(
    state: PlayerUiState,
    positionMs: Long,
    progressFlow: StateFlow<PlayerProgressState>?,
    volume: Float,
    activeAccent: Color,
    sizeClass: MeloWindowSizeClass,
    isDesktop: Boolean,
    reducedMotion: Boolean,
    showLyrics: Boolean,
    showQueue: Boolean,
    controlsVisible: Boolean,
    actions: FullscreenPlayerActions,
    queueSidePanelContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    artworkCornerRadiusDp: Int = ArtworkCornerStyle.ROUNDED.radiusDp,
    showRemainingTime: Boolean = false,
    lyricsConfig: NowPlayingLyricsConfig = NowPlayingLyricsConfig(),
) {
    val isIdleHidden = isDesktop && !controlsVisible
    val chromeAlpha by animateFloatAsState(
        targetValue = if (!isIdleHidden) 1f else 0f,
        animationSpec =
            tween(
                durationMillis =
                    when {
                        reducedMotion -> 0
                        !isIdleHidden -> 220
                        else -> 650
                    },
                easing = FastOutSlowInEasing,
            ),
        label = "FullscreenChromeAlpha",
    )

    when {
        sizeClass.isExpandedOrMediumLandscape -> {
            ExpandedNowPlayingLayout(
                state = state,
                positionMs = positionMs,
                progressFlow = progressFlow,
                volume = volume,
                activeAccent = activeAccent,
                isDesktop = isDesktop,
                reducedMotion = reducedMotion,
                showLyrics = showLyrics,
                showQueue = showQueue,
                isIdleHidden = isIdleHidden,
                chromeAlpha = chromeAlpha,
                actions = actions,
                queueSidePanelContent = queueSidePanelContent,
                artworkCornerRadiusDp = artworkCornerRadiusDp,
                showRemainingTime = showRemainingTime,
                lyricsConfig = lyricsConfig,
                modifier = modifier,
            )
        }

        sizeClass.isMediumPortrait -> {
            MediumPortraitNowPlayingLayout(
                state = state,
                positionMs = positionMs,
                progressFlow = progressFlow,
                activeAccent = activeAccent,
                reducedMotion = reducedMotion,
                showLyrics = showLyrics,
                showQueue = showQueue,
                actions = actions,
                artworkCornerRadiusDp = artworkCornerRadiusDp,
                showRemainingTime = showRemainingTime,
                lyricsConfig = lyricsConfig,
                modifier = modifier,
            )
        }

        else -> {
            CompactNowPlayingLayout(
                state = state,
                positionMs = positionMs,
                progressFlow = progressFlow,
                activeAccent = activeAccent,
                reducedMotion = reducedMotion,
                showLyrics = showLyrics,
                showQueue = showQueue,
                actions = actions,
                artworkCornerRadiusDp = artworkCornerRadiusDp,
                showRemainingTime = showRemainingTime,
                lyricsConfig = lyricsConfig,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ExpandedNowPlayingLayout(
    state: PlayerUiState,
    positionMs: Long,
    progressFlow: StateFlow<PlayerProgressState>?,
    volume: Float,
    activeAccent: Color,
    isDesktop: Boolean,
    reducedMotion: Boolean,
    showLyrics: Boolean,
    showQueue: Boolean,
    isIdleHidden: Boolean,
    chromeAlpha: Float,
    actions: FullscreenPlayerActions,
    queueSidePanelContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    artworkCornerRadiusDp: Int = ArtworkCornerStyle.ROUNDED.radiusDp,
    showRemainingTime: Boolean = false,
    lyricsConfig: NowPlayingLyricsConfig = NowPlayingLyricsConfig(),
) {
    val hasLyrics = state.trackLyrics.hasDisplayableLyrics()
    val showRightPane = showQueue || (showLyrics && (hasLyrics || state.isLyricsLoading))
    val artworkSize by animateDpAsState(
        targetValue = if (showRightPane) EXPANDED_ARTWORK_SIZE else CENTERED_ARTWORK_SIZE,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 360, easing = FastOutSlowInEasing),
        label = "FullscreenArtworkSize",
    )
    var controlsBlockHeightPx by remember { mutableIntStateOf(0) }
    val heroTranslationY by animateFloatAsState(
        targetValue = if (isIdleHidden) controlsBlockHeightPx * 0.5f else 0f,
        animationSpec =
            tween(
                durationMillis =
                    when {
                        reducedMotion -> 0
                        isIdleHidden -> 650
                        else -> 280
                    },
                easing = FastOutSlowInEasing,
            ),
        label = "FullscreenHeroIdleCenterY",
    )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        NowPlayingTopBar(
            onCollapse = actions.onCollapse,
            onMoreClick = actions.onMoreClick,
            modifier = Modifier.alpha(chromeAlpha),
        )

        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val containerWidth = minOf(maxWidth, EXPANDED_CONTENT_MAX_WIDTH)
            val rightPaneWidth = (containerWidth * 0.52f).coerceIn(360.dp, 640.dp)
            val leftColumnWidth =
                (containerWidth - rightPaneWidth - 40.dp).coerceIn(340.dp, EXPANDED_LEFT_COLUMN_MAX_WIDTH)
            val paneEnterMs = if (reducedMotion) 0 else 360
            val paneExitMs = if (reducedMotion) 0 else 280

            Row(
                modifier =
                    Modifier
                        .width(containerWidth)
                        .fillMaxHeight(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier =
                        Modifier
                            .width(leftColumnWidth)
                            .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .graphicsLayer { translationY = heroTranslationY },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        FullscreenArtworkCard(
                            albumArt = state.albumArt,
                            title = state.title,
                            isBuffering = state.isBuffering,
                            activeAccent = activeAccent,
                            cornerRadius = artworkCornerRadiusDp.dp,
                            modifier = Modifier.size(artworkSize),
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        FullscreenTrackTitleRow(
                            title = state.title,
                            artist = state.artist,
                            isFavorite = state.isFavorite,
                            activeAccent = activeAccent,
                            onArtistClick = actions.onArtistClick,
                            onToggleFavorite = actions.onToggleFavorite,
                            modifier = Modifier.widthIn(max = EXPANDED_CONTROLS_MAX_WIDTH),
                        )
                    }

                    ExpandedControlsBlock(
                        state = state,
                        progressFlow = progressFlow,
                        volume = volume,
                        activeAccent = activeAccent,
                        hasLyrics = hasLyrics,
                        showLyrics = showLyrics,
                        showQueue = showQueue,
                        showRemainingTime = showRemainingTime,
                        chromeAlpha = chromeAlpha,
                        actions = actions,
                        onHeightChanged = { controlsBlockHeightPx = it },
                    )
                }

                AnimatedVisibility(
                    visible = showRightPane,
                    enter =
                        expandHorizontally(
                            expandFrom = Alignment.Start,
                            animationSpec = tween(paneEnterMs, easing = FastOutSlowInEasing),
                        ) +
                            slideInHorizontally(tween(paneEnterMs, easing = FastOutSlowInEasing)) { it / 5 } +
                            fadeIn(tween(paneEnterMs, easing = FastOutSlowInEasing)),
                    exit =
                        shrinkHorizontally(
                            shrinkTowards = Alignment.Start,
                            animationSpec = tween(paneExitMs, easing = FastOutSlowInEasing),
                        ) +
                            slideOutHorizontally(tween(paneExitMs, easing = FastOutSlowInEasing)) { it / 5 } +
                            fadeOut(tween(paneExitMs, easing = FastOutSlowInEasing)),
                ) {
                    Row(modifier = Modifier.fillMaxHeight()) {
                        Spacer(modifier = Modifier.width(40.dp))
                        ExpandedRightPane(
                            state = state,
                            positionMs = positionMs,
                            progressFlow = progressFlow,
                            activeAccent = activeAccent,
                            isDesktop = isDesktop,
                            isIdleHidden = isIdleHidden,
                            reducedMotion = reducedMotion,
                            showQueue = showQueue,
                            actions = actions,
                            queueSidePanelContent = queueSidePanelContent,
                            lyricsConfig = lyricsConfig,
                            modifier =
                                Modifier
                                    .width(rightPaneWidth)
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedRightPane(
    state: PlayerUiState,
    positionMs: Long,
    progressFlow: StateFlow<PlayerProgressState>?,
    activeAccent: Color,
    isDesktop: Boolean,
    isIdleHidden: Boolean,
    reducedMotion: Boolean,
    showQueue: Boolean,
    actions: FullscreenPlayerActions,
    queueSidePanelContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    lyricsConfig: NowPlayingLyricsConfig = NowPlayingLyricsConfig(),
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        AnimatedContent(
            targetState = showQueue,
            transitionSpec = {
                val enterMs = if (reducedMotion) 0 else 320
                val exitMs = if (reducedMotion) 0 else 240
                val enterSpec = tween<Float>(enterMs, easing = FastOutSlowInEasing)
                val exitSpec = tween<Float>(exitMs, easing = FastOutSlowInEasing)
                val transition =
                    if (targetState) {
                        val enter =
                            fadeIn(enterSpec) +
                                slideInHorizontally(tween(enterMs, easing = FastOutSlowInEasing)) { it / 4 } +
                                scaleIn(enterSpec, initialScale = 0.96f)
                        val exit =
                            fadeOut(exitSpec) +
                                slideOutHorizontally(tween(exitMs, easing = FastOutSlowInEasing)) { -it / 6 }
                        enter togetherWith exit
                    } else {
                        val enter =
                            fadeIn(enterSpec) +
                                slideInHorizontally(tween(enterMs, easing = FastOutSlowInEasing)) { -it / 6 }
                        val exit =
                            fadeOut(exitSpec) +
                                slideOutHorizontally(tween(exitMs, easing = FastOutSlowInEasing)) { it / 4 } +
                                scaleOut(exitSpec, targetScale = 0.96f)
                        enter togetherWith exit
                    }
                transition using SizeTransform(clip = false)
            },
            label = "ExpandedRightPaneTransition",
        ) { queueOpen ->
            if (queueOpen) {
                FullscreenQueueSidePanel(
                    onClose = actions.onCloseQueue,
                    queueContent = queueSidePanelContent,
                )
            } else {
                FullscreenLyricsPane(
                    trackLyrics = state.trackLyrics,
                    positionMs = positionMs,
                    progressFlow = progressFlow,
                    isLoading = state.isLyricsLoading,
                    showTranslation = state.showTranslation,
                    isTranslating = state.isTranslating,
                    targetLanguage = state.targetLanguage,
                    activeAccent = activeAccent,
                    onSeekTo = actions.onSeekTo,
                    onToggleTranslation = actions.onToggleTranslation,
                    onSelectLanguage = actions.onSelectLanguage,
                    style = FullscreenLyricsStyle.Expanded,
                    lyricsConfig = lyricsConfig,
                    showControlsOnHoverOnly = isDesktop,
                    isIdleHidden = isIdleHidden,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ExpandedControlsBlock(
    state: PlayerUiState,
    progressFlow: StateFlow<PlayerProgressState>?,
    volume: Float,
    activeAccent: Color,
    hasLyrics: Boolean,
    showLyrics: Boolean,
    showQueue: Boolean,
    showRemainingTime: Boolean,
    chromeAlpha: Float,
    actions: FullscreenPlayerActions,
    onHeightChanged: (Int) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .widthIn(max = EXPANDED_CONTROLS_MAX_WIDTH)
                .fillMaxWidth()
                .onSizeChanged { onHeightChanged(it.height) }
                .alpha(chromeAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        PlayerSlider(
            state = state,
            activeAccent = activeAccent,
            onSeekTo = actions.onSeekTo,
            progressState = progressFlow,
            timeLabelColor = Color.White.copy(alpha = 0.72f),
            inactiveTrackColor = Color.White.copy(alpha = 0.22f),
            showRemainingTime = showRemainingTime,
            onToggleShowRemainingTime = actions.onToggleShowRemainingTime,
        )

        Spacer(modifier = Modifier.height(12.dp))

        PlayerTransportControls(
            isPlaying = state.isPlaying,
            isBuffering = state.isBuffering,
            shuffleEnabled = state.shuffleEnabled,
            repeatMode = state.repeatMode,
            hasPrevious = state.hasPrevious,
            hasNext = state.hasNext,
            activeAccent = activeAccent,
            onToggleShuffle = actions.onToggleShuffle,
            onPlayPrevious = actions.onPlayPrevious,
            onTogglePlayPause = actions.onTogglePlayPause,
            onPlayNext = actions.onPlayNext,
            onToggleRepeat = actions.onToggleRepeat,
            playButtonSize = 68.dp,
            primaryIconTint = Color.White,
            mutedIconTint = Color.White.copy(alpha = 0.68f),
        )

        Spacer(modifier = Modifier.height(10.dp))

        ExpandedUtilityRow(
            volume = volume,
            onVolumeChange = actions.onVolumeChange,
            onToggleMute = actions.onToggleMute,
            hasLyrics = hasLyrics,
            isLyricsActive = showLyrics && !showQueue,
            isQueueActive = showQueue,
            activeAccent = activeAccent,
            onToggleLyrics = actions.onToggleLyrics,
            onToggleQueue = actions.onToggleQueue,
        )

        if (!hasLyrics && !state.isLyricsLoading) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.player_lyrics_unavailable),
                style = MeloType.labelSmall,
                color = Color.White.copy(alpha = 0.68f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MediumPortraitNowPlayingLayout(
    state: PlayerUiState,
    positionMs: Long,
    progressFlow: StateFlow<PlayerProgressState>?,
    activeAccent: Color,
    reducedMotion: Boolean,
    showLyrics: Boolean,
    showQueue: Boolean,
    actions: FullscreenPlayerActions,
    artworkCornerRadiusDp: Int,
    showRemainingTime: Boolean,
    lyricsConfig: NowPlayingLyricsConfig,
    modifier: Modifier = Modifier,
) {
    val hasLyrics = state.trackLyrics.hasDisplayableLyrics()
    val effectiveShowLyrics = showLyrics && (hasLyrics || state.isLyricsLoading)
    var cumulativeDragY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .pointerInput(hasLyrics, effectiveShowLyrics) {
                    if (!hasLyrics || effectiveShowLyrics) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = { cumulativeDragY = 0f },
                        onVerticalDrag = { _, dragAmount -> cumulativeDragY += dragAmount },
                        onDragEnd = {
                            if (cumulativeDragY < SWIPE_UP_LYRICS_THRESHOLD_PX) {
                                actions.onToggleLyrics()
                            }
                            cumulativeDragY = 0f
                        },
                    )
                },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedContent(
            targetState = effectiveShowLyrics,
            transitionSpec = {
                val enterMs = if (reducedMotion) 0 else 300
                val exitMs = if (reducedMotion) 0 else 220
                if (targetState) {
                    (
                        fadeIn(tween(enterMs, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(enterMs, easing = FastOutSlowInEasing)) { it / 10 }
                    ) togetherWith (
                        fadeOut(tween(exitMs, easing = FastOutSlowInEasing)) +
                            slideOutVertically(tween(exitMs, easing = FastOutSlowInEasing)) { -it / 10 }
                    )
                } else {
                    (
                        fadeIn(tween(enterMs, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(enterMs, easing = FastOutSlowInEasing)) { -it / 10 }
                    ) togetherWith (
                        fadeOut(tween(exitMs, easing = FastOutSlowInEasing)) +
                            slideOutVertically(tween(exitMs, easing = FastOutSlowInEasing)) { it / 10 }
                    )
                }
            },
            label = "MediumPortraitLyricsTransition",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { showingLyrics ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showingLyrics) {
                    CompactTrackHeader(
                        albumArt = state.albumArt,
                        title = state.title,
                        artist = state.artist,
                        isFavorite = state.isFavorite,
                        activeAccent = activeAccent,
                        onCollapse = actions.onCollapse,
                        onArtistClick = actions.onArtistClick,
                        onToggleFavorite = actions.onToggleFavorite,
                        onMoreClick = actions.onMoreClick,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FullscreenLyricsPane(
                        trackLyrics = state.trackLyrics,
                        positionMs = positionMs,
                        progressFlow = progressFlow,
                        isLoading = state.isLyricsLoading,
                        showTranslation = state.showTranslation,
                        isTranslating = state.isTranslating,
                        targetLanguage = state.targetLanguage,
                        activeAccent = activeAccent,
                        onSeekTo = actions.onSeekTo,
                        onToggleTranslation = actions.onToggleTranslation,
                        onSelectLanguage = actions.onSelectLanguage,
                        style = FullscreenLyricsStyle.Expanded,
                        lyricsConfig = lyricsConfig,
                        showControlsOnHoverOnly = false,
                        reducedMotion = reducedMotion,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                } else {
                    NowPlayingTopBar(
                        onCollapse = actions.onCollapse,
                        onMoreClick = actions.onMoreClick,
                    )

                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        FullscreenArtworkCard(
                            albumArt = state.albumArt,
                            title = state.title,
                            isBuffering = state.isBuffering,
                            activeAccent = activeAccent,
                            cornerRadius = artworkCornerRadiusDp.dp,
                            onClick = if (hasLyrics) actions.onToggleLyrics else null,
                            modifier = Modifier.size(360.dp),
                        )
                    }

                    FullscreenTrackTitleRow(
                        title = state.title,
                        artist = state.artist,
                        isFavorite = state.isFavorite,
                        activeAccent = activeAccent,
                        onArtistClick = actions.onArtistClick,
                        onToggleFavorite = actions.onToggleFavorite,
                        modifier = Modifier.widthIn(max = 460.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        StackedBottomControls(
            state = state,
            progressFlow = progressFlow,
            activeAccent = activeAccent,
            hasLyrics = hasLyrics,
            isLyricsActive = effectiveShowLyrics,
            isQueueActive = showQueue,
            showRemainingTime = showRemainingTime,
            actions = actions,
            modifier = Modifier.widthIn(max = 460.dp),
        )
    }
}

@Composable
private fun CompactNowPlayingLayout(
    state: PlayerUiState,
    positionMs: Long,
    progressFlow: StateFlow<PlayerProgressState>?,
    activeAccent: Color,
    reducedMotion: Boolean,
    showLyrics: Boolean,
    showQueue: Boolean,
    actions: FullscreenPlayerActions,
    artworkCornerRadiusDp: Int,
    showRemainingTime: Boolean,
    lyricsConfig: NowPlayingLyricsConfig,
    modifier: Modifier = Modifier,
) {
    val hasLyrics = state.trackLyrics.hasDisplayableLyrics()
    val effectiveShowLyrics = showLyrics && (hasLyrics || state.isLyricsLoading)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedContent(
            targetState = effectiveShowLyrics,
            transitionSpec = {
                val enterMs = if (reducedMotion) 0 else 300
                val exitMs = if (reducedMotion) 0 else 220
                if (targetState) {
                    (
                        fadeIn(tween(enterMs, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(enterMs, easing = FastOutSlowInEasing)) { it / 10 }
                    ) togetherWith (
                        fadeOut(tween(exitMs, easing = FastOutSlowInEasing)) +
                            slideOutVertically(tween(exitMs, easing = FastOutSlowInEasing)) { -it / 10 }
                    )
                } else {
                    (
                        fadeIn(tween(enterMs, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(enterMs, easing = FastOutSlowInEasing)) { -it / 10 }
                    ) togetherWith (
                        fadeOut(tween(exitMs, easing = FastOutSlowInEasing)) +
                            slideOutVertically(tween(exitMs, easing = FastOutSlowInEasing)) { it / 10 }
                    )
                }
            },
            label = "CompactLyricsTransition",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { showingLyrics ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showingLyrics) {
                    CompactTrackHeader(
                        albumArt = state.albumArt,
                        title = state.title,
                        artist = state.artist,
                        isFavorite = state.isFavorite,
                        activeAccent = activeAccent,
                        onCollapse = actions.onCollapse,
                        onArtistClick = actions.onArtistClick,
                        onToggleFavorite = actions.onToggleFavorite,
                        onMoreClick = actions.onMoreClick,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FullscreenLyricsPane(
                        trackLyrics = state.trackLyrics,
                        positionMs = positionMs,
                        progressFlow = progressFlow,
                        isLoading = state.isLyricsLoading,
                        showTranslation = state.showTranslation,
                        isTranslating = state.isTranslating,
                        targetLanguage = state.targetLanguage,
                        activeAccent = activeAccent,
                        onSeekTo = actions.onSeekTo,
                        onToggleTranslation = actions.onToggleTranslation,
                        onSelectLanguage = actions.onSelectLanguage,
                        style = FullscreenLyricsStyle.Compact,
                        lyricsConfig = lyricsConfig,
                        showControlsOnHoverOnly = false,
                        reducedMotion = reducedMotion,
                        showFloatingControls = false,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                } else {
                    NowPlayingTopBar(
                        onCollapse = actions.onCollapse,
                        onMoreClick = actions.onMoreClick,
                    )

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        FullscreenArtworkCard(
                            albumArt = state.albumArt,
                            title = state.title,
                            isBuffering = state.isBuffering,
                            activeAccent = activeAccent,
                            cornerRadius = artworkCornerRadiusDp.dp,
                            onClick = if (hasLyrics) actions.onToggleLyrics else null,
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(1f, matchHeightConstraintsFirst = true),
                        )
                    }

                    FullscreenTrackTitleRow(
                        title = state.title,
                        artist = state.artist,
                        isFavorite = state.isFavorite,
                        activeAccent = activeAccent,
                        onArtistClick = actions.onArtistClick,
                        onToggleFavorite = actions.onToggleFavorite,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        StackedBottomControls(
            state = state,
            progressFlow = progressFlow,
            activeAccent = activeAccent,
            hasLyrics = hasLyrics,
            isLyricsActive = effectiveShowLyrics,
            isQueueActive = showQueue,
            showRemainingTime = showRemainingTime,
            actions = actions,
        )
    }
}

@Composable
private fun StackedBottomControls(
    state: PlayerUiState,
    progressFlow: StateFlow<PlayerProgressState>?,
    activeAccent: Color,
    hasLyrics: Boolean,
    isLyricsActive: Boolean,
    isQueueActive: Boolean,
    showRemainingTime: Boolean,
    actions: FullscreenPlayerActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlayerSlider(
            state = state,
            activeAccent = activeAccent,
            onSeekTo = actions.onSeekTo,
            progressState = progressFlow,
            timeLabelColor = Color.White.copy(alpha = 0.72f),
            inactiveTrackColor = Color.White.copy(alpha = 0.22f),
            showRemainingTime = showRemainingTime,
            onToggleShowRemainingTime = actions.onToggleShowRemainingTime,
        )

        Spacer(modifier = Modifier.height(6.dp))

        PlayerTransportControls(
            isPlaying = state.isPlaying,
            isBuffering = state.isBuffering,
            shuffleEnabled = state.shuffleEnabled,
            repeatMode = state.repeatMode,
            hasPrevious = state.hasPrevious,
            hasNext = state.hasNext,
            activeAccent = activeAccent,
            onToggleShuffle = actions.onToggleShuffle,
            onPlayPrevious = actions.onPlayPrevious,
            onTogglePlayPause = actions.onTogglePlayPause,
            onPlayNext = actions.onPlayNext,
            onToggleRepeat = actions.onToggleRepeat,
            playButtonSize = 64.dp,
            primaryIconTint = Color.White,
            mutedIconTint = Color.White.copy(alpha = 0.68f),
        )

        Spacer(modifier = Modifier.height(6.dp))

        CompactUtilityRow(
            hasLyrics = hasLyrics,
            isLyricsActive = isLyricsActive,
            isQueueActive = isQueueActive,
            showTranslation = state.showTranslation,
            isTranslating = state.isTranslating,
            targetLanguage = state.targetLanguage,
            activeAccent = activeAccent,
            onToggleLyrics = actions.onToggleLyrics,
            onToggleTranslation = actions.onToggleTranslation,
            onSelectLanguage = actions.onSelectLanguage,
            onToggleQueue = actions.onToggleQueue,
        )

        if (!hasLyrics && !state.isLyricsLoading) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.player_lyrics_unavailable),
                style = MeloType.labelSmall,
                color = Color.White.copy(alpha = 0.68f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
