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
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.common_action_undo
import com.github.adriianh.melo.resources.track_snackbar_added_to_likes
import com.github.adriianh.melo.resources.track_snackbar_added_to_playlist
import com.github.adriianh.melo.resources.track_snackbar_added_to_queue
import com.github.adriianh.melo.resources.track_snackbar_batch_added_to_playlist
import com.github.adriianh.melo.resources.track_snackbar_download_removed
import com.github.adriianh.melo.resources.track_snackbar_downloading
import com.github.adriianh.melo.resources.track_snackbar_play_next
import com.github.adriianh.melo.resources.track_snackbar_removed_from_likes
import com.github.adriianh.melo.ui.i18n.UiText
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
        playlistName: String,
        actionColor: Color? = null,
    ) {
        snackbar.show(
            msg = UiText.Resource(Res.string.track_snackbar_added_to_playlist, listOf(playlistName)),
            vector = Icons.AutoMirrored.Filled.PlaylistPlay,
            actionColor = actionColor,
        )
    }

    fun showBatchAddedToPlaylistSnackbar(
        count: Int,
        playlistName: String,
        actionColor: Color? = null,
    ) {
        snackbar.show(
            msg =
                UiText.Plural(
                    res = Res.plurals.track_snackbar_batch_added_to_playlist,
                    quantity = count,
                    args = listOf(count, playlistName),
                ),
            vector = Icons.AutoMirrored.Filled.PlaylistPlay,
            actionColor = actionColor,
        )
    }

    fun showPlayNextSnackbar(
        track: Track,
        actionColor: Color? = null,
    ) {
        val insertIndex = queueViewModel.queueState.value.currentIndex + 1
        queueViewModel.insertTrackNext(track)
        snackbar.show(
            msg = UiText.Resource(Res.string.track_snackbar_play_next),
            vector = Icons.AutoMirrored.Filled.PlaylistPlay,
            action = UiText.Resource(Res.string.common_action_undo),
            actionColor = actionColor,
            onAction = { queueViewModel.removeTrackAt(insertIndex) },
        )
    }

    fun showAddedToQueueSnackbar(
        track: Track,
        actionColor: Color? = null,
    ) {
        queueViewModel.addToQueue(track)
        snackbar.show(
            msg = UiText.Resource(Res.string.track_snackbar_added_to_queue),
            vector = Icons.AutoMirrored.Filled.QueueMusic,
            action = UiText.Resource(Res.string.common_action_undo),
            actionColor = actionColor,
            onAction = { queueViewModel.removeTrackFromQueue(track) },
        )
    }

    fun showToggledLikeSnackbar(
        track: Track,
        isCurrentlyLiked: Boolean,
        actionColor: Color? = null,
    ) {
        libraryViewModel.toggleLike(track.id, !isCurrentlyLiked)
        snackbar.show(
            msg =
                UiText.Resource(
                    if (isCurrentlyLiked) {
                        Res.string.track_snackbar_removed_from_likes
                    } else {
                        Res.string.track_snackbar_added_to_likes
                    },
                ),
            vector = if (isCurrentlyLiked) Icons.Default.HeartBroken else Icons.Default.Favorite,
            action = UiText.Resource(Res.string.common_action_undo),
            actionColor = actionColor,
            onAction = { libraryViewModel.toggleLike(track.id, isCurrentlyLiked) },
        )
    }

    fun downloadTrack(
        track: Track,
        actionColor: Color? = null,
    ) {
        libraryViewModel.downloadTrack(track)
        snackbar.show(
            msg = UiText.Resource(Res.string.track_snackbar_downloading, listOf(track.title)),
            actionColor = actionColor,
        )
    }

    fun deleteDownloadedTrack(
        trackId: String,
        actionColor: Color? = null,
    ) {
        libraryViewModel.deleteDownloadedTrack(trackId)
        snackbar.show(
            msg = UiText.Resource(Res.string.track_snackbar_download_removed),
            actionColor = actionColor,
        )
    }

    @Composable
    fun resolveActiveAccent(accentColor: Color): Color =
        if (accentColor != Color.Transparent && accentColor != MeloColors.textMuted && accentColor != Color.Black) {
            ColorUtils.harmonize(accentColor, MeloColors.isDark)
        } else {
            MeloColors.brandAccent
        }

    @Composable
    fun resolveActiveAccent(playerState: PlayerUiState): Color = resolveActiveAccent(playerState.accentColor)
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
