@file:Suppress("ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.search.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.controls.MeloSwipeActions
import com.github.adriianh.melo.ui.components.controls.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.track.TrackRow

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
        onSwipeRight = { onSwipeRight(track) },
        trailingActions = MeloSwipeActions.queueTrailingActions(track) { onSwipeLeft(track) },
        leadingAction = MeloSwipeActions.like(isLiked),
        enabled = !isSelectionMode,
        modifier = modifier,
    ) {
        TrackRow(
            track = track,
            onClick = onPlay,
            onMoreClick = if (isSelectionMode) null else onMoreClick,
            isSelectionMode = isSelectionMode,
            isSelected = isSelected,
            onSelectionToggle = onSelectionToggle,
            onLongClick = onLongClick,
        )
    }
}
