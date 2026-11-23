package com.github.adriianh.melo.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.controls.MeloSwipeActions
import com.github.adriianh.melo.ui.components.controls.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.track.TrackRow

fun LazyListScope.detailTrackItems(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isLiked: (Track) -> Boolean,
    isOffline: Boolean = false,
    onTrackClick: (Int, Track) -> Unit,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    accentColor: Color,
    isQueued: (Track) -> Boolean = { false },
    onRemoveFromQueue: ((Track) -> Unit)? = null,
    keyPrefix: String = "detail",
    itemModifier: Modifier = Modifier,
    isDownloaded: (Track) -> Boolean = { false },
    isDownloading: (Track) -> Boolean = { false },
    downloadProgress: (Track) -> Float = { 0f },
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onTrackLongClick: ((Track) -> Unit)? = null,
    onToggleSelectTrack: ((Track) -> Unit)? = null,
) {
    itemsIndexed(
        items = tracks,
        key = { index, song -> "${keyPrefix}_${song.id}_$index" },
        contentType = { _, _ -> "detail_track_row" },
    ) { index, song ->
        val isPlayingThis = currentTrackId == song.id
        val liked = isLiked(song)
        val downloaded = isDownloaded(song)
        val downloading = isDownloading(song)
        val progress = downloadProgress(song)
        val isSelected = song.id in selectedTrackIds
        val isTrackEnabled = !isOffline || downloaded

        Box(modifier = itemModifier) {
            MeloSwipeableItem(
                onSwipeRight = { onSwipeRight(song) },
                trailingActions =
                    MeloSwipeActions.queueTrailingActions(
                        isQueued = isQueued(song),
                        onQueue = { onSwipeLeft(song) },
                        onRemove = onRemoveFromQueue?.let { remove -> { remove(song) } },
                    ),
                leadingAction = MeloSwipeActions.like(liked),
                enabled = !isSelectionMode && isTrackEnabled,
            ) {
                val rowBg =
                    when {
                        isSelected -> accentColor.copy(alpha = 0.18f)
                        isPlayingThis -> accentColor.copy(alpha = 0.15f)
                        else -> Color.Transparent
                    }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(rowBg),
                ) {
                    TrackRow(
                        track = song,
                        trackNumber = index + 1,
                        isCurrent = isPlayingThis,
                        isPlaying = isPlayingThis && isPlaying,
                        isEnabled = isTrackEnabled,
                        isDownloaded = downloaded,
                        isDownloading = downloading,
                        downloadProgress = progress,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onSelectionToggle = { onToggleSelectTrack?.invoke(song) },
                        onLongClick = onTrackLongClick?.let { onLong -> { onLong(song) } },
                        onClick = { if (!downloading) onTrackClick(index, song) },
                        onMoreClick = if (isSelectionMode) null else ({ onMoreClick(song) }),
                    )
                }
            }
        }
    }
}
