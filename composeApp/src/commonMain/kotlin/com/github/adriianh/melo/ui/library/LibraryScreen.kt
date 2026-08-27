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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.ui.components.TrackContextMenu
import com.github.adriianh.melo.ui.components.rememberTrackInteraction
import com.github.adriianh.melo.ui.library.components.AlbumsTabContent
import com.github.adriianh.melo.ui.library.components.ArtistsTabContent
import com.github.adriianh.melo.ui.library.components.HistoryTabContent
import com.github.adriianh.melo.ui.library.components.LibraryHeader
import com.github.adriianh.melo.ui.library.components.LibraryNotLoggedInCard
import com.github.adriianh.melo.ui.library.components.LikedSongsTabContent
import com.github.adriianh.melo.ui.library.components.PlaylistsTabContent
import com.github.adriianh.melo.ui.player.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenSettings: () -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val playerViewModel: PlayerViewModel = koinViewModel()
    val playerState by playerViewModel.uiState.collectAsState()

    val interaction = rememberTrackInteraction()
    val activeAccent = interaction.resolveActiveAccent(playerState)

    if (interaction.contextMenuTrack != null) {
        val track = interaction.contextMenuTrack!!
        val isLiked = state.likedSongs.any { it.id == track.id }
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

    if (!state.isLoggedIn) {
        LibraryNotLoggedInCard(onLoginClick = onOpenSettings)
        return
    }

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

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            when (state.selectedTab) {
                LibraryTab.PLAYLISTS -> PlaylistsTabContent(
                    playlists = state.playlists,
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
                            scope = interaction.scope,
                            action = "Deshacer",
                            actionColor = activeAccent,
                            onAction = {
                                viewModel.toggleLike(track.id, true)
                            }
                        )
                    }
                )

                LibraryTab.ARTISTS -> ArtistsTabContent(
                    artists = state.artists,
                    onArtistClick = onArtistClick
                )

                LibraryTab.ALBUMS -> AlbumsTabContent(
                    albums = state.albums,
                    onAlbumClick = onAlbumClick
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
                    }
                )
            }
        }
    }
}