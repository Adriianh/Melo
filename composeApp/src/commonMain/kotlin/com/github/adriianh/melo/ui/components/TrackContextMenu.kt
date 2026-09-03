package com.github.adriianh.melo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.library.LibraryUiState
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@OptIn(ExperimentalMaterial3Api::class)
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
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MeloColors.surface1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MeloColors.textMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MeloAsyncImage(
                    url = track.artworkUrl,
                    contentDescription = track.title,
                    size = 56.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MeloColors.border.copy(alpha = 0.5f),
                thickness = DividerDefaults.Thickness
            )

            ContextMenuItem(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                text = "Reproducir siguiente",
                onClick = { onPlayNext(); onDismissRequest() }
            )
            if (inQueue) {
                ContextMenuItem(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    text = "Eliminar de la cola",
                    onClick = { onRemoveFromQueue(); onDismissRequest() }
                )
            } else {
                ContextMenuItem(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    text = "Añadir a la cola",
                    onClick = { onAddToQueue(); onDismissRequest() }
                )
            }
            ContextMenuItem(
                icon = if (isLiked) Icons.Default.HeartBroken else Icons.Default.FavoriteBorder,
                text = if (isLiked) "Eliminar de Me Gusta" else "Añadir a Me Gusta",
                onClick = { onToggleLike(); onDismissRequest() }
            )

            if (isDownloaded) {
                ContextMenuItem(
                    icon = Icons.Default.DeleteOutline,
                    text = "Eliminar de descargas",
                    onClick = { onDeleteDownload?.invoke(); onDismissRequest() },
                    iconTint = MaterialTheme.colorScheme.error
                )
            } else if (isDownloading) {
                ContextMenuItem(
                    icon = Icons.Default.Downloading,
                    text = "Descargando... ${downloadProgress?.let { "${(it * 100).toInt()}%" } ?: ""}",
                    onClick = {},
                    iconTint = MaterialTheme.colorScheme.primary
                )
            } else if (onDownload != null && !track.id.startsWith("local:")) {
                ContextMenuItem(
                    icon = Icons.Default.Download,
                    text = "Descargar canción",
                    onClick = { onDownload(); onDismissRequest() }
                )
            }

            ContextMenuItem(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                text = "Añadir a playlist...",
                onClick = { onAddToPlaylist(); onDismissRequest() }
            )

            if (onRemoveFromPlaylist != null) {
                ContextMenuItem(
                    icon = Icons.Default.DeleteOutline,
                    text = "Quitar de esta playlist",
                    onClick = { onRemoveFromPlaylist(); onDismissRequest() },
                    iconTint = MaterialTheme.colorScheme.error
                )
            }

            ContextMenuItem(
                icon = Icons.Default.Person,
                text = "Ir al artista",
                onClick = { onGoToArtist(); onDismissRequest() }
            )
            ContextMenuItem(
                icon = Icons.Default.Album,
                text = "Ir al álbum",
                onClick = { onGoToAlbum(); onDismissRequest() }
            )
            ContextMenuItem(
                icon = Icons.Default.Share,
                text = "Compartir",
                onClick = { onShare(); onDismissRequest() }
            )
        }
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
) {
    val track = interaction.contextMenuTrack
    if (track != null) {
        val isLiked = libraryState.likedSongs.any { it.id == track.id }
        val isDownloaded =
            libraryState.downloadedTracks.any { it.track.id == track.id && it.downloadType == DownloadType.MANUAL }
        val isDownloading = libraryState.activeDownloads.containsKey(track.id)
        val downloadProgress = libraryState.activeDownloads[track.id]

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
            downloadProgress = downloadProgress
        )
    }

    val playlistTracks = interaction.addToPlaylistTracks
    if (!playlistTracks.isNullOrEmpty()) {
        val containingPlaylistIds by remember(playlistTracks) {
            if (playlistTracks.size == 1) {
                interaction.libraryViewModel.getPlaylistIdsForTrack(playlistTracks.first().id)
            } else {
                kotlinx.coroutines.flow.flowOf(emptySet())
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
                        playlistTracks.first()
                    ) {
                        interaction.showAddedToPlaylistSnackbar(
                            playlist.name,
                            activeAccent
                        )
                    }
                } else {
                    interaction.libraryViewModel.addTracksToPlaylist(
                        playlist.id,
                        playlistTracks
                    ) { count ->
                        interaction.showBatchAddedToPlaylistSnackbar(
                            count,
                            playlist.name,
                            activeAccent
                        )
                    }
                }
            },
            onCreatePlaylistAndAdd = { name ->
                interaction.libraryViewModel.createPlaylist(name) { id ->
                    if (playlistTracks.size == 1) {
                        interaction.libraryViewModel.addTrackToPlaylist(
                            id,
                            playlistTracks.first()
                        ) {
                            interaction.showAddedToPlaylistSnackbar(
                                name,
                                activeAccent
                            )
                        }
                    } else {
                        interaction.libraryViewModel.addTracksToPlaylist(
                            id,
                            playlistTracks
                        ) { count ->
                            interaction.showBatchAddedToPlaylistSnackbar(count, name, activeAccent)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    iconTint: Color = MeloColors.textPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            style = MeloType.body,
            color = MeloColors.textPrimary
        )
    }
}