package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.PlayerUiState

@Composable
internal fun DockedLyricsContent(
    state: PlayerUiState,
    activeAccent: Color,
    viewModel: PlayerViewModel,
) {
    NowPlayingLyricsCard(
        trackLyrics = state.trackLyrics,
        activeLyricIndex = state.activeLyricIndex,
        isLoading = state.isLyricsLoading,
        showTranslation = state.showTranslation,
        isTranslating = state.isTranslating,
        targetLanguage = state.targetLanguage,
        activeAccent = activeAccent,
        onSeekTo = { viewModel.seekTo(it) },
        onToggleTranslation = { viewModel.toggleTranslation() },
        onSelectLanguage = { viewModel.setTargetLanguage(it) },
        modifier = Modifier.fillMaxSize()
    )
}