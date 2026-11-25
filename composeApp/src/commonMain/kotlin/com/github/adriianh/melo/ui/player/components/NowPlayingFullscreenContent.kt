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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.ArtworkCornerStyle
import com.github.adriianh.core.domain.model.LyricsActiveLineColor
import com.github.adriianh.core.domain.model.LyricsAlignment
import com.github.adriianh.core.domain.player.AudioOutputDevice
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.player_lyrics_header
import com.github.adriianh.melo.resources.player_lyrics_unavailable
import com.github.adriianh.melo.resources.player_now_playing_from_album
import com.github.adriianh.melo.resources.player_now_playing_header_label
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.util.LocalWindowActivity
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.MeloWindowSizeClass
import com.github.adriianh.melo.util.PlayerProgressState
import com.github.adriianh.melo.util.PlayerUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    val onAlbumClick: () -> Unit = {},
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
    val onRefreshAudioOutputs: () -> Unit = {},
    val onSelectAudioOutput: (String?) -> Unit = {},
    val onAudioOutputMenuOpenChanged: (Boolean) -> Unit = {},
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
    audioOutputs: List<AudioOutputDevice> = emptyList(),
    selectedAudioOutputId: String? = null,
    lyricsConfig: NowPlayingLyricsConfig = NowPlayingLyricsConfig(),
) {
    val isIdleHidden = !controlsVisible
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
                audioOutputs = audioOutputs,
                selectedAudioOutputId = selectedAudioOutputId,
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
                chromeAlpha = chromeAlpha,
                actions = actions,
                artworkCornerRadiusDp = artworkCornerRadiusDp,
                showRemainingTime = showRemainingTime,
                audioOutputs = audioOutputs,
                selectedAudioOutputId = selectedAudioOutputId,
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
                isIdleHidden = isIdleHidden,
                chromeAlpha = chromeAlpha,
                actions = actions,
                artworkCornerRadiusDp = artworkCornerRadiusDp,
                showRemainingTime = showRemainingTime,
                audioOutputs = audioOutputs,
                selectedAudioOutputId = selectedAudioOutputId,
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
    audioOutputs: List<AudioOutputDevice> = emptyList(),
    selectedAudioOutputId: String? = null,
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
                        audioOutputs = audioOutputs,
                        selectedAudioOutputId = selectedAudioOutputId,
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
    audioOutputs: List<AudioOutputDevice>,
    selectedAudioOutputId: String?,
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
            audioOutputs = audioOutputs,
            selectedAudioOutputId = selectedAudioOutputId,
            onRefreshAudioOutputs = actions.onRefreshAudioOutputs,
            onSelectAudioOutput = actions.onSelectAudioOutput,
            onAudioOutputMenuOpenChanged = actions.onAudioOutputMenuOpenChanged,
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
    chromeAlpha: Float,
    actions: FullscreenPlayerActions,
    artworkCornerRadiusDp: Int,
    showRemainingTime: Boolean,
    audioOutputs: List<AudioOutputDevice>,
    selectedAudioOutputId: String?,
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
                        showActions = chromeAlpha > 0.01f,
                        reducedMotion = reducedMotion,
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
                    val album = state.currentTrack?.album.orEmpty()
                    NowPlayingTopBar(
                        onCollapse = actions.onCollapse,
                        onMoreClick = actions.onMoreClick,
                        headerSubtitle =
                            if (album.isNotBlank()) {
                                stringResource(Res.string.player_now_playing_from_album)
                            } else {
                                stringResource(Res.string.player_now_playing_header_label)
                            },
                        headerTitle = album.ifBlank { state.artist },
                        onHeaderClick = if (album.isNotBlank()) actions.onAlbumClick else actions.onArtistClick,
                        modifier = Modifier.alpha(chromeAlpha),
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
            audioOutputs = audioOutputs,
            selectedAudioOutputId = selectedAudioOutputId,
            actions = actions,
            modifier = Modifier.widthIn(max = 460.dp).alpha(chromeAlpha),
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
    isIdleHidden: Boolean,
    chromeAlpha: Float,
    actions: FullscreenPlayerActions,
    artworkCornerRadiusDp: Int,
    showRemainingTime: Boolean,
    audioOutputs: List<AudioOutputDevice>,
    selectedAudioOutputId: String?,
    lyricsConfig: NowPlayingLyricsConfig,
    modifier: Modifier = Modifier,
) {
    val hasLyrics = state.trackLyrics.hasDisplayableLyrics()
    val effectiveShowLyrics = showLyrics && (hasLyrics || state.isLyricsLoading)
    val showLiveLyricStrip =
        lyricsConfig.showCompactPreviewLine && state.trackLyrics?.hasSync == true
    var controlsBlockHeightPx by remember { mutableIntStateOf(0) }
    val heroTranslationY by animateFloatAsState(
        targetValue = if (isIdleHidden && !effectiveShowLyrics) controlsBlockHeightPx * 0.35f else 0f,
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
        label = "CompactHeroIdleCenterY",
    )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedContent(
            targetState = effectiveShowLyrics,
            transitionSpec = { verticalLyricsTransitionSpec(targetState, reducedMotion) },
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
                        showActions = !isIdleHidden,
                        reducedMotion = reducedMotion,
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
                        isIdleHidden = isIdleHidden,
                        reducedMotion = reducedMotion,
                        showFloatingControls = false,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                } else {
                    CompactArtworkView(
                        state = state,
                        positionMs = positionMs,
                        progressFlow = progressFlow,
                        activeAccent = activeAccent,
                        hasLyrics = hasLyrics,
                        showLiveLyricStrip = showLiveLyricStrip,
                        artworkCornerRadiusDp = artworkCornerRadiusDp,
                        lyricsConfig = lyricsConfig,
                        reducedMotion = reducedMotion,
                        chromeAlpha = chromeAlpha,
                        heroTranslationY = heroTranslationY,
                        actions = actions,
                    )
                }
            }
        }

        val bottomEnterMs = if (reducedMotion) 0 else 240
        val bottomExitMs = if (reducedMotion) 0 else 420
        AnimatedVisibility(
            visible = !isIdleHidden || !effectiveShowLyrics,
            enter =
                expandVertically(
                    expandFrom = Alignment.Bottom,
                    animationSpec = tween(bottomEnterMs, easing = FastOutSlowInEasing),
                ) + fadeIn(tween(bottomEnterMs, easing = FastOutSlowInEasing)),
            exit =
                shrinkVertically(
                    shrinkTowards = Alignment.Bottom,
                    animationSpec = tween(bottomExitMs, easing = FastOutSlowInEasing),
                ) + fadeOut(tween(bottomExitMs, easing = FastOutSlowInEasing)),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(10.dp))

                StackedBottomControls(
                    state = state,
                    progressFlow = progressFlow,
                    activeAccent = activeAccent,
                    hasLyrics = hasLyrics,
                    isLyricsActive = effectiveShowLyrics,
                    isQueueActive = showQueue,
                    showRemainingTime = showRemainingTime,
                    audioOutputs = audioOutputs,
                    selectedAudioOutputId = selectedAudioOutputId,
                    actions = actions,
                    modifier =
                        Modifier
                            .onSizeChanged { controlsBlockHeightPx = it.height }
                            .alpha(chromeAlpha),
                )
            }
        }
    }
}

