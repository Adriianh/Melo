package com.github.adriianh.melo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.util.desktopScroll

@Composable
fun SongFourRowCarousel(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = tracks.chunked(4)
    val listState = rememberLazyListState()

    CarouselScrollContainer(state = listState) {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val columnWidth = adaptiveCardWidth(
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
                items(columns) { columnTracks ->
                    Column(
                        modifier = Modifier.width(columnWidth),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        columnTracks.forEach { track ->
                            TrackRow(
                                track = track,
                                onClick = { onTrackClick(track) },
                                showGlassBackground = true
                            )
                        }
                    }
                }
            }
        }
    }
}