@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
) {
    val hasLyrics = state.trackLyrics.hasDisplayableLyrics()
    val showRightPane = showQueue || (showLyrics && (hasLyrics || state.isLyricsLoading))
    val artworkSize = if (showRightPane) EXPANDED_ARTWORK_SIZE else CENTERED_ARTWORK_SIZE
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

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier =
                    Modifier
                        .widthIn(max = EXPANDED_CONTENT_MAX_WIDTH)
                        .fillMaxSize(),
                horizontalArrangement =
                    if (showRightPane) Arrangement.spacedBy(40.dp) else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier =
                        Modifier
                            .then(
                                if (showRightPane) {
                                    Modifier.widthIn(max = EXPANDED_LEFT_COLUMN_MAX_WIDTH).weight(0.48f)
                                } else {
                                    Modifier.widthIn(max = EXPANDED_LEFT_COLUMN_MAX_WIDTH)
                                },
                            ).fillMaxHeight(),
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
                        chromeAlpha = chromeAlpha,
                        actions = actions,
                        onHeightChanged = { controlsBlockHeightPx = it },
                    )
                }

                if (showRightPane) {
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
                        modifier =
                            Modifier
                                .weight(0.52f)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp),
                    )
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
        if (effectiveShowLyrics) {
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

        Spacer(modifier = Modifier.height(14.dp))

        StackedBottomControls(
            state = state,
            progressFlow = progressFlow,
            activeAccent = activeAccent,
            hasLyrics = hasLyrics,
            isLyricsActive = effectiveShowLyrics,
            isQueueActive = showQueue,
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
        if (effectiveShowLyrics) {
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

        Spacer(modifier = Modifier.height(10.dp))

        StackedBottomControls(
            state = state,
            progressFlow = progressFlow,
            activeAccent = activeAccent,
            hasLyrics = hasLyrics,
            isLyricsActive = effectiveShowLyrics,
            isQueueActive = showQueue,
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
