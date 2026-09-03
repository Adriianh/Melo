package com.github.adriianh.melo.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.MeloEmptyState
import com.github.adriianh.melo.ui.components.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.TrackRow

@Composable
fun LikedSongsTabContent(
    songs: List<Track>,
    onPlayTrack: (Track) -> Unit,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
    if (songs.isEmpty()) {
        MeloEmptyState(message = "Aún no tienes canciones en tus 'Me Gusta'.", modifier = modifier)
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(songs, key = { it.id }) { track ->
            val isSelected = track.id in selectedTrackIds
            MeloSwipeableItem(
                onSwipeLeft = { if (!isSelectionMode) onSwipeLeft(track) },
                onSwipeRight = { if (!isSelectionMode) onSwipeRight(track) },
                swipeRightIcon = Icons.Default.HeartBroken
            ) {
                TrackRow(
                    track = track,
                    onClick = { onPlayTrack(track) },
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
