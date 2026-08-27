package com.github.adriianh.melo.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.MeloSnackbarHost
import com.github.adriianh.melo.ui.components.TrackContextMenu
import com.github.adriianh.melo.ui.components.rememberTrackInteraction
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.components.NowPlayingArtistSection
import com.github.adriianh.melo.ui.player.components.NowPlayingDesktopLayout
import com.github.adriianh.melo.ui.player.components.NowPlayingMobileLayout
import com.github.adriianh.melo.ui.player.components.NowPlayingQueueSection
import com.github.adriianh.melo.ui.player.components.NowPlayingTopBar
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.getPlatform
import org.koin.compose.viewmodel.koinViewModel

enum class PanelSection {
    QUEUE,
    LYRICS,
    ARTIST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onCollapse: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val artistDetails by viewModel.artistDetails.collectAsState()
    val platform = remember { getPlatform() }
    var selectedSection by remember { mutableStateOf(PanelSection.QUEUE) }
    var showLyrics by remember { mutableStateOf(false) }
    var expandedBottomSection by remember { mutableStateOf<PanelSection?>(null) }

    val interaction = rememberTrackInteraction(libraryViewModel, queueViewModel)
    val activeAccent = interaction.resolveActiveAccent(state)

    var contextMenuInQueue by remember { mutableStateOf(false) }

    if (interaction.contextMenuTrack != null) {
        val track = interaction.contextMenuTrack!!
        val isLiked = libraryState.likedSongs.any { it.id == track.id }
        TrackContextMenu(
            track = track,
            onDismissRequest = interaction::dismissContextMenu,
            onPlayNext = {
                interaction.showPlayNextSnackbar(track, activeAccent)
                interaction.contextMenuTrack = null
            },
            onAddToQueue = {
                interaction.showAddedToQueueSnackbar(track, activeAccent)
                interaction.contextMenuTrack = null
            },
            onRemoveFromQueue = {
                queueViewModel.removeTrackFromQueue(track)
                interaction.contextMenuTrack = null
            },
            inQueue = contextMenuInQueue,
            onToggleLike = {
                interaction.showToggledLikeSnackbar(track, isLiked, activeAccent)
                interaction.contextMenuTrack = null
            },
            isLiked = isLiked,
            onAddToPlaylist = { /* TODO */ },
            onGoToArtist = {
                interaction.contextMenuTrack = null
                onArtistClick(track.artist)
            },
            onGoToAlbum = {
                interaction.contextMenuTrack = null
            },
            onShare = { /* TODO */ }
        )
    }

    val onMoreClick: (Track, Boolean) -> Unit = { track, inQueue ->
        interaction.openContextMenu(track)
        contextMenuInQueue = inQueue
    }
    val onSwipeQueueItem: (Int) -> Unit = { trackIndex ->
        val removedTrack = queueViewModel.removeTrackAt(trackIndex)
        if (removedTrack != null) {
            interaction.snackbar.show(
                msg = "Eliminada de la cola",
                vector = Icons.Default.Delete,
                scope = interaction.scope,
                action = "Deshacer",
                actionColor = activeAccent,
                onAction = { queueViewModel.insertTrackAt(removedTrack, trackIndex) }
            )
        }
    }
    val onSwipeSuggestionItem: (Track) -> Unit = { track ->
        interaction.showAddedToQueueSnackbar(track, activeAccent)
    }
    val onSwipeRight: (Track) -> Unit = { track ->
        val trackIsLiked = libraryState.likedSongs.any { it.id == track.id }
        interaction.showToggledLikeSnackbar(track, trackIsLiked, activeAccent)
    }
    val isLiked: (Track) -> Boolean = { track -> libraryState.likedSongs.any { it.id == track.id } }

    Box(modifier = modifier.fillMaxSize()) {
        NowPlayingBackground(artworkUrl = state.albumArt)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            NowPlayingTopBar(
                title = state.title,
                onCollapse = onCollapse
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (platform.type == PlatformType.DESKTOP) {
                NowPlayingDesktopLayout(
                    state = state,
                    activeAccent = activeAccent,
                    showLyrics = showLyrics,
                    onToggleLyrics = { showLyrics = !showLyrics },
                    selectedSection = selectedSection,
                    onSelectSection = { selectedSection = it },
                    artistDetails = artistDetails,
                    onMoreClick = onMoreClick,
                    onSwipeQueueItem = onSwipeQueueItem,
                    onSwipeSuggestionItem = onSwipeSuggestionItem,
                    onPlayNextSuggestionItem = { track ->
                        interaction.showPlayNextSnackbar(
                            track,
                            activeAccent
                        )
                    },
                    onSwipeRight = onSwipeRight,
                    isLiked = isLiked,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onArtistClick = { id -> onCollapse(); onArtistClick(id) },
                    onAlbumClick = { id -> onCollapse(); onAlbumClick(id) },
                    onPlaylistClick = { id -> onCollapse(); onPlaylistClick(id) },
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            } else {
                NowPlayingMobileLayout(
                    state = state,
                    activeAccent = activeAccent,
                    showLyrics = showLyrics,
                    onToggleLyrics = { showLyrics = !showLyrics },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onArtistClick = {
                        artistDetails?.id?.let { id ->
                            onCollapse()
                            onArtistClick(id)
                        }
                    },
                    onOpenQueue = { expandedBottomSection = PanelSection.QUEUE },
                    onStartRadio = { track -> queueViewModel.startRadio(track) },
                    onOpenArtist = { expandedBottomSection = PanelSection.ARTIST },
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (expandedBottomSection != null) {
            ModalBottomSheet(
                onDismissRequest = { expandedBottomSection = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                containerColor = MeloColors.surface1.copy(alpha = 0.95f),
                contentColor = MeloColors.textPrimary,
                scrimColor = Color.Black.copy(alpha = 0.55f),
                tonalElevation = 12.dp,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MeloColors.textMuted.copy(alpha = 0.4f))
                    )
                }
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp)
                            .fillMaxHeight(0.70f)
                    ) {
                        Text(
                            text = when (expandedBottomSection) {
                                PanelSection.QUEUE -> "Cola de reproducción"
                                PanelSection.ARTIST -> "Acerca del Artista"
                                else -> ""
                            },
                            style = MeloType.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MeloColors.textPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        when (expandedBottomSection) {
                            PanelSection.QUEUE -> NowPlayingQueueSection(
                                state = state,
                                activeAccent = activeAccent,
                                onMoreClick = onMoreClick,
                                onSwipeQueueItem = onSwipeQueueItem,
                                onSwipeSuggestionItem = onSwipeSuggestionItem,
                                onPlayNextSuggestionItem = { track ->
                                    interaction.showPlayNextSnackbar(
                                        track,
                                        activeAccent
                                    )
                                },
                                onSwipeRight = onSwipeRight,
                                isLiked = isLiked
                            )

                            PanelSection.ARTIST -> NowPlayingArtistSection(
                                state = state,
                                artistDetails = artistDetails,
                                activeAccent = activeAccent,
                                onArtistClick = { id ->
                                    expandedBottomSection = null
                                    onCollapse()
                                    onArtistClick(id)
                                },
                                onAlbumClick = { id ->
                                    expandedBottomSection = null
                                    onCollapse()
                                    onAlbumClick(id)
                                },
                                onPlaylistClick = { id ->
                                    expandedBottomSection = null
                                    onCollapse()
                                    onPlaylistClick(id)
                                }
                            )

                            else -> {}
                        }
                    }
                    MeloSnackbarHost(
                        state = interaction.snackbar,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        bottomPadding = 24.dp
                    )
                }
            }
        }

        if (expandedBottomSection == null) {
            MeloSnackbarHost(
                state = interaction.snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
                bottomPadding = 32.dp
            )
        }
    }
}