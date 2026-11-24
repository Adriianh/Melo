@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.player_tab_artist
import com.github.adriianh.melo.resources.player_tab_lyrics
import com.github.adriianh.melo.resources.player_tab_queue
import com.github.adriianh.melo.ui.components.controls.SegmentedControl
import com.github.adriianh.melo.ui.components.layout.GlassPanel
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.ui.player.PanelSection
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.PlayerUiState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NowPlayingDesktopLayout(
    modifier: Modifier = Modifier,
    state: PlayerUiState,
    activeAccent: Color,
    selectedSection: PanelSection,
    onSelectSection: (PanelSection) -> Unit,
    artistDetails: SearchResult.Artist?,
    onMoreClick: (Track, Boolean) -> Unit,
    onOpenContextMenu: ((Track, Boolean, SongMenuAnchor?) -> Unit)? = null,
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
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1.2f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(340.dp)
                        .shadow(28.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                MeloAsyncImage(
                    url = state.albumArt,
                    contentDescription = state.title,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clickable { onSelectSection(PanelSection.LYRICS) },
                    shape = RoundedCornerShape(22.dp),
                )
                if (state.isBuffering) {
                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = activeAccent)
                    }
                }
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
                modifier = Modifier.widthIn(max = 420.dp),
            )

            Spacer(modifier = Modifier.height(14.dp))

            PlayerSlider(
                state = state,
                activeAccent = activeAccent,
                onSeekTo = viewModel::seekTo,
                progressState = viewModel.progressState,
                modifier = Modifier.widthIn(max = 420.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))

            PlayerTransportControls(
                state = state,
                activeAccent = activeAccent,
                viewModel = viewModel,
            )
        }

        Box(
            modifier =
                Modifier
                    .width(440.dp)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
        ) {
            GlassPanel(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    val queueLabel = stringResource(Res.string.player_tab_queue)
                    val lyricsLabel = stringResource(Res.string.player_tab_lyrics)
                    val artistLabel = stringResource(Res.string.player_tab_artist)

                    SegmentedControl(
                        options = PanelSection.entries,
                        selected = selectedSection,
                        onSelect = { onSelectSection(it) },
                        label = {
                            when (it) {
                                PanelSection.QUEUE -> queueLabel
                                PanelSection.LYRICS -> lyricsLabel
                                PanelSection.ARTIST -> artistLabel
                            }
                        },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedSection) {
                            PanelSection.QUEUE ->
                                NowPlayingQueueSection(
                                    state = state,
                                    activeAccent = activeAccent,
                                    onMoreClick = onMoreClick,
                                    onOpenContextMenu = onOpenContextMenu,
                                    onSwipeQueueItem = onSwipeQueueItem,
                                    onSwipeSuggestionItem = onSwipeSuggestionItem,
                                    onPlayNextSuggestionItem = onPlayNextSuggestionItem,
                                    onSwipeRight = onSwipeRight,
                                    isLiked = isLiked,
                                )

                            PanelSection.LYRICS ->
                                IsolatedNowPlayingLyricsCard(
                                    state = state,
                                    activeAccent = activeAccent,
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize(),
                                )

                            PanelSection.ARTIST ->
                                NowPlayingArtistSection(
                                    state = state,
                                    artistDetails = artistDetails,
                                    activeAccent = activeAccent,
                                    onArtistClick = onArtistClick,
                                    onAlbumClick = onAlbumClick,
                                    onPlaylistClick = onPlaylistClick,
                                    onOpenTrackContextMenu = { track, anchor ->
                                        onOpenContextMenu?.invoke(track, false, anchor)
                                    },
                                )
                        }
                    }
                }
            }
        }
    }
}
