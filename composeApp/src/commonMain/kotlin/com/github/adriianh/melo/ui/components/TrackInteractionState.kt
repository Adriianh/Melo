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
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.PlayerUiState
import kotlinx.coroutines.CoroutineScope
import org.koin.compose.viewmodel.koinViewModel

class TrackInteractionState(
    val snackbar: MeloSnackbarState,
    val scope: CoroutineScope,
    private val libraryViewModel: LibraryViewModel,
    private val queueViewModel: QueueViewModel,
) {
    var contextMenuTrack by mutableStateOf<Track?>(null)

    fun openContextMenu(track: Track) {
        contextMenuTrack = track
    }

    fun dismissContextMenu() {
        contextMenuTrack = null
    }

    fun showPlayNextSnackbar(track: Track, actionColor: Color? = null) {
        val insertIndex = queueViewModel.queueState.value.currentIndex + 1
        queueViewModel.insertTrackNext(track)
        snackbar.show(
            msg = "Se reproducirá a continuación",
            vector = Icons.AutoMirrored.Filled.PlaylistPlay,
            scope = scope,
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
            scope = scope,
            action = "Deshacer",
            actionColor = actionColor,
            onAction = { queueViewModel.removeTrackFromQueue(track) }
        )
    }

    fun showToggledLikeSnackbar(track: Track, isCurrentlyLiked: Boolean, actionColor: Color? = null) {
        libraryViewModel.toggleLike(track.id, !isCurrentlyLiked)
        snackbar.show(
            msg = if (isCurrentlyLiked) "Eliminada de tus Me Gusta" else "Añadida a tus Me Gusta",
            vector = if (isCurrentlyLiked) Icons.Default.HeartBroken else Icons.Default.Favorite,
            scope = scope,
            action = "Deshacer",
            actionColor = actionColor,
            onAction = { libraryViewModel.toggleLike(track.id, isCurrentlyLiked) }
        )
    }

    @Composable
    fun resolveActiveAccent(playerState: PlayerUiState): Color {
        return if (playerState.accentColor != Color.Transparent && playerState.accentColor != MeloColors.textMuted) {
            playerState.accentColor
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
        TrackInteractionState(snackbar, scope, libraryViewModel, queueViewModel)
    }
}
