package com.github.adriianh.melo.ui.components.track

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.layout.MultiRowHorizontalCarousel

@Composable
fun TrackFourRowCarousel(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
    containerWidth: Dp? = null,
    listState: LazyListState = rememberLazyListState(),
    onMoreClick: ((Track) -> Unit)? = null,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
    MultiRowHorizontalCarousel(
        items = tracks,
        rows = 4,
        minCardWidth = 320.dp,
        modifier = modifier,
        containerWidth = containerWidth,
        listState = listState,
        key = { col -> col.firstOrNull()?.id ?: col.hashCode() },
    ) { track ->
        val isSelected = track.id in selectedTrackIds
        TrackRow(
            track = track,
            onClick = { onTrackClick(track) },
            showGlassBackground = true,
            onMoreClick =
                if (onMoreClick != null && !isSelectionMode) {
                    { onMoreClick(track) }
                } else {
                    null
                },
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onSelectionToggle = { onToggleSelectTrack?.invoke(track) },
            onLongClick = onTrackLongClick?.let { onLong -> { onLong(track) } },
        )
    }
}
