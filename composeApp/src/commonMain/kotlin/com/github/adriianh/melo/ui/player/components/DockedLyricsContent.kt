package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.LocalWindowActivity
import com.github.adriianh.melo.util.PlayerUiState

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
) {
    IsolatedNowPlayingLyricsCard(
        state = state,
        activeAccent = activeAccent,
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize(),
    )
}
