package com.github.adriianh.melo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.ColorUtils
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.PlayerUiState
import org.koin.compose.viewmodel.koinViewModel

class TrackInteractionState(
    val snackbar: MeloSnackbarState,
    val libraryViewModel: LibraryViewModel,
    val queueViewModel: QueueViewModel,
) {
    var contextMenuTrack by mutableStateOf<Track?>(null)
    var addToPlaylistTrack by mutableStateOf<Track?>(null)
    var addToPlaylistTracks by mutableStateOf<List<Track>?>(null)

    fun openContextMenu(track: Track) {
        contextMenuTrack = track
    }

    fun dismissContextMenu() {
        contextMenuTrack = null
    }

    fun openAddToPlaylist(track: Track) {
        addToPlaylistTrack = track
        addToPlaylistTracks = listOf(track)
    }

    fun openAddToPlaylist(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        addToPlaylistTrack = tracks.firstOrNull()
        addToPlaylistTracks = tracks
    }

    fun dismissAddToPlaylist() {
        addToPlaylistTrack = null
        addToPlaylistTracks = null
    }

    fun showAddedToPlaylistSnackbar(
        track: Track,
        playlistName: String,
        actionColor: Color? = null
    ) {
        snackbar.show(
            msg = "Añadida a $playlistName",
            vector = Icons.AutoMirrored.Filled.PlaylistPlay,
            actionColor = actionColor
        )
    }

    fun showBatchAddedToPlaylistSnackbar(
        count: Int,
        playlistName: String,
        actionColor: Color? = null
    ) {
        snackbar.show(
            msg = if (count == 1) "1 canción añadida a $playlistName" else "$count canciones añadidas a $playlistName",
            vector = Icons.AutoMirrored.Filled.PlaylistPlay,
            actionColor = actionColor
        )
    }

    fun showPlayNextSnackbar(track: Track, actionColor: Color? = null) {
        val insertIndex = queueViewModel.queueState.value.currentIndex + 1
        queueViewModel.insertTrackNext(track)
        snackbar.show(
            msg = "Se reproducirá a continuación",
            vector = Icons.AutoMirrored.Filled.PlaylistPlay,
            action = "Deshacer",
            actionColor = actionColor,
            onAction = { queueViewModel.removeTrackAt(insertIndex) }
        )
    }

    fun showAddedToQueueSnackbar(track: Track, actionColor: Color? = null) {
        queueViewModel.addToQueue(track)
        snackbar.show(
            msg = "Añadida a la cola",
            vector = Icons.AutoMirrored.Filled.QueueMusic,
            action = "Deshacer",
            actionColor = actionColor,
            onAction = { queueViewModel.removeTrackFromQueue(track) }
        )
    }

    fun showToggledLikeSnackbar(
        track: Track,
        isCurrentlyLiked: Boolean,
        actionColor: Color? = null
    ) {
        libraryViewModel.toggleLike(track.id, !isCurrentlyLiked)
        snackbar.show(
            msg = if (isCurrentlyLiked) "Eliminada de tus Me Gusta" else "Añadida a tus Me Gusta",
            vector = if (isCurrentlyLiked) Icons.Default.HeartBroken else Icons.Default.Favorite,
            action = "Deshacer",
            actionColor = actionColor,
            onAction = { libraryViewModel.toggleLike(track.id, isCurrentlyLiked) }
        )
    }

    fun downloadTrack(track: Track, actionColor: Color? = null) {
        libraryViewModel.downloadTrack(track)
        snackbar.show(
            msg = "Descargando ${track.title}...",
            actionColor = actionColor
        )
    }

    fun deleteDownloadedTrack(trackId: String, actionColor: Color? = null) {
        libraryViewModel.deleteDownloadedTrack(trackId)
        snackbar.show(
            msg = "Descarga eliminada",
            actionColor = actionColor
        )
    }

    @Composable
    fun resolveActiveAccent(playerState: PlayerUiState): Color {
        val color = playerState.accentColor
        return if (color != Color.Transparent && color != MeloColors.textMuted && color != Color.Black) {
            ColorUtils.harmonize(color, MeloColors.isDark)
        } else {
            MeloColors.brandAccent
        }
    }
}

@Composable
fun rememberTrackInteraction(
    libraryViewModel: LibraryViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
): TrackInteractionState {
    val snackbar = LocalMeloSnackbar.current
    val scope = rememberCoroutineScope()
    return remember(snackbar, scope, libraryViewModel, queueViewModel) {
        TrackInteractionState(snackbar, libraryViewModel, queueViewModel)
    }
}