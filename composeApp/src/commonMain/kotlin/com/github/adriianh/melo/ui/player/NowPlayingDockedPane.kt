@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.a11y_close_panel
import com.github.adriianh.melo.resources.a11y_expand_fullscreen
import com.github.adriianh.melo.resources.a11y_favorite
import com.github.adriianh.melo.resources.a11y_start_radio
import com.github.adriianh.melo.resources.player_header_now_playing
import com.github.adriianh.melo.resources.player_tab_artist
import com.github.adriianh.melo.resources.player_tab_lyrics
import com.github.adriianh.melo.resources.player_tab_queue
import com.github.adriianh.melo.ui.components.cards.SuggestionTrackCard
import com.github.adriianh.melo.ui.components.controls.SegmentedControl
import com.github.adriianh.melo.ui.components.track.TrackInteractionContextMenu
import com.github.adriianh.melo.ui.components.track.rememberTrackInteraction
import com.github.adriianh.melo.ui.library.LibraryUiState
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.components.DockedArtistContent
import com.github.adriianh.melo.ui.player.components.DockedLyricsContent
import com.github.adriianh.melo.ui.player.components.DockedQueueContent
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerUiState
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DesktopNowPlayingDockedPane(
    modifier: Modifier = Modifier,
    onExpandToFullscreen: () -> Unit,
    onClose: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    selectedSection: PanelSection = PanelSection.QUEUE,
    onSectionChange: (PanelSection) -> Unit = {},
    viewModel: PlayerViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.stableUiState.collectAsState()
    val artistDetails by viewModel.artistDetails.collectAsState()
    val interaction = rememberTrackInteraction(libraryViewModel, queueViewModel)
    val activeAccent = interaction.resolveActiveAccent(MaterialTheme.colorScheme.primary)
    var contextMenuInQueue by remember { mutableStateOf(false) }

    val isLibraryNeeded =
        interaction.contextMenuTrack != null || !interaction.addToPlaylistTracks.isNullOrEmpty()
    val libraryState by remember(isLibraryNeeded) {
        if (isLibraryNeeded) libraryViewModel.uiState else flowOf(LibraryUiState())
    }.collectAsState(initial = LibraryUiState())

    TrackInteractionContextMenu(
        interaction = interaction,
        libraryState = libraryState,
        activeAccent = activeAccent,
        onArtistClick = onArtistClick,
        onAlbumWithArtistClick = { album, _ -> onAlbumClick(album) },
        inQueue = contextMenuInQueue,
    )

    DesktopNowPlayingDockedPaneContent(
        state = state,
        artistDetails = artistDetails,
        modifier = modifier,
        selectedSection = selectedSection,
        onSectionChange = onSectionChange,
        onExpandToFullscreen = onExpandToFullscreen,
        onClose = onClose,
        onArtistClick = onArtistClick,
        onStartRadio = { state.currentTrack?.let(queueViewModel::startRadio) },
        onToggleFavorite = { viewModel.toggleFavorite() },
        queueContent = { accent ->
            DockedQueueContent(
                state = state,
                activeAccent = accent,
                queueViewModel = queueViewModel,
                onOpenContextMenu = { track, inQueue, anchor ->
                    contextMenuInQueue = inQueue
                    interaction.openContextMenu(track, anchor)
                },
            )
        },
        lyricsContent = { accent -> DockedLyricsContent(state, accent, viewModel) },
        artistContent = { accent ->
            DockedArtistContent(
                state = state,
                artistDetails = artistDetails,
                activeAccent = accent,
                onArtistClick = onArtistClick,
                onAlbumClick = onAlbumClick,
                queueViewModel = queueViewModel,
                onOpenTrackContextMenu = { track, anchor ->
                    contextMenuInQueue = false
                    interaction.openContextMenu(track, anchor)
                },
            )
        },
    )
}

