package com.github.adriianh.melo.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.SearchUiState

@Composable
fun SongsResultsContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    queueViewModel: QueueViewModel,
    modifier: Modifier = Modifier,
    onMoreClick: ((Track) -> Unit)? = null,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = searchResultContentPadding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(uiState.songResults, key = { it.id }) { track ->
            val isSelected = track.id in selectedTrackIds
            TrackRow(
                track = track,
                onClick = { queueViewModel.playTrack(track) },
                onMoreClick = if (onMoreClick != null && !isSelectionMode) {
                    { onMoreClick(track) }
                } else null,
                isSelectionMode = isSelectionMode,
                isSelected = isSelected,
                onSelectionToggle = { onToggleSelectTrack?.invoke(track) },
                onLongClick = onTrackLongClick?.let { onLong -> { onLong(track) } }
            )
        }
    }
}