package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.GlassPanel
import com.github.adriianh.melo.ui.components.SegmentedControl
import com.github.adriianh.melo.ui.player.PanelSection
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.PlayerUiState

@Composable
internal fun NowPlayingDesktopLayout(
    state: PlayerUiState,
    activeAccent: Color,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
    selectedSection: PanelSection,
    onSelectSection: (PanelSection) -> Unit,
    artistDetails: SearchResult.Artist?,
    onMoreClick: (Track, Boolean) -> Unit,
    onSwipeQueueItem: (Int) -> Unit,
    onSwipeSuggestionItem: (Track) -> Unit,
    onPlayNextSuggestionItem: (Track) -> Unit = {},
    onSwipeRight: (Track) -> Unit,
    isLiked: (Track) -> Boolean,
    onToggleFavorite: () -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1.2f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .shadow(28.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                MeloAsyncImage(
                    url = state.albumArt,
                    contentDescription = state.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onSelectSection(PanelSection.LYRICS) },
                    shape = RoundedCornerShape(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            TrackInfoBar(
                state = state,
                activeAccent = activeAccent,
                showLyrics = selectedSection == PanelSection.LYRICS,
                onToggleLyrics = {
                    if (selectedSection == PanelSection.LYRICS) {
                        onSelectSection(PanelSection.QUEUE)
                    } else {
                        onSelectSection(PanelSection.LYRICS)
                    }
                },
                onToggleFavorite = onToggleFavorite,
                onArtistClick = { state.currentTrack?.artist?.let { onArtistClick(it) } },
                modifier = Modifier.widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            PlayerSlider(
                state = state,
                activeAccent = activeAccent,
                onSeekTo = viewModel::seekTo,
                modifier = Modifier.widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            PlayerTransportControls(
                state = state,
                activeAccent = activeAccent,
                viewModel = viewModel
            )
        }

        Box(
            modifier = Modifier
                .width(440.dp)
                .fillMaxHeight()
                .padding(vertical = 8.dp)
        ) {
            GlassPanel(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    SegmentedControl(
                        options = PanelSection.entries,
                        selected = selectedSection,
                        onSelect = { onSelectSection(it) },
                        label = {
                            when (it) {
                                PanelSection.QUEUE -> "Cola"
                                PanelSection.LYRICS -> "Letra"
                                PanelSection.ARTIST -> "Artista"
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedSection) {
                            PanelSection.QUEUE -> NowPlayingQueueSection(
                                state = state,
                                activeAccent = activeAccent,
                                onMoreClick = onMoreClick,
                                onSwipeQueueItem = onSwipeQueueItem,
                                onSwipeSuggestionItem = onSwipeSuggestionItem,
                                onPlayNextSuggestionItem = onPlayNextSuggestionItem,
                                onSwipeRight = onSwipeRight,
                                isLiked = isLiked
                            )

                            PanelSection.LYRICS -> NowPlayingLyricsCard(
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

                            PanelSection.ARTIST -> NowPlayingArtistSection(
                                state = state,
                                artistDetails = artistDetails,
                                activeAccent = activeAccent,
                                onArtistClick = onArtistClick,
                                onAlbumClick = onAlbumClick,
                                onPlaylistClick = onPlaylistClick
                            )
                        }
                    }
                }
            }
        }
    }
}