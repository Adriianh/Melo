package com.github.adriianh.melo.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.melo.ui.LocalSelectionMode
import com.github.adriianh.melo.ui.components.BatchSelectionBottomBar
import com.github.adriianh.melo.ui.components.TrackInteractionContextMenu
import com.github.adriianh.melo.ui.components.rememberTrackInteraction
import com.github.adriianh.melo.ui.library.components.AlbumsTabContent
import com.github.adriianh.melo.ui.library.components.ArtistsTabContent
import com.github.adriianh.melo.ui.library.components.DownloadsTabContent
import com.github.adriianh.melo.ui.library.components.HistoryTabContent
import com.github.adriianh.melo.ui.library.components.LibraryHeader
import com.github.adriianh.melo.ui.library.components.LibraryNotLoggedInCard
import com.github.adriianh.melo.ui.library.components.LikedSongsTabContent
import com.github.adriianh.melo.ui.library.components.LocalTabContent
import com.github.adriianh.melo.ui.library.components.PlaylistsTabContent
import com.github.adriianh.melo.ui.player.PlayerViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenSettings: () -> Unit = {},
    onAlbumClick: (id: String, title: String, artwork: String?, author: String) -> Unit = { _, _, _, _ -> },
    onPlaylistClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: LibraryViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val playerViewModel: PlayerViewModel = koinViewModel()
    val playerState by playerViewModel.uiState.collectAsState()

    val interaction = rememberTrackInteraction(viewModel, queueViewModel)
    val activeAccent = interaction.resolveActiveAccent(playerState)

    var selectedTrackIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedTrackIds.isNotEmpty()

    val selectionModeState = LocalSelectionMode.current
    LaunchedEffect(isSelectionMode) {
        selectionModeState.value = isSelectionMode
    }
    DisposableEffect(Unit) {
        onDispose {
            selectionModeState.value = false
        }
    }

    val visibleSongs = remember(
        state.selectedTab,
        state.likedSongs,
        state.history,
        state.localTracks,
        state.downloadedTracks
    ) {
        when (state.selectedTab) {
            LibraryTab.LIKED -> state.likedSongs
            LibraryTab.HISTORY -> state.history.map { it.track }
            LibraryTab.LOCAL -> state.localTracks
            LibraryTab.DOWNLOADS -> state.downloadedTracks.map { it.track }
            else -> emptyList()
        }
    }

    LaunchedEffect(state.selectedTab) {
        selectedTrackIds = emptySet()
    }

    TrackInteractionContextMenu(
        interaction = interaction,
        libraryState = state,
        activeAccent = activeAccent,
        onArtistClick = onArtistClick
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                top = 16.dp,
                start = 24.dp,
                end = 24.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LibraryHeader(
            profile = state.profile,
            selectedTab = state.selectedTab,
            onTabSelected = viewModel::selectTab,
            onOpenSettings = onOpenSettings,
            onRefresh = viewModel::refreshAll
        )

        val isOfflineTab =
            state.selectedTab == LibraryTab.DOWNLOADS ||
                    state.selectedTab == LibraryTab.LOCAL ||
                    state.selectedTab == LibraryTab.PLAYLISTS
        if (!state.isLoggedIn && !isOfflineTab) {
            LibraryNotLoggedInCard(onLoginClick = onOpenSettings)
        } else if (state.isLoading && !isOfflineTab) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                when (state.selectedTab) {
                    LibraryTab.PLAYLISTS -> PlaylistsTabContent(
                        customPlaylists = state.customPlaylists,
                        remotePlaylists = state.playlists,
                        onCreatePlaylist = viewModel::createPlaylist,
                        onRenamePlaylist = viewModel::renamePlaylist,
                        onDeletePlaylist = viewModel::deletePlaylist,
                        onPlaylistClick = onPlaylistClick
                    )

                    LibraryTab.LIKED -> LikedSongsTabContent(
                        songs = state.likedSongs,
                        onPlayTrack = { track ->
                            viewModel.playTrack(track, state.likedSongs)
                        },
                        onMoreClick = { interaction.openContextMenu(it) },
                        onSwipeLeft = { interaction.showAddedToQueueSnackbar(it, activeAccent) },
                        onSwipeRight = { track ->
                            viewModel.toggleLike(track.id, false)
                            interaction.snackbar.show(
                                msg = "Eliminada de tus Me Gusta",
                                vector = Icons.Default.HeartBroken,
                                action = "Deshacer",
                                actionColor = activeAccent,
                                onAction = {
                                    viewModel.toggleLike(track.id, true)
                                }
                            )
                        },
                        isSelectionMode = isSelectionMode,
                        selectedTrackIds = selectedTrackIds,
                        onToggleSelectTrack = { track ->
                            selectedTrackIds =
                                if (track.id in selectedTrackIds) selectedTrackIds - track.id else selectedTrackIds + track.id
                        },
                        onTrackLongClick = { track ->
                            if (!isSelectionMode) selectedTrackIds = setOf(track.id)
                        }
                    )

                    LibraryTab.DOWNLOADS -> {
                        val manualDownloads = state.downloadedTracks
                            .filter { it.downloadType == DownloadType.MANUAL }
                        val cachedDownloads = state.downloadedTracks
                            .filter { it.downloadType != DownloadType.MANUAL }
                        val prioritizedOfflineQueue =
                            manualDownloads.map { it.track } + cachedDownloads.map { it.track }

                        DownloadsTabContent(
                            downloadedTracks = manualDownloads,
                            onPlayTrack = { track ->
                                viewModel.playTrack(track, prioritizedOfflineQueue)
                            },
                            onPlayAll = {
                                if (prioritizedOfflineQueue.isNotEmpty()) {
                                    viewModel.playTrack(
                                        prioritizedOfflineQueue.first(),
                                        prioritizedOfflineQueue
                                    )
                                }
                            },
                            onDeleteDownload = viewModel::deleteDownloadedTrack,
                            onMoreClick = { interaction.openContextMenu(it) },
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick
                        )
                    }

                    LibraryTab.LOCAL -> LocalTabContent(
                        localTracks = state.localTracks,
                        localLibraryPaths = state.localLibraryPaths,
                        isScanning = state.isScanningLocal,
                        onRescan = viewModel::scanLocalTracks,
                        onAddFolder = viewModel::addLocalLibraryPath,
                        onRemoveFolder = viewModel::removeLocalLibraryPath,
                        onPlayTrack = { track ->
                            viewModel.playTrack(track, state.localTracks)
                        },
                        onPlayAll = {
                            if (state.localTracks.isNotEmpty()) {
                                viewModel.playTrack(state.localTracks.first(), state.localTracks)
                            }
                        },
                        onMoreClick = { interaction.openContextMenu(it) }
                    )

                    LibraryTab.ARTISTS -> ArtistsTabContent(
                        artists = state.artists,
                        onArtistClick = onArtistClick
                    )

                    LibraryTab.ALBUMS -> AlbumsTabContent(
                        albums = state.albums,
                        onAlbumClick = { id -> onAlbumClick(id, "", null, "") }
                    )

                    LibraryTab.HISTORY -> HistoryTabContent(
                        history = state.history,
                        isLiked = { track -> state.likedSongs.any { it.id == track.id } },
                        onPlayTrack = { entry ->
                            viewModel.playTrack(entry.track)
                        },
                        onMoreClick = { interaction.openContextMenu(it) },
                        onSwipeLeft = { interaction.showAddedToQueueSnackbar(it, activeAccent) },
                        onSwipeRight = { track ->
                            val trackIsLiked = state.likedSongs.any { it.id == track.id }
                            interaction.showToggledLikeSnackbar(track, trackIsLiked, activeAccent)
                        },
                        isSelectionMode = isSelectionMode,
                        selectedTrackIds = selectedTrackIds,
                        onToggleSelectTrack = { track ->
                            selectedTrackIds =
                                if (track.id in selectedTrackIds) selectedTrackIds - track.id else selectedTrackIds + track.id
                        },
                        onTrackLongClick = { track ->
                            if (!isSelectionMode) selectedTrackIds = setOf(track.id)
                        }
                    )
                }

                BatchSelectionBottomBar(
                    isVisible = isSelectionMode,
                    selectedCount = selectedTrackIds.size,
                    totalCount = visibleSongs.size,
                    onClearSelection = { selectedTrackIds = emptySet() },
                    onSelectAllToggle = {
                        selectedTrackIds =
                            if (selectedTrackIds.size == visibleSongs.size) emptySet() else visibleSongs.map { it.id }
                                .toSet()
                    },
                    onAddToPlaylist = {
                        val selTracks = visibleSongs.filter { it.id in selectedTrackIds }
                        interaction.openAddToPlaylist(selTracks)
                        selectedTrackIds = emptySet()
                    },
                    onAddToQueue = {
                        val selTracks = visibleSongs.filter { it.id in selectedTrackIds }
                        queueViewModel.addAllToQueue(selTracks)
                        interaction.snackbar.show(
                            msg = if (selTracks.size == 1) "1 canción añadida a la cola" else "${selTracks.size} canciones añadidas a la cola",
                            actionColor = activeAccent
                        )
                        selectedTrackIds = emptySet()
                    },
                    onDownload = {
                        val selTracks = visibleSongs.filter { it.id in selectedTrackIds }
                        selTracks.forEach { viewModel.downloadTrack(it) }
                        interaction.snackbar.show(
                            msg = "Descargando ${selTracks.size} canciones...",
                            actionColor = activeAccent
                        )
                        selectedTrackIds = emptySet()
                    },
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}