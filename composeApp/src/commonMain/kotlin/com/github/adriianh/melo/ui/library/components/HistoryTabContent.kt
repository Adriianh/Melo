package com.github.adriianh.melo.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.MeloEmptyState
import com.github.adriianh.melo.ui.components.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.util.formatRelativeTime

@Composable
fun HistoryTabContent(
    history: List<HistoryEntry>,
    isLiked: (Track) -> Boolean,
    onPlayTrack: (HistoryEntry) -> Unit,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
    if (history.isEmpty()) {
        MeloEmptyState(
            message = "No hay reproducciones recientes registradas.",
            modifier = modifier
        )
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(history, key = { it.track.id + "_" + it.playedAt }) { entry ->
            val track = entry.track
            val isSelected = track.id in selectedTrackIds
            val relTime = formatRelativeTime(entry.playedAt)
            val subtitleWithTime = buildString {
                append(track.artist)
                if (track.album.isNotBlank() && track.album != track.title) {
                    append(" • ")
                    append(track.album)
                }
                if (relTime.isNotBlank()) {
                    append(" • ")
                    append(relTime)
                }
            }

            MeloSwipeableItem(
                onSwipeLeft = { if (!isSelectionMode) onSwipeLeft(track) },
                onSwipeRight = { if (!isSelectionMode) onSwipeRight(track) },
                swipeRightIcon = if (isLiked(track)) Icons.Default.HeartBroken else Icons.Default.Favorite
            ) {
                TrackRow(
                    track = track,
                    subtitleOverride = subtitleWithTime,
                    onClick = { onPlayTrack(entry) },
                    onMoreClick = if (isSelectionMode) null else {
                        { onMoreClick(track) }
                    },
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    onSelectionToggle = { onToggleSelectTrack?.invoke(track) },
                    onLongClick = onTrackLongClick?.let { onLong -> { onLong(track) } }
                )
            }
        }
    }
}