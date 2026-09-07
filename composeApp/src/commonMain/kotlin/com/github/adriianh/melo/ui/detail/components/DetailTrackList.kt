package com.github.adriianh.melo.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.util.MeloColors

fun LazyListScope.detailTrackItems(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isLiked: (Track) -> Boolean,
    onTrackClick: (Int, Track) -> Unit,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    accentColor: Color,
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
        tracks,
        key = { index, song -> "${keyPrefix}_${song.id}_$index" }
    ) { index, song ->
        val isPlayingThis = currentTrackId == song.id
        val liked = isLiked(song)
        val downloaded = isDownloaded(song)
        val downloading = isDownloading(song)
        val progress = downloadProgress(song)
        val isSelected = song.id in selectedTrackIds

        Box(modifier = itemModifier) {
            MeloSwipeableItem(
                onSwipeLeft = { if (!isSelectionMode) onSwipeLeft(song) },
                onSwipeRight = { if (!isSelectionMode) onSwipeRight(song) },
                swipeRightIcon = if (liked) Icons.Default.HeartBroken else Icons.Default.Favorite,
                swipeLeftIcon = Icons.AutoMirrored.Filled.QueueMusic,
                swipeLeftColor = MeloColors.brandAccent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) accentColor.copy(alpha = 0.18f)
                            else if (isPlayingThis) accentColor.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                ) {
                    TrackRow(
                        track = song,
                        trackNumber = index + 1,
                        isCurrent = isPlayingThis,
                        isPlaying = isPlayingThis && isPlaying,
                        isDownloaded = downloaded,
                        isDownloading = downloading,
                        downloadProgress = progress,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onSelectionToggle = { onToggleSelectTrack?.invoke(song) },
                        onLongClick = onTrackLongClick?.let { onLong -> { onLong(song) } },
                        onClick = {
                            if (!downloading) {
                                onTrackClick(index, song)
                            }
                        },
                        onMoreClick = if (isSelectionMode) null else {
                            { onMoreClick(song) }
                        }
                    )
                }
            }
        }
    }
}