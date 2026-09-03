package com.github.adriianh.melo.ui.search.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.TrackRow

@Composable
fun SwipeableTrackRow(
    track: Track,
    isLiked: Boolean,
    onPlay: () -> Unit,
    onSwipeLeft: (Track) -> Unit = {},
    onSwipeRight: (Track) -> Unit = {},
    onMoreClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionToggle: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    MeloSwipeableItem(
        onSwipeLeft = { if (!isSelectionMode) onSwipeLeft(track) },
        onSwipeRight = { if (!isSelectionMode) onSwipeRight(track) },
        swipeRightIcon = if (isLiked) Icons.Default.HeartBroken else Icons.Default.Favorite,
        modifier = modifier
    ) {
        TrackRow(
            track = track,
            onClick = onPlay,
            onMoreClick = if (isSelectionMode) null else onMoreClick,
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onSelectionToggle = onSelectionToggle,
            onLongClick = onLongClick
        )
    }
}