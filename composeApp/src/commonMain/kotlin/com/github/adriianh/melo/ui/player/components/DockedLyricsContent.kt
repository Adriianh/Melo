package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.adriianh.core.domain.model.LyricsActiveLineColor
import com.github.adriianh.core.domain.model.LyricsActiveLineWeight
import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.LocalWindowActivity
import com.github.adriianh.melo.util.PlayerProgressState
import com.github.adriianh.melo.util.PlayerUiState
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun rememberLiveActiveLyricIndex(viewModel: PlayerViewModel): Int =
    if (LocalWindowActivity.current.isVisible) {
        viewModel.activeLyricIndex.collectAsState().value
    } else {
        viewModel.activeLyricIndex.value
    }

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
internal fun IsolatedNowPlayingLyricsCard(
    state: PlayerUiState,
    activeAccent: Color,
    viewModel: PlayerViewModel,
    onToggleArtwork: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val activeLyricIndex = rememberLiveActiveLyricIndex(viewModel)
    NowPlayingLyricsCard(
        trackLyrics = state.trackLyrics,
        activeLyricIndex = activeLyricIndex,
        isLoading = state.isLyricsLoading,
        showTranslation = state.showTranslation,
        isTranslating = state.isTranslating,
        targetLanguage = state.targetLanguage,
        activeAccent = activeAccent,
        onSeekTo = { viewModel.seekTo(it) },
        onToggleTranslation = { viewModel.toggleTranslation() },
        onSelectLanguage = { viewModel.setTargetLanguage(it) },
        onToggleArtwork = onToggleArtwork,
        modifier = modifier,
    )
}

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
internal fun DockedLyricsContent(
    state: PlayerUiState,
    activeAccent: Color,
    viewModel: PlayerViewModel,
    lyricsConfig: NowPlayingLyricsConfig = defaultDockedLyricsConfig(),
    reducedMotion: Boolean = false,
) {
    DockedLyricsPaneContent(
        trackLyrics = state.trackLyrics,
        positionMs = viewModel.progressState.value.progressMs,
        progressFlow = viewModel.progressState,
        isLoading = state.isLyricsLoading,
        showTranslation = state.showTranslation,
        isTranslating = state.isTranslating,
        targetLanguage = state.targetLanguage,
        activeAccent = activeAccent,
        lyricsConfig = lyricsConfig,
        reducedMotion = reducedMotion,
        onSeekTo = { viewModel.seekTo(it) },
        onToggleTranslation = { viewModel.toggleTranslation() },
        onSelectLanguage = { viewModel.setTargetLanguage(it) },
    )
}

internal fun defaultDockedLyricsConfig(reducedMotion: Boolean = false): NowPlayingLyricsConfig =
    NowPlayingLyricsConfig(
        blurEnabled = !reducedMotion,
        activeLineWeight = LyricsActiveLineWeight.BOLD,
        activeLineColor = LyricsActiveLineColor.WHITE,
        lineSpacingDp = 14,
        showSourceBadge = true,
        showInstrumentalBreaks = true,
    )

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
internal fun DockedLyricsPaneContent(
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
    lyricsConfig: NowPlayingLyricsConfig = defaultDockedLyricsConfig(),
    reducedMotion: Boolean = false,
) {
    FullscreenLyricsPane(
        trackLyrics = trackLyrics,
        positionMs = positionMs,
        progressFlow = progressFlow,
        isLoading = isLoading,
        showTranslation = showTranslation,
        isTranslating = isTranslating,
        targetLanguage = targetLanguage,
        activeAccent = activeAccent,
        onSeekTo = onSeekTo,
        onToggleTranslation = onToggleTranslation,
        onSelectLanguage = onSelectLanguage,
        style = FullscreenLyricsStyle.Docked,
        lyricsConfig = lyricsConfig.copy(blurEnabled = lyricsConfig.blurEnabled && !reducedMotion),
        showControlsOnHoverOnly = false,
        reducedMotion = reducedMotion,
        showFloatingControls = true,
        modifier = modifier.fillMaxSize(),
    )
}