@Composable
fun DesktopNowPlayingDockedPaneContent(
    modifier: Modifier = Modifier,
    state: PlayerUiState,
    artistDetails: SearchResult.Artist?,
    selectedSection: PanelSection = PanelSection.QUEUE,
    onSectionChange: (PanelSection) -> Unit = {},
    onExpandToFullscreen: () -> Unit = {},
    onClose: () -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onStartRadio: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    queueContent: @Composable (Color) -> Unit = {},
    lyricsContent: @Composable (Color) -> Unit = {},
    artistContent: @Composable (Color) -> Unit = {},
) {
    val activeAccent = MaterialTheme.colorScheme.primary

    if (!state.hasTrack) return

    Column(
        modifier =
            modifier
                .width(290.dp)
                .fillMaxHeight()
                .padding(top = 12.dp, bottom = 12.dp, end = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MeloColors.glassSurface)
                .border(0.5.dp, MeloColors.glassBorder, RoundedCornerShape(20.dp))
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DockedPaneHeader(
            onExpandToFullscreen = onExpandToFullscreen,
            onClose = onClose,
        )

        MeloAsyncImage(
            url = state.albumArt,
            contentDescription = state.title,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .shadow(10.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
            size = 160.dp,
            shape = RoundedCornerShape(12.dp),
        )

        DockedPaneTrackInfo(
            state = state,
            activeAccent = activeAccent,
            onArtistClick = onArtistClick,
            artistId = artistDetails?.id,
            onStartRadio = onStartRadio,
            onToggleFavorite = onToggleFavorite,
        )

        val queueLabel = stringResource(Res.string.player_tab_queue)
        val lyricsLabel = stringResource(Res.string.player_tab_lyrics)
        val artistLabel = stringResource(Res.string.player_tab_artist)

        SegmentedControl(
            options = PanelSection.entries,
            selected = selectedSection,
            onSelect = onSectionChange,
            label = {
                when (it) {
                    PanelSection.QUEUE -> queueLabel
                    PanelSection.LYRICS -> lyricsLabel
                    PanelSection.ARTIST -> artistLabel
                }
            },
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            when (selectedSection) {
                PanelSection.QUEUE -> queueContent(activeAccent)
                PanelSection.LYRICS -> lyricsContent(activeAccent)
                PanelSection.ARTIST -> artistContent(activeAccent)
            }
        }
    }
}

@Composable
private fun DockedPaneHeader(
    onExpandToFullscreen: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.player_header_now_playing),
            style = MeloType.labelSmall,
            letterSpacing = 1.2.sp,
            color = MeloColors.textMuted,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExpandToFullscreen, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.OpenInFull,
                    contentDescription = stringResource(Res.string.a11y_expand_fullscreen),
                    tint = MeloColors.textPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(Res.string.a11y_close_panel),
                    tint = MeloColors.textMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun DockedPaneTrackInfo(
    state: PlayerUiState,
    activeAccent: Color,
    onArtistClick: (String) -> Unit,
    artistId: String?,
    onStartRadio: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title,
                style = MeloType.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MeloColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.artist,
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.clickable {
                        artistId?.let(onArtistClick)
                    },
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onStartRadio,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = stringResource(Res.string.a11y_start_radio),
                    tint = MeloColors.textMuted,
                    modifier = Modifier.size(18.dp),
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(Res.string.a11y_favorite),
                    tint = if (state.isFavorite) activeAccent else MeloColors.textMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SimulatedDockedQueueContent(activeAccent: Color) {
    val upNextTrack =
        Track(
            id = "q1",
            title = "Instant Crush",
            artist = "Daft Punk feat. Julian Casablancas",
            album = "Random Access Memories",
            durationMs = 337000L,
            genres = emptyList(),
            artworkUrl = null,
            sourceId = null,
        )
    val suggestionTrack =
        Track(
            id = "s1",
            title = "Starboy",
            artist = "The Weeknd feat. Daft Punk",
            album = "Starboy",
            durationMs = 230000L,
            genres = emptyList(),
            artworkUrl = null,
            sourceId = null,
        )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Text(
                text = "UP NEXT",
                style = MeloType.labelSmall,
                fontWeight = FontWeight.Bold,
                color = activeAccent,
            )
            Spacer(modifier = Modifier.height(4.dp))
            SuggestionTrackCard(
                track = upNextTrack,
                activeAccent = activeAccent,
                onClick = {},
                onAddClick = {},
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SUGGESTIONS",
                style = MeloType.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeloColors.textSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            SuggestionTrackCard(
                track = suggestionTrack,
                activeAccent = activeAccent,
                onClick = {},
                onAddClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDockedPaneHeader() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .width(290.dp)
                    .background(MeloColors.glassSurface)
                    .padding(14.dp),
        ) {
            DockedPaneHeader(
                onExpandToFullscreen = {},
                onClose = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDockedPaneTrackInfo() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .width(290.dp)
                    .background(MeloColors.glassSurface)
                    .padding(14.dp),
        ) {
            DockedPaneTrackInfo(
                state =
                    PlayerUiState(
                        title = "Midnight City",
                        artist = "M83",
                        isFavorite = true,
                        elapsedLabel = "2:15",
                        remainingLabel = "1:45",
                        progressFraction = 0.56f,
                    ),
                activeAccent = MeloColors.brandAccent,
                onArtistClick = {},
                artistId = "artist_m83",
                onStartRadio = {},
                onToggleFavorite = {},
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDesktopNowPlayingDockedPaneSimulatedQueuePink() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .height(650.dp)
                    .background(MeloColors.surface1)
                    .padding(16.dp),
        ) {
            DesktopNowPlayingDockedPaneContent(
                state =
                    PlayerUiState(
                        title = "Get Lucky",
                        artist = "Daft Punk feat. Pharrell Williams",
                        albumArt = "https://example.com/art.jpg",
                        isFavorite = true,
                        accentColor = Color(0xFFEC407A), // Dynamic vibrant pink
                        elapsedLabel = "1:20",
                        remainingLabel = "2:48",
                        progressFraction = 0.32f,
                    ),
                artistDetails = null,
                selectedSection = PanelSection.QUEUE,
                queueContent = { activeAccent ->
                    SimulatedDockedQueueContent(activeAccent)
                },
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDesktopNowPlayingDockedPaneSimulatedQueueCyan() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .height(650.dp)
                    .background(MeloColors.surface1)
                    .padding(16.dp),
        ) {
            DesktopNowPlayingDockedPaneContent(
                state =
                    PlayerUiState(
                        title = "Starboy",
                        artist = "The Weeknd feat. Daft Punk",
                        albumArt = "https://example.com/starboy.jpg",
                        isFavorite = false,
                        accentColor = Color(0xFF00E5FF), // Dynamic electric cyan
                        elapsedLabel = "2:10",
                        remainingLabel = "1:40",
                        progressFraction = 0.56f,
                    ),
                artistDetails = null,
                selectedSection = PanelSection.QUEUE,
                queueContent = { activeAccent ->
                    SimulatedDockedQueueContent(activeAccent)
                },
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDesktopNowPlayingDockedPaneLyrics() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .height(650.dp)
                    .background(MeloColors.surface1)
                    .padding(16.dp),
        ) {
            DesktopNowPlayingDockedPaneContent(
                state =
                    PlayerUiState(
                        title = "Starboy",
                        artist = "The Weeknd",
                        albumArt = "https://example.com/starboy.jpg",
                        isFavorite = false,
                        accentColor = Color(0xFFFFB300), // Dynamic amber
                        elapsedLabel = "2:10",
                        remainingLabel = "1:40",
                        progressFraction = 0.56f,
                        lyrics = "I'm tryna put you in the worst mood, ah\nP1 cleaner than your church shoes, ah",
                    ),
                artistDetails = null,
                selectedSection = PanelSection.LYRICS,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewDesktopNowPlayingDockedPaneArtist() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .height(650.dp)
                    .background(MeloColors.surface1)
                    .padding(16.dp),
        ) {
            DesktopNowPlayingDockedPaneContent(
                state =
                    PlayerUiState(
                        title = "Midnight City",
                        artist = "M83",
                        albumArt = "https://example.com/m83.jpg",
                        isFavorite = true,
                        accentColor = Color(0xFFAB47BC), // Dynamic purple
                        elapsedLabel = "3:00",
                        remainingLabel = "1:03",
                        progressFraction = 0.75f,
                    ),
                artistDetails = null,
                selectedSection = PanelSection.ARTIST,
            )
        }
    }
}
