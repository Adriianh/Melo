@file:Suppress("ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.home.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.home_search_empty
import com.github.adriianh.melo.resources.section_songs
import com.github.adriianh.melo.ui.components.controls.LocalSwipeRevealGroup
import com.github.adriianh.melo.ui.components.controls.MeloSwipeActions
import com.github.adriianh.melo.ui.components.controls.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.controls.rememberSwipeRevealGroup
import com.github.adriianh.melo.ui.components.track.TrackInteractionState
import com.github.adriianh.melo.ui.components.track.TrackRow
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeSearchResultsView(
    activeAccent: Color,
    isSearching: Boolean,
    likedSongs: List<Track>,
    results: List<Track>,
    queueViewModel: QueueViewModel,
    onMoreClick: (Track) -> Unit,
    interaction: TrackInteractionState,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
    if (isSearching) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (results.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(Res.string.home_search_empty),
                style = MeloType.body,
                color = MeloColors.textMuted,
            )
        }
    } else {
        val listState = rememberLazyListState()
        val shouldLoadMore by remember {
            derivedStateOf {
                val totalItems = listState.layoutInfo.totalItemsCount
                val lastVisibleIndex =
                    listState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: 0
                totalItems > 1 && lastVisibleIndex >= totalItems - 5
            }
        }
        LaunchedEffect(shouldLoadMore, hasMore, isLoadingMore, results.size) {
            if (shouldLoadMore && hasMore && !isLoadingMore) {
                onLoadMore()
            }
        }

        val revealGroup = rememberSwipeRevealGroup()
        val queuedTrackIds = interaction.rememberQueuedTrackIds()
        CompositionLocalProvider(LocalSwipeRevealGroup provides revealGroup) {
            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Text(
                        stringResource(Res.string.section_songs),
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(results, key = { it.id }) { track ->
                    val isLiked = likedSongs.any { it.id == track.id }
                    val isSelected = track.id in selectedTrackIds
                    MeloSwipeableItem(
                        onSwipeRight = {
                            interaction.showToggledLikeSnackbar(track, isLiked, activeAccent)
                        },
                        trailingActions =
                            MeloSwipeActions.queueTrailingActions(
                                isQueued = track.id in queuedTrackIds,
                                onQueue = { interaction.showAddedToQueueSnackbar(track, activeAccent) },
                                onRemove = { interaction.showRemovedFromQueueSnackbar(track, activeAccent) },
                            ),
                        leadingAction = MeloSwipeActions.like(isLiked),
                        enabled = !isSelectionMode,
                    ) {
                        TrackRow(
                            track = track,
                            onClick = { queueViewModel.playTrack(track) },
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
                if (isLoadingMore) {
                    item(key = "home_search_loading_more") {
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
    }
}
