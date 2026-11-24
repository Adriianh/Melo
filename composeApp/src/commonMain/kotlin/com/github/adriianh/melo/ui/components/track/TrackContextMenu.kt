@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.components.track

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.dialogs.AddToPlaylistSheet
import com.github.adriianh.melo.ui.library.LibraryUiState
import kotlinx.coroutines.flow.flowOf

@Composable
fun TrackContextMenu(
    track: Track,
    onDismissRequest: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onRemoveFromQueue: () -> Unit = {},
    inQueue: Boolean = false,
    onToggleLike: () -> Unit,
    isLiked: Boolean = false,
    onAddToPlaylist: () -> Unit,
    onGoToArtist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onShare: () -> Unit,
    onDownload: (() -> Unit)? = null,
    onDeleteDownload: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onRemoveFromLibrary: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float? = null,
    isOnArtistScreen: Boolean = false,
    isOnAlbumScreen: Boolean = false,
    anchor: SongMenuAnchor? = null,
    playlists: List<Playlist> = emptyList(),
    containingPlaylistIds: Set<Long> = emptySet(),
    onSelectPlaylist: (Playlist) -> Unit = {},
    onCreatePlaylistAndAdd: (String) -> Unit = {},
) {
    val windowSize = rememberSongMenuWindowSize()
    val entries =
        remember(
            track,
            isLiked,
            isDownloaded,
            isDownloading,
            downloadProgress,
            inQueue,
            isOnArtistScreen,
            isOnAlbumScreen,
            playlists,
            containingPlaylistIds,
            onRemoveFromPlaylist,
            onRemoveFromLibrary,
        ) {
            buildSongMenuEntries(
                track = track,
                context =
                    SongMenuContext(
                        isLiked = isLiked,
                        isDownloaded = isDownloaded,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        inQueue = inQueue,
                        isOnArtistScreen = isOnArtistScreen,
                        isOnAlbumScreen = isOnAlbumScreen,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onRemoveFromQueue = onRemoveFromQueue,
                        onToggleLike = onToggleLike,
                        onAddToPlaylist = onAddToPlaylist,
                        playlists = playlists,
                        containingPlaylistIds = containingPlaylistIds,
                        onSelectPlaylist = onSelectPlaylist,
                        onCreatePlaylistAndAdd = onCreatePlaylistAndAdd,
                        onDownload = onDownload,
                        onDeleteDownload = onDeleteDownload,
                        onGoToArtist = onGoToArtist,
                        onGoToAlbum = onGoToAlbum,
                        onShare = onShare,
                        onRemoveFromPlaylist = onRemoveFromPlaylist,
                        onRemoveFromLibrary = onRemoveFromLibrary,
                    ),
            )
        }

    when (windowSize) {
        SongMenuWindowSize.Compact ->
            SongMenuCompactSheet(
                track = track,
                entries = entries,
                onDismissRequest = onDismissRequest,
            )

        SongMenuWindowSize.Medium,
        SongMenuWindowSize.Expanded,
        ->
            SongMenuAnchoredPopup(
                entries = entries,
                anchor = anchor,
                windowSize = windowSize,
                onDismissRequest = onDismissRequest,
            )
    }
}