private fun verticalLyricsTransitionSpec(
    targetShowingLyrics: Boolean,
    reducedMotion: Boolean,
): androidx.compose.animation.ContentTransform {
    val enterMs = if (reducedMotion) 0 else 300
    val exitMs = if (reducedMotion) 0 else 220
    val enterOffsetSign = if (targetShowingLyrics) 1 else -1
    return (
        fadeIn(tween(enterMs, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(enterMs, easing = FastOutSlowInEasing)) { enterOffsetSign * (it / 10) }
    ) togetherWith (
        fadeOut(tween(exitMs, easing = FastOutSlowInEasing)) +
            slideOutVertically(tween(exitMs, easing = FastOutSlowInEasing)) { -enterOffsetSign * (it / 10) }
    )
}

@Composable
private fun CompactArtworkView(
    state: PlayerUiState,
    positionMs: Long,
    progressFlow: StateFlow<PlayerProgressState>?,
    activeAccent: Color,
    hasLyrics: Boolean,
    showLiveLyricStrip: Boolean,
    artworkCornerRadiusDp: Int,
    lyricsConfig: NowPlayingLyricsConfig,
    reducedMotion: Boolean,
    chromeAlpha: Float,
    heroTranslationY: Float,
    actions: FullscreenPlayerActions,
) {
    val album = state.currentTrack?.album.orEmpty()
    NowPlayingTopBar(
        onCollapse = actions.onCollapse,
        onMoreClick = actions.onMoreClick,
        headerSubtitle =
            if (album.isNotBlank()) {
                stringResource(Res.string.player_now_playing_from_album)
            } else {
                stringResource(Res.string.player_now_playing_header_label)
            },
        headerTitle = album.ifBlank { state.artist },
        onHeaderClick = if (album.isNotBlank()) actions.onAlbumClick else actions.onArtistClick,
        modifier = Modifier.alpha(chromeAlpha),
    )

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .graphicsLayer { translationY = heroTranslationY },
        contentAlignment = Alignment.Center,
    ) {
        val reservedTextHeight = if (showLiveLyricStrip) 196.dp else 100.dp
        val artworkSide = minOf(maxWidth - 16.dp, maxHeight - reservedTextHeight).coerceAtLeast(160.dp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            FullscreenArtworkCard(
                albumArt = state.albumArt,
                title = state.title,
                isBuffering = state.isBuffering,
                activeAccent = activeAccent,
                cornerRadius = artworkCornerRadiusDp.dp,
                onClick = if (hasLyrics) actions.onToggleLyrics else null,
                modifier = Modifier.size(artworkSide),
            )

            Spacer(modifier = Modifier.height(20.dp))

            FullscreenTrackTitleRow(
                title = state.title,
                artist = state.artist,
                isFavorite = state.isFavorite,
                activeAccent = activeAccent,
                onArtistClick = actions.onArtistClick,
                onToggleFavorite = actions.onToggleFavorite,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            if (showLiveLyricStrip) {
                Spacer(modifier = Modifier.height(14.dp))
                CompactLiveLyricStrip(
                    trackLyrics = state.trackLyrics,
                    positionMs = positionMs,
                    progressFlow = progressFlow,
                    activeAccent = activeAccent,
                    lyricsConfig = lyricsConfig,
                    reducedMotion = reducedMotion,
                    onClick = actions.onToggleLyrics,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun CompactLiveLyricStrip(
    trackLyrics: com.github.adriianh.core.domain.model.TrackLyrics?,
    positionMs: Long,
    progressFlow: StateFlow<PlayerProgressState>?,
    activeAccent: Color,
    lyricsConfig: NowPlayingLyricsConfig,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (trackLyrics == null || !trackLyrics.hasSync) return
    val entries =
        remember(trackLyrics.syncedLyrics, lyricsConfig.showInstrumentalBreaks) {
            buildFullscreenLyricEntries(
                lines = trackLyrics.syncedLyrics,
                includeInstrumentalBreaks = lyricsConfig.showInstrumentalBreaks,
            )
        }
    val syncOffsetMs = lyricsConfig.syncOffsetMs
    val isWindowVisible = LocalWindowActivity.current.isVisible
    val activeIndex =
        if (progressFlow != null && isWindowVisible) {
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

    val lyricLines = remember(entries) { entries.withIndex().filter { it.value is FullscreenLyricEntry.LyricLine } }
    val activePos =
        lyricLines
            .indexOfLast { it.index <= activeIndex }
            .takeIf { it >= 0 }
            ?: 0
    val activeLine = (lyricLines.getOrNull(activePos)?.value as? FullscreenLyricEntry.LyricLine)?.line?.text.orEmpty()
    val nextLine = (lyricLines.getOrNull(activePos + 1)?.value as? FullscreenLyricEntry.LyricLine)?.line?.text
    if (activeLine.isBlank()) return

    val textAlign = if (lyricsConfig.alignment == LyricsAlignment.CENTER) TextAlign.Center else TextAlign.Start
    val activeColor =
        if (lyricsConfig.activeLineColor == LyricsActiveLineColor.ACCENT) activeAccent else Color.White
    val activeWeight = lyricsConfig.activeLineWeight.toComposeFontWeight()
    val nextAlpha = lyricAlphaForDistance(1, lyricsConfig.inactiveOpacity)
    val nextBlur = if (!reducedMotion && lyricsConfig.blurEnabled) lyricBlurForDistance(1) else 0.dp

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.12f),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.player_lyrics_header).uppercase(),
                fontSize = 9.5.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.10.em,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.48f),
                textAlign = textAlign,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )

            AnimatedContent(
                targetState = activeLine to nextLine,
                transitionSpec = {
                    val animMs = if (reducedMotion) 0 else 260
                    (
                        fadeIn(tween(animMs, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(animMs, easing = FastOutSlowInEasing)) { it / 3 }
                    ) togetherWith (
                        fadeOut(tween(animMs, easing = FastOutSlowInEasing)) +
                            slideOutVertically(tween(animMs, easing = FastOutSlowInEasing)) { -it / 3 }
                    )
                },
                label = "CompactLiveLyricLine",
                modifier = Modifier.fillMaxWidth(),
            ) { (current, upcoming) ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = current,
                        style =
                            MeloType.body.copy(
                                fontSize = 17.sp,
                                lineHeight = 22.sp,
                                fontWeight = activeWeight,
                            ),
                        color = activeColor,
                        textAlign = textAlign,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!upcoming.isNullOrBlank()) {
                        val blurModifier = if (nextBlur > 0.dp) Modifier.blur(nextBlur) else Modifier
                        Text(
                            text = upcoming,
                            style =
                                MeloType.body.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            color = Color.White.copy(alpha = nextAlpha),
                            textAlign = textAlign,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().then(blurModifier),
                        )
                    }
                }
            }
        }
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
    audioOutputs: List<AudioOutputDevice>,
    selectedAudioOutputId: String?,
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
            audioOutputs = audioOutputs,
            selectedAudioOutputId = selectedAudioOutputId,
            onRefreshAudioOutputs = actions.onRefreshAudioOutputs,
            onSelectAudioOutput = actions.onSelectAudioOutput,
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
