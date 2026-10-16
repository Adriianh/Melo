package com.github.adriianh.melo.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.github.adriianh.melo.util.autoScrollOnNewItem
import com.github.adriianh.melo.util.desktopScroll
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
    isOffline: Boolean = false,
    isDownloaded: (Track) -> Boolean = { false },
    isDownloading: (Track) -> Boolean = { false },
    downloadProgress: (Track) -> Float = { 0f },
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    if (history.isEmpty()) {
        MeloEmptyState(
            message = "No hay reproducciones recientes registradas.",
            modifier = modifier,
        )
        return
    }

    val listState = rememberLazyListState()
    listState.autoScrollOnNewItem(history) { "${it.track.id}_${it.playedAt}" }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize().desktopScroll(listState),
    ) {
        items(history, key = { it.track.id + "_" + it.playedAt }) { entry ->
            val track = entry.track
            val isSelected = track.id in selectedTrackIds
            val downloaded = isDownloaded(track)
            val downloading = isDownloading(track)
            val progress = downloadProgress(track)
            val isTrackEnabled = !isOffline || downloaded

            historyItemRow(
                entry = entry,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                isTrackEnabled = isTrackEnabled,
                downloaded = downloaded,
                downloading = downloading,
                progress = progress,
                isLiked = isLiked(track),
                onPlayTrack = onPlayTrack,
                onMoreClick = onMoreClick,
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight,
                onToggleSelectTrack = onToggleSelectTrack,
                onTrackLongClick = onTrackLongClick,
            )
        }
    }
}

@Composable
private fun historyItemRow(
    entry: HistoryEntry,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isTrackEnabled: Boolean,
    downloaded: Boolean,
    downloading: Boolean,
    progress: Float,
    isLiked: Boolean,
    onPlayTrack: (HistoryEntry) -> Unit,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    onToggleSelectTrack: ((Track) -> Unit)?,
    onTrackLongClick: ((Track) -> Unit)?,
) {
    val track = entry.track
    val relTime = formatRelativeTime(entry.playedAt)
    val subtitleWithTime =
        buildString {
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
        onSwipeLeft = { if (!isSelectionMode && isTrackEnabled) onSwipeLeft(track) },
        onSwipeRight = { if (!isSelectionMode) onSwipeRight(track) },
        swipeRightIcon = if (isLiked) Icons.Default.HeartBroken else Icons.Default.Favorite,
    ) {
        TrackRow(
            track = track,
            subtitleOverride = subtitleWithTime,
            onClick = {
                if (isTrackEnabled && !downloading) {
                    onPlayTrack(entry)
                }
            },
            isEnabled = isTrackEnabled,
            isDownloaded = downloaded,
            isDownloading = downloading,
            downloadProgress = progress,
            onMoreClick =
                if (isSelectionMode) {
                    null
                } else {
                    { onMoreClick(track) }
                },
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onSelectionToggle = { onToggleSelectTrack?.invoke(track) },
            onLongClick = onTrackLongClick?.let { onLong -> { onLong(track) } },
        )
    }
}
