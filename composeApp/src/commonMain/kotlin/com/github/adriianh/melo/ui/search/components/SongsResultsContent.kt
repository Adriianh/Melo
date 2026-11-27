@file:Suppress("ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.SearchUiState

@Composable
fun SongsResultsContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    queueViewModel: QueueViewModel,
    modifier: Modifier = Modifier,
    onLoadMore: () -> Unit = {},
    onMoreClick: ((Track) -> Unit)? = null,
    onSwipeLeft: (Track) -> Unit = {},
    onSwipeRight: (Track) -> Unit = {},
    isLiked: (Track) -> Boolean = { false },
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    val hasMore = uiState.continuation != null && !uiState.isOfflineSearch
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 5
        }
    }

    LaunchedEffect(shouldLoadMore, hasMore, uiState.isLoadingMore, uiState.songResults.size) {
        if (shouldLoadMore && hasMore && !uiState.isLoadingMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = searchResultContentPadding(paddingValues),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(uiState.songResults, key = { it.id }) { track ->
            val isSelected = track.id in selectedTrackIds
            SwipeableTrackRow(
                track = track,
                isLiked = isLiked(track),
                onPlay = { queueViewModel.playTrack(track) },
                onSwipeLeft = onSwipeLeft,
                onSwipeRight = onSwipeRight,
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
        if (uiState.isLoadingMore) {
            item(key = "songs_loading_more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