@Composable
fun TrackInteractionContextMenu(
    interaction: TrackInteractionState,
    libraryState: LibraryUiState,
    activeAccent: Color,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    inQueue: Boolean = false,
    onRemoveFromQueue: () -> Unit = {},
    onRemoveFromPlaylist: ((Track) -> Unit)? = null,
    onRemoveFromLibrary: ((Track) -> Unit)? = null,
    isOnArtistScreen: Boolean = false,
    isOnAlbumScreen: Boolean = false,
) {
    val track = interaction.contextMenuTrack
    if (track != null) {
        val isLiked = libraryState.likedSongs.any { it.id == track.id }
        val isDownloaded =
            libraryState.downloadedTracks.any { it.track.id == track.id && it.downloadType == DownloadType.MANUAL }
        val isDownloading = libraryState.activeDownloads.containsKey(track.id)
        val downloadProgress = libraryState.activeDownloads[track.id]
        val containingPlaylistIds by remember(track.id) {
            interaction.libraryViewModel.getPlaylistIdsForTrack(track.id)
        }.collectAsState(initial = emptySet())

        TrackContextMenu(
            track = track,
            onDismissRequest = interaction::dismissContextMenu,
            onPlayNext = {
                interaction.showPlayNextSnackbar(track, activeAccent)
                interaction.dismissContextMenu()
            },
            onAddToQueue = {
                interaction.showAddedToQueueSnackbar(track, activeAccent)
                interaction.dismissContextMenu()
            },
            onRemoveFromQueue = {
                onRemoveFromQueue()
                interaction.dismissContextMenu()
            },
            inQueue = inQueue,
            onToggleLike = {
                interaction.showToggledLikeSnackbar(track, isLiked, activeAccent)
                interaction.dismissContextMenu()
            },
            isLiked = isLiked,
            onAddToPlaylist = {
                interaction.openAddToPlaylist(track)
                interaction.dismissContextMenu()
            },
            onRemoveFromPlaylist = onRemoveFromPlaylist?.let { rem -> { rem(track) } },
            onRemoveFromLibrary = onRemoveFromLibrary?.let { rem -> { rem(track) } },
            onGoToArtist = {
                interaction.dismissContextMenu()
                onArtistClick(track.artist)
            },
            onGoToAlbum = {
                interaction.dismissContextMenu()
                if (track.album.isNotBlank()) onAlbumClick(track.album)
            },
            onShare = { /* TODO */ },
            onDownload = {
                interaction.downloadTrack(track, activeAccent)
                interaction.dismissContextMenu()
            },
            onDeleteDownload = {
                interaction.deleteDownloadedTrack(track.id, activeAccent)
                interaction.dismissContextMenu()
            },
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            isOnArtistScreen = isOnArtistScreen,
            isOnAlbumScreen = isOnAlbumScreen,
            anchor = interaction.contextMenuAnchor,
            playlists = libraryState.customPlaylists,
            containingPlaylistIds = containingPlaylistIds,
            onSelectPlaylist = { playlist ->
                interaction.libraryViewModel.addTrackToPlaylist(playlist.id, track) {
                    interaction.showAddedToPlaylistSnackbar(playlist.name, activeAccent)
                }
            },
            onCreatePlaylistAndAdd = { name ->
                interaction.libraryViewModel.createPlaylist(name) { id ->
                    interaction.libraryViewModel.addTrackToPlaylist(id, track) {
                        interaction.showAddedToPlaylistSnackbar(name, activeAccent)
                    }
                }
            },
        )
    }

    val playlistTracks = interaction.addToPlaylistTracks
    if (!playlistTracks.isNullOrEmpty()) {
        val containingPlaylistIds by remember(playlistTracks) {
            if (playlistTracks.size == 1) {
                interaction.libraryViewModel.getPlaylistIdsForTrack(playlistTracks.first().id)
            } else {
                flowOf(emptySet())
            }
        }.collectAsState(initial = emptySet())

        AddToPlaylistSheet(
            tracks = playlistTracks,
            playlists = libraryState.customPlaylists,
            containingPlaylistIds = containingPlaylistIds,
            onDismissRequest = interaction::dismissAddToPlaylist,
            onSelectPlaylist = { playlist ->
                if (playlistTracks.size == 1) {
                    interaction.libraryViewModel.addTrackToPlaylist(
                        playlist.id,
                        playlistTracks.first(),
                    ) {
                        interaction.showAddedToPlaylistSnackbar(
                            playlist.name,
                            activeAccent,
                        )
                    }
                } else {
                    interaction.libraryViewModel.addTracksToPlaylist(
                        playlist.id,
                        playlistTracks,
                    ) { count ->
                        interaction.showBatchAddedToPlaylistSnackbar(
                            count,
                            playlist.name,
                            activeAccent,
                        )
                    }
                }
            },
            onCreatePlaylistAndAdd = { name ->
                interaction.libraryViewModel.createPlaylist(name) { id ->
                    if (playlistTracks.size == 1) {
                        interaction.libraryViewModel.addTrackToPlaylist(
                            id,
                            playlistTracks.first(),
                        ) {
                            interaction.showAddedToPlaylistSnackbar(
                                name,
                                activeAccent,
                            )
                        }
                    } else {
                        interaction.libraryViewModel.addTracksToPlaylist(
                            id,
                            playlistTracks,
                        ) { count ->
                            interaction.showBatchAddedToPlaylistSnackbar(count, name, activeAccent)
                        }
                    }
                }
            },
        )
    }
}
