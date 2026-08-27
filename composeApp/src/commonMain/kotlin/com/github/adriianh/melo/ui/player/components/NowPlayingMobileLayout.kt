package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.PlayerUiState

@Composable
internal fun NowPlayingMobileLayout(
    state: PlayerUiState,
    activeAccent: Color,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArtistClick: () -> Unit,
    onOpenQueue: () -> Unit,
    onStartRadio: (Track) -> Unit,
    onOpenArtist: () -> Unit,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    if (showLyrics) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
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
                onToggleArtwork = onToggleLyrics,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlayerSlider(
                    state = state,
                    activeAccent = activeAccent,
                    onSeekTo = viewModel::seekTo
                )

                Spacer(modifier = Modifier.height(4.dp))

                PlayerTransportControls(
                    state = state,
                    activeAccent = activeAccent,
                    viewModel = viewModel
                )
            }
        }
    } else {
        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .widthIn(max = 340.dp)
                    .aspectRatio(1f)
                    .shadow(24.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                MeloAsyncImage(
                    url = state.albumArt,
                    contentDescription = state.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onToggleLyrics() },
                    shape = RoundedCornerShape(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            TrackInfoBar(
                state = state,
                activeAccent = activeAccent,
                showLyrics = false,
                onToggleLyrics = onToggleLyrics,
                onToggleFavorite = onToggleFavorite,
                onArtistClick = onArtistClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlayerSlider(
                state = state,
                activeAccent = activeAccent,
                onSeekTo = viewModel::seekTo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            PlayerTransportControls(
                state = state,
                activeAccent = activeAccent,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.height(16.dp))

            LiveLyricsPreviewCard(
                trackLyrics = state.trackLyrics,
                activeLyricIndex = state.activeLyricIndex,
                showTranslation = state.showTranslation,
                activeAccent = activeAccent,
                onExpand = onToggleLyrics,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            NowPlayingMobileActions(
                activeAccent = activeAccent,
                state = state,
                onOpenQueue = onOpenQueue,
                onStartRadio = onStartRadio,
                onOpenArtist = onOpenArtist,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}