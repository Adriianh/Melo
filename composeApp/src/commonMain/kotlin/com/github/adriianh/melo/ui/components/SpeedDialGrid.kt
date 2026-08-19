package com.github.adriianh.melo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.desktopScroll

@Composable
fun SpeedDialGrid(
    items: List<SearchResult>,
    onItemClick: (SearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = items.chunked(3)
    val listState = rememberLazyListState()

    CarouselScrollContainer(state = listState) {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            val columnWidth = adaptiveCardWidth(
                minCardWidth = 280.dp,
                spacing = 10.dp,
                horizontalPadding = 24.dp
            )

            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .desktopScroll(listState)
            ) {
                items(columns) { columnItems ->
                    Column(
                        modifier = Modifier.width(columnWidth),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        columnItems.forEach { item ->
                            val title: String
                            val subtitle: String
                            val artworkUrl: String?

                            when (item) {
                                is SearchResult.Song -> {
                                    title = item.track.title
                                    subtitle = item.track.artist
                                    artworkUrl = item.track.artworkUrl
                                }

                                is SearchResult.Album -> {
                                    title = item.title
                                    subtitle = item.author
                                    artworkUrl = item.artworkUrl
                                }

                                is SearchResult.Playlist -> {
                                    title = item.title
                                    subtitle = item.author
                                    artworkUrl = item.artworkUrl
                                }

                                is SearchResult.Artist -> {
                                    title = item.name
                                    subtitle = "Artista"
                                    artworkUrl = item.artworkUrl
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MeloColors.glassSurface.copy(alpha = 0.35f))
                                    .clickable { onItemClick(item) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MeloAsyncImage(
                                    url = artworkUrl,
                                    contentDescription = title,
                                    size = 48.dp,
                                    shape = RoundedCornerShape(6.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MeloType.labelMedium,
                                        color = MeloColors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MeloType.labelSmall,
                                        color = MeloColors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}