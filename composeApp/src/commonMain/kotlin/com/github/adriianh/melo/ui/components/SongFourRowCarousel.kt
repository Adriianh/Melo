package com.github.adriianh.melo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.util.desktopScroll

@Composable
fun SongFourRowCarousel(
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
    val columns = remember(tracks) { tracks.chunked(4) }

    CarouselScrollContainer(state = listState) {
        if (containerWidth != null) {
            val columnWidth = remember(containerWidth) {
                calculateAdaptiveCardWidth(
                    containerWidth = containerWidth,
                    minCardWidth = 320.dp,
                    spacing = 12.dp,
                    horizontalPadding = 24.dp
                )
            }

            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = modifier
                    .fillMaxWidth()
                    .desktopScroll(listState)
            ) {
                items(
                    items = columns,
                    key = { col -> col.firstOrNull()?.id ?: col.hashCode() },
                    contentType = { "song_column" }
                ) { columnTracks ->
                    Column(
                        modifier = Modifier.width(columnWidth),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        columnTracks.forEach { track ->
                            val isSelected = track.id in selectedTrackIds
                            TrackRow(
                                track = track,
                                onClick = { onTrackClick(track) },
                                showGlassBackground = true,
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
            }
        } else {
            BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
                val columnWidth = calculateAdaptiveCardWidth(
                    containerWidth = maxWidth,
                    minCardWidth = 320.dp,
                    spacing = 12.dp,
                    horizontalPadding = 24.dp
                )

                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .desktopScroll(listState)
                ) {
                    items(
                        items = columns,
                        key = { col -> col.firstOrNull()?.id ?: col.hashCode() },
                        contentType = { "song_column" }
                    ) { columnTracks ->
                        Column(
                            modifier = Modifier.width(columnWidth),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            columnTracks.forEach { track ->
                                val isSelected = track.id in selectedTrackIds
                                TrackRow(
                                    track = track,
                                    onClick = { onTrackClick(track) },
                                    showGlassBackground = true,
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
                }
            }
        }
    }
}