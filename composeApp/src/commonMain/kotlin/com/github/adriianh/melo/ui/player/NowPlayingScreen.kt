package com.github.adriianh.melo.ui.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.NowPlayingBackdropScrim
import com.github.adriianh.core.domain.model.NowPlayingBlurIntensity
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.common_action_undo
import com.github.adriianh.melo.resources.track_snackbar_added_to_likes
import com.github.adriianh.melo.resources.track_snackbar_added_to_queue
import com.github.adriianh.melo.resources.track_snackbar_play_next
import com.github.adriianh.melo.resources.track_snackbar_removed_from_likes
import com.github.adriianh.melo.resources.track_snackbar_removed_from_queue
import com.github.adriianh.melo.ui.components.AddToPlaylistSheet
import com.github.adriianh.melo.ui.components.MeloSnackbarState
import com.github.adriianh.melo.ui.components.TrackContextMenu
import com.github.adriianh.melo.ui.components.rememberTrackInteraction
import com.github.adriianh.melo.ui.i18n.UiText
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.components.ExpandedSectionSheet
import com.github.adriianh.melo.ui.player.components.NowPlayingDesktopLayout
import com.github.adriianh.melo.ui.player.components.NowPlayingMobileLayout
import com.github.adriianh.melo.ui.player.components.NowPlayingTopBar
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.getPlatform
import org.koin.compose.viewmodel.koinViewModel

enum class PanelSection {
    QUEUE,
    LYRICS,
    ARTIST,
}

@Composable
fun NowPlayingScreen(
    modifier: Modifier = Modifier,
    onCollapse: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    titleBar: @Composable () -> Unit = {},
    blurEnabled: Boolean = true,
    blurIntensity: NowPlayingBlurIntensity = NowPlayingBlurIntensity.MEDIUM,
    backdropScrim: NowPlayingBackdropScrim = NowPlayingBackdropScrim.NORMAL,
    viewModel: PlayerViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.stableUiState.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val artistDetails by viewModel.artistDetails.collectAsState()
    val platform = remember { getPlatform() }
    var selectedSection by remember { mutableStateOf(PanelSection.QUEUE) }
    var showLyrics by remember { mutableStateOf(false) }
    var expandedBottomSection by remember { mutableStateOf<PanelSection?>(null) }

    val interaction = rememberTrackInteraction(libraryViewModel, queueViewModel)
    val activeAccent = MaterialTheme.colorScheme.primary

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    val sheetSnackbarState = remember { MeloSnackbarState() }
    var contextMenuInQueue by remember { mutableStateOf(false) }

    val isTrackLiked: (Track) -> Boolean =
        remember(libraryState.likedSongs) {
            { track ->
                val rawId = track.sourceId ?: track.id.removePrefix("piped:")
                libraryState.likedSongs.any {
                    it.id == track.id ||
                        (!it.sourceId.isNullOrBlank() && it.sourceId == track.sourceId) ||
                        (rawId.isNotBlank() && (it.id.removePrefix("piped:") == rawId || it.sourceId == rawId))
                }
            }
        }

    val currentTrackIsLiked =
        remember(state.currentTrack, state.isFavorite, libraryState.likedSongs) {
            val track = state.currentTrack
            track != null && (state.isFavorite || isTrackLiked(track))
        }
    val effectiveState =
        remember(state, currentTrackIsLiked) {
            state.copy(isFavorite = currentTrackIsLiked)
        }

    if (interaction.contextMenuTrack != null) {
        val track = interaction.contextMenuTrack!!
        val isLiked = isTrackLiked(track)
        val isDownloaded =
            libraryState.downloadedTracks.any { it.track.id == track.id && it.downloadType == DownloadType.MANUAL }
        val isDownloading = libraryState.activeDownloads.containsKey(track.id)
        val downloadProgress = libraryState.activeDownloads[track.id]
        val targetSnackbar =
            if (contextMenuInQueue && expandedBottomSection != null) sheetSnackbarState else interaction.snackbar

        TrackContextMenu(
            track = track,
            onDismissRequest = interaction::dismissContextMenu,
            onPlayNext = {
                val insertIndex = queueViewModel.queueState.value.currentIndex + 1
                queueViewModel.insertTrackNext(track)
                targetSnackbar.show(
                    msg = UiText.Resource(Res.string.track_snackbar_play_next),
                    vector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    action = UiText.Resource(Res.string.common_action_undo),
                    actionColor = activeAccent,
                    onAction = { queueViewModel.removeTrackAt(insertIndex) },
                )
                interaction.contextMenuTrack = null
            },
            onAddToQueue = {
                queueViewModel.addToQueue(track)
                targetSnackbar.show(
                    msg = UiText.Resource(Res.string.track_snackbar_added_to_queue),
                    vector = Icons.AutoMirrored.Filled.QueueMusic,
                    action = UiText.Resource(Res.string.common_action_undo),
                    actionColor = activeAccent,
                    onAction = { queueViewModel.removeTrackFromQueue(track) },
                )
                interaction.contextMenuTrack = null
            },
            onRemoveFromQueue = {
                val removedIndex = queueViewModel.removeTrackFromQueue(track)
                if (removedIndex != -1) {
                    targetSnackbar.show(
                        msg = UiText.Resource(Res.string.track_snackbar_removed_from_queue),
                        vector = Icons.Default.Delete,
                        action = UiText.Resource(Res.string.common_action_undo),
                        actionColor = activeAccent,
                        onAction = { queueViewModel.insertTrackAt(track, removedIndex) },
                    )
                }
                interaction.contextMenuTrack = null
            },
            inQueue = contextMenuInQueue,
            onToggleLike = {
                libraryViewModel.toggleLike(track.id, !isLiked)
                targetSnackbar.show(
                    msg =
                        UiText.Resource(
                            if (isLiked) {
                                Res.string.track_snackbar_removed_from_likes
                            } else {
                                Res.string.track_snackbar_added_to_likes
                            },
                        ),
                    vector = if (isLiked) Icons.Default.HeartBroken else Icons.Default.Favorite,
                    action = UiText.Resource(Res.string.common_action_undo),
                    actionColor = activeAccent,
                    onAction = { libraryViewModel.toggleLike(track.id, isLiked) },
                )
                interaction.contextMenuTrack = null
            },
            isLiked = isLiked,
            onAddToPlaylist = {
                interaction.openAddToPlaylist(track)
                interaction.dismissContextMenu()
            },
            onGoToArtist = {
                interaction.contextMenuTrack = null
                onArtistClick(track.artist)
            },
            onGoToAlbum = {
                interaction.contextMenuTrack = null
            },
            onShare = { /* TODO */ },
            onDownload = {
                interaction.downloadTrack(track, activeAccent)
                interaction.contextMenuTrack = null
            },
            onDeleteDownload = {
                interaction.deleteDownloadedTrack(track.id, activeAccent)
                interaction.contextMenuTrack = null
            },
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
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
                msg = UiText.Resource(Res.string.track_snackbar_removed_from_queue),
                vector = Icons.Default.Delete,
                action = UiText.Resource(Res.string.common_action_undo),
                actionColor = activeAccent,
                onAction = { queueViewModel.insertTrackAt(removedTrack, trackIndex) },
            )
        }
    }
    val onSwipeSuggestionItem: (Track) -> Unit = { track ->
        interaction.showAddedToQueueSnackbar(track, activeAccent)
    }
    val onSwipeRight: (Track) -> Unit = { track ->
        val trackIsLiked = isTrackLiked(track)
        interaction.showToggledLikeSnackbar(track, trackIsLiked, activeAccent)
    }
    val isLiked: (Track) -> Boolean = isTrackLiked

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { }
                },
    ) {
        NowPlayingBackground(
            artworkUrl = effectiveState.albumArt,
            blurEnabled = blurEnabled,
            blurIntensity = blurIntensity,
            backdropScrim = backdropScrim,
            accentColor = activeAccent,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.Top,
        ) {
            titleBar()

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            horizontal = if (platform.type == PlatformType.DESKTOP) 24.dp else 16.dp,
                            vertical = 8.dp,
                        ),
            ) {
                NowPlayingTopBar(
                    title = effectiveState.title,
                    onCollapse = onCollapse,
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (platform.type == PlatformType.DESKTOP) {
                    NowPlayingDesktopLayout(
                        modifier = Modifier.weight(1f),
                        state = effectiveState,
                        activeAccent = activeAccent,
                        selectedSection = selectedSection,
                        onSelectSection = { selectedSection = it },
                        artistDetails = artistDetails,
                        onMoreClick = onMoreClick,
                        onSwipeQueueItem = onSwipeQueueItem,
                        onSwipeSuggestionItem = onSwipeSuggestionItem,
                        onPlayNextSuggestionItem = { track ->
                            interaction.showPlayNextSnackbar(
                                track,
                                activeAccent,
                            )
                        },
                        onSwipeRight = onSwipeRight,
                        isLiked = isLiked,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onArtistClick = { id ->
                            onCollapse()
                            onArtistClick(id)
                        },
                        onAlbumClick = { id ->
                            onCollapse()
                            onAlbumClick(id)
                        },
                        onPlaylistClick = { id ->
                            onCollapse()
                            onPlaylistClick(id)
                        },
                        viewModel = viewModel,
                    )
                } else {
                    NowPlayingMobileLayout(
                        state = effectiveState,
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
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        ExpandedSectionSheet(
            section = expandedBottomSection,
            state = state,
            artistDetails = artistDetails,
            activeAccent = activeAccent,
            snackbarState = sheetSnackbarState,
            onDismiss = { expandedBottomSection = null },
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
            },
            onMoreClick = onMoreClick,
            isLiked = isLiked,
        )

        val playlistTrack = interaction.addToPlaylistTrack
        if (playlistTrack != null) {
            val containingPlaylistIds by remember(playlistTrack.id) {
                libraryViewModel.getPlaylistIdsForTrack(playlistTrack.id)
            }.collectAsState(initial = emptySet())

            AddToPlaylistSheet(
                track = playlistTrack,
                playlists = libraryState.customPlaylists,
                containingPlaylistIds = containingPlaylistIds,
                onDismissRequest = interaction::dismissAddToPlaylist,
                onSelectPlaylist = { playlist ->
                    libraryViewModel.addTrackToPlaylist(playlist.id, playlistTrack) {
                        interaction.showAddedToPlaylistSnackbar(
                            playlist.name,
                            activeAccent,
                        )
                    }
                },
                onCreatePlaylistAndAdd = { name ->
                    libraryViewModel.createPlaylist(name) { id ->
                        libraryViewModel.addTrackToPlaylist(id, playlistTrack) {
                            interaction.showAddedToPlaylistSnackbar(
                                name,
                                activeAccent,
                            )
                        }
                    }
                },
            )
        }
    }
}
