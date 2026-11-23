@file:Suppress("ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.library_liked_empty
import com.github.adriianh.melo.ui.components.controls.LocalSwipeRevealGroup
import com.github.adriianh.melo.ui.components.controls.MeloSwipeActions
import com.github.adriianh.melo.ui.components.controls.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.controls.rememberSwipeRevealGroup
import com.github.adriianh.melo.ui.components.feedback.MeloEmptyState
import com.github.adriianh.melo.ui.components.track.TrackRow
import com.github.adriianh.melo.util.autoScrollOnNewItem
import com.github.adriianh.melo.util.desktopScroll
import org.jetbrains.compose.resources.stringResource

@Composable
fun LikedSongsTabContent(
    songs: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    modifier: Modifier = Modifier,
    isQueued: (Track) -> Boolean = { false },
    onRemoveFromQueue: ((Track) -> Unit)? = null,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
    isOffline: Boolean = false,
    isDownloaded: (Track) -> Boolean = { false },
    isDownloading: (Track) -> Boolean = { false },
    downloadProgress: (Track) -> Float = { 0f },
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (songs.isEmpty()) {
        MeloEmptyState(message = stringResource(Res.string.library_liked_empty), modifier = modifier)
        return
    }

    val listState = rememberLazyListState()
    listState.autoScrollOnNewItem(songs) { it.id }
    val revealGroup = rememberSwipeRevealGroup()

    CompositionLocalProvider(LocalSwipeRevealGroup provides revealGroup) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = contentPadding,
            modifier = modifier.fillMaxSize().desktopScroll(listState),
        ) {
            items(songs, key = { it.id }) { track ->
                val isSelected = track.id in selectedTrackIds
                val downloaded = isDownloaded(track)
                val downloading = isDownloading(track)
                val progress = downloadProgress(track)
                val isTrackEnabled = !isOffline || downloaded

                MeloSwipeableItem(
                    onSwipeRight = { onSwipeRight(track) },
                    trailingActions =
                        if (!isTrackEnabled) {
                            emptyList()
                        } else {
                            MeloSwipeActions.queueTrailingActions(
                                isQueued = isQueued(track),
                                onQueue = { onSwipeLeft(track) },
                                onRemove = onRemoveFromQueue?.let { remove -> { remove(track) } },
                            )
                        },
                    leadingAction = MeloSwipeActions.like(isLiked = true),
                    enabled = !isSelectionMode,
                ) {
                    TrackRow(
                        track = track,
                        onClick = { if (isTrackEnabled && !downloading) onPlayTrack(track) },
                        isEnabled = isTrackEnabled,
                        isDownloaded = downloaded,
                        isDownloading = downloading,
                        downloadProgress = progress,
                        onMoreClick = if (isSelectionMode) null else ({ onMoreClick(track) }),
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onSelectionToggle = { onToggleSelectTrack?.invoke(track) },
                        onLongClick = onTrackLongClick?.let { onLong -> { onLong(track) } },
                    )
                }
            }
        }
    }
}
