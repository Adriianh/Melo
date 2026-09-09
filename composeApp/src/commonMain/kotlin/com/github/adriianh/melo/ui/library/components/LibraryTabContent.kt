package com.github.adriianh.melo.ui.library.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.BatchSelectionBottomBar
import com.github.adriianh.melo.ui.components.TrackInteractionState
import com.github.adriianh.melo.ui.library.LibraryFilters
import com.github.adriianh.melo.ui.library.LibraryTab
import com.github.adriianh.melo.ui.library.LibraryUiState
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel

@Composable
fun LibraryTabContent(
    tab: LibraryTab,
    state: LibraryUiState,
    filters: LibraryFilters,
    activeAccent: Color,
    isSelectionMode: Boolean,
    selectedTrackIds: Set<String>,
    interaction: TrackInteractionState,
    onPlaylistClick: (String, String, String?, String) -> Unit,
    onAlbumClick: (id: String, title: String, artwork: String?, author: String) -> Unit,
    onArtistClick: (String) -> Unit,
    onToggleSelectTrack: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAllToggle: (List<Track>) -> Unit,
    viewModel: LibraryViewModel,
    queueViewModel: QueueViewModel,
    modifier: Modifier = Modifier,
) {
    val likedTrackIds = remember(state.likedSongs) {
        state.likedSongs.map { it.id }.toSet()
    }
    val visibleSongs = when (tab) {
        LibraryTab.LIKED -> filters.likedSongs
        LibraryTab.HISTORY -> filters.history.map { it.track }
        LibraryTab.LOCAL -> filters.localTracks
        LibraryTab.DOWNLOADS -> filters.downloadedTracks.map { it.track }
        else -> emptyList()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = tab,
            label = "library_tab_crossfade",
            modifier = Modifier.fillMaxSize()
        ) { currentTab ->
            when (currentTab) {
                LibraryTab.PLAYLISTS -> PlaylistsTabContent(
                    customPlaylists = filters.customPlaylists,
                    remotePlaylists = filters.remotePlaylists,
                    userArtists = (state.artists.map { it.name } + state.likedSongs.map { it.artist }).distinct(),
                    onCreatePlaylist = viewModel::createPlaylist,
                    onRenamePlaylist = viewModel::renamePlaylist,
                    onDeletePlaylist = viewModel::deletePlaylist,
                    onPlaylistClick = onPlaylistClick,
                    viewMode = state.viewMode
                )

                LibraryTab.LIKED -> LikedSongsTabContent(
                    songs = filters.likedSongs,
                    onPlayTrack = { track ->
                        viewModel.playTrack(track, filters.likedSongs)
                    },
                    onMoreClick = { interaction.openContextMenu(it) },
                    onSwipeLeft = {
                        interaction.showAddedToQueueSnackbar(it, activeAccent)
                    },
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
                    onToggleSelectTrack = onToggleSelectTrack,
                    onTrackLongClick = onTrackLongClick
                )

                LibraryTab.DOWNLOADS -> {
                    val manualDownloads = filters.downloadedTracks
                        .filter { it.downloadType == DownloadType.MANUAL }
                    val cachedDownloads = filters.downloadedTracks
                        .filter { it.downloadType != DownloadType.MANUAL }
                    val prioritizedOfflineQueue =
                        manualDownloads.map { it.track } + cachedDownloads.map { it.track }

                    DownloadsTabContent(
                        downloadedTracks = manualDownloads,
                        activeDownloads = state.activeDownloads,
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
                    localTracks = filters.localTracks,
                    localLibraryPaths = state.localLibraryPaths,
                    isScanning = state.isScanningLocal,
                    onRescan = viewModel::scanLocalTracks,
                    onAddFolder = viewModel::addLocalLibraryPath,
                    onRemoveFolder = viewModel::removeLocalLibraryPath,
                    onPlayTrack = { track ->
                        viewModel.playTrack(track, filters.localTracks)
                    },
                    onPlayAll = {
                        if (filters.localTracks.isNotEmpty()) {
                            viewModel.playTrack(
                                filters.localTracks.first(),
                                filters.localTracks
                            )
                        }
                    },
                    onMoreClick = { interaction.openContextMenu(it) }
                )

                LibraryTab.ARTISTS -> ArtistsTabContent(
                    artists = filters.artists,
                    onArtistClick = onArtistClick,
                    viewMode = state.viewMode
                )

                LibraryTab.ALBUMS -> AlbumsTabContent(
                    albums = filters.albums,
                    onAlbumClick = { id -> onAlbumClick(id, "", null, "") },
                    viewMode = state.viewMode
                )

                LibraryTab.HISTORY -> HistoryTabContent(
                    history = filters.history,
                    isLiked = { track -> track.id in likedTrackIds },
                    onPlayTrack = { entry ->
                        viewModel.playTrack(entry.track)
                    },
                    onMoreClick = { interaction.openContextMenu(it) },
                    onSwipeLeft = {
                        interaction.showAddedToQueueSnackbar(it, activeAccent)
                    },
                    onSwipeRight = { track ->
                        val trackIsLiked = track.id in likedTrackIds
                        interaction.showToggledLikeSnackbar(
                            track,
                            trackIsLiked,
                            activeAccent
                        )
                    },
                    isSelectionMode = isSelectionMode,
                    selectedTrackIds = selectedTrackIds,
                    onToggleSelectTrack = onToggleSelectTrack,
                    onTrackLongClick = onTrackLongClick
                )
            }
        }

        BatchSelectionBottomBar(
            isVisible = isSelectionMode,
            selectedCount = selectedTrackIds.size,
            totalCount = visibleSongs.size,
            onClearSelection = onClearSelection,
            onSelectAllToggle = { onSelectAllToggle(visibleSongs) },
            onAddToPlaylist = {
                val selTracks = visibleSongs.filter { it.id in selectedTrackIds }
                interaction.openAddToPlaylist(selTracks)
            },
            onAddToQueue = {
                val selTracks = visibleSongs.filter { it.id in selectedTrackIds }
                queueViewModel.addAllToQueue(selTracks)
                interaction.snackbar.show(
                    msg = if (selTracks.size == 1) "1 canción añadida a la cola" else "${selTracks.size} canciones añadidas a la cola",
                    actionColor = activeAccent
                )
            },
            onDownload = {
                val selTracks = visibleSongs.filter { it.id in selectedTrackIds }
                selTracks.forEach { viewModel.downloadTrack(it) }
                interaction.snackbar.show(
                    msg = "Descargando ${selTracks.size} canciones...",
                    actionColor = activeAccent
                )
            },
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}