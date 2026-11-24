@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.common_action_undo
import com.github.adriianh.melo.resources.player_sheet_artist_title
import com.github.adriianh.melo.resources.player_sheet_queue_title
import com.github.adriianh.melo.resources.track_snackbar_added_to_likes
import com.github.adriianh.melo.resources.track_snackbar_added_to_queue
import com.github.adriianh.melo.resources.track_snackbar_play_next
import com.github.adriianh.melo.resources.track_snackbar_removed_from_likes
import com.github.adriianh.melo.resources.track_snackbar_removed_from_queue
import com.github.adriianh.melo.ui.components.feedback.MeloSnackbarHost
import com.github.adriianh.melo.ui.components.feedback.MeloSnackbarState
import com.github.adriianh.melo.ui.components.track.SongMenuAnchor
import com.github.adriianh.melo.ui.i18n.UiText
import com.github.adriianh.melo.ui.library.LibraryViewModel
import com.github.adriianh.melo.ui.player.PanelSection
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerUiState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedSectionSheet(
    section: PanelSection?,
    state: PlayerUiState,
    artistDetails: SearchResult.Artist?,
    activeAccent: Color,
    snackbarState: MeloSnackbarState,
    onDismiss: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    queueViewModel: QueueViewModel = koinViewModel(),
    libraryViewModel: LibraryViewModel = koinViewModel(),
    onMoreClick: (Track, Boolean) -> Unit = { _, _ -> },
    onOpenContextMenu: ((Track, Boolean, SongMenuAnchor?) -> Unit)? = null,
    isLiked: (Track) -> Boolean = { false },
) {
    if (section == null) return

    ModalBottomSheet(
        onDismissRequest = {
            snackbarState.dismiss()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MeloColors.surface1.copy(alpha = 0.95f),
        contentColor = MeloColors.textPrimary,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        tonalElevation = 12.dp,
        dragHandle = {
            Box(
                modifier =
                    Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MeloColors.textMuted.copy(alpha = 0.4f)),
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                        .fillMaxHeight(0.70f),
            ) {
                Text(
                    text =
                        when (section) {
                            PanelSection.QUEUE -> stringResource(Res.string.player_sheet_queue_title)
                            PanelSection.ARTIST -> stringResource(Res.string.player_sheet_artist_title)
                            else -> ""
                        },
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                when (section) {
                    PanelSection.QUEUE ->
                        NowPlayingQueueSection(
                            state = state,
                            activeAccent = activeAccent,
                            onMoreClick = onMoreClick,
                            onOpenContextMenu = onOpenContextMenu,
                            onSwipeQueueItem = { trackIndex ->
                                val removedTrack = queueViewModel.removeTrackAt(trackIndex)
                                if (removedTrack != null) {
                                    snackbarState.show(
                                        msg = UiText.Resource(Res.string.track_snackbar_removed_from_queue),
                                        vector = Icons.Default.Delete,
                                        action = UiText.Resource(Res.string.common_action_undo),
                                        actionColor = activeAccent,
                                        onAction = {
                                            queueViewModel.insertTrackAt(
                                                removedTrack,
                                                trackIndex,
                                            )
                                        },
                                    )
                                }
                            },
                            onSwipeSuggestionItem = { track ->
                                queueViewModel.addToQueue(track)
                                snackbarState.show(
                                    msg = UiText.Resource(Res.string.track_snackbar_added_to_queue),
                                    vector = Icons.AutoMirrored.Filled.QueueMusic,
                                    action = UiText.Resource(Res.string.common_action_undo),
                                    actionColor = activeAccent,
                                    onAction = { queueViewModel.removeTrackFromQueue(track) },
                                )
                            },
                            onPlayNextSuggestionItem = { track ->
                                val insertIndex =
                                    queueViewModel.queueState.value.currentIndex + 1
                                queueViewModel.insertTrackNext(track)
                                snackbarState.show(
                                    msg = UiText.Resource(Res.string.track_snackbar_play_next),
                                    vector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                    action = UiText.Resource(Res.string.common_action_undo),
                                    actionColor = activeAccent,
                                    onAction = { queueViewModel.removeTrackAt(insertIndex) },
                                )
                            },
                            onSwipeRight = { track ->
                                val trackIsLiked = isLiked(track)
                                libraryViewModel.toggleLike(track.id, !trackIsLiked)
                                snackbarState.show(
                                    msg =
                                        UiText.Resource(
                                            if (trackIsLiked) {
                                                Res.string.track_snackbar_removed_from_likes
                                            } else {
                                                Res.string.track_snackbar_added_to_likes
                                            },
                                        ),
                                    vector = if (trackIsLiked) Icons.Default.HeartBroken else Icons.Default.Favorite,
                                    action = UiText.Resource(Res.string.common_action_undo),
                                    actionColor = activeAccent,
                                    onAction = {
                                        libraryViewModel.toggleLike(
                                            track.id,
                                            trackIsLiked,
                                        )
                                    },
                                )
                            },
                            isLiked = isLiked,
                        )

                    PanelSection.ARTIST ->
                        NowPlayingArtistSection(
                            state = state,
                            artistDetails = artistDetails,
                            activeAccent = activeAccent,
                            onArtistClick = { id ->
                                snackbarState.dismiss()
                                onArtistClick(id)
                            },
                            onAlbumClick = { id ->
                                snackbarState.dismiss()
                                onAlbumClick(id)
                            },
                            onPlaylistClick = { id ->
                                snackbarState.dismiss()
                                onPlaylistClick(id)
                            },
                            onOpenTrackContextMenu = { track, anchor ->
                                onOpenContextMenu?.invoke(track, false, anchor)
                            },
                        )

                    else -> {}
                }
            }
            MeloSnackbarHost(
                state = snackbarState,
                modifier = Modifier.align(Alignment.BottomCenter),
                bottomPadding = 24.dp,
            )
        }
    }
}
