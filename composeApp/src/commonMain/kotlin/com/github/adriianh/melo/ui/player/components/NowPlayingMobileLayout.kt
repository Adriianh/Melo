package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(230.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = showLyrics,
                label = "MobileLyricsCrossfade"
            ) { isLyrics ->
                if (isLyrics) {
                    NowPlayingLyricsCard(
                        lyrics = state.lyrics,
                        activeAccent = activeAccent,
                        onToggleArtwork = onToggleLyrics,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MeloAsyncImage(
                        url = state.albumArt,
                        contentDescription = state.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onToggleLyrics() },
                        size = 230.dp,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TrackInfoBar(
            state = state,
            activeAccent = activeAccent,
            showLyrics = showLyrics,
            onToggleLyrics = onToggleLyrics,
            onToggleFavorite = onToggleFavorite,
            onArtistClick = onArtistClick,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )

        PlayerSlider(
            state = state,
            activeAccent = activeAccent,
            onSeekTo = viewModel::seekTo
        )

        PlayerTransportControls(
            state = state,
            activeAccent = activeAccent,
            viewModel = viewModel
        )

        Spacer(modifier = Modifier.height(6.dp))

        NowPlayingMobileActions(
            activeAccent = activeAccent,
            state = state,
            onOpenQueue = onOpenQueue,
            onStartRadio = onStartRadio,
            onOpenArtist = onOpenArtist,
            modifier = Modifier.fillMaxWidth()
        )
    }
}