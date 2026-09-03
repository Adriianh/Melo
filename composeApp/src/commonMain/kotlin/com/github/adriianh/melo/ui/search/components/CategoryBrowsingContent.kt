package com.github.adriianh.melo.ui.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.SearchUiState
import com.github.adriianh.melo.util.MeloType

@Composable
fun CategoryBrowsingContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String, String, String?, String) -> Unit,
    queueViewModel: QueueViewModel,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    isLiked: (Track) -> Boolean,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
    if (uiState.isBrowsingCategory) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        val browseResult = uiState.browseCategoryResult
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable { onBack() }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Volver", style = MeloType.body, color = MaterialTheme.colorScheme.primary)
                }
            }

            browseResult?.sections?.forEach { section ->
                section.title?.let { item { SectionHeader(title = it) } }
                items(section.items) { item ->
                    when (item) {
                        is SearchResult.Song -> {
                            val isSelected = item.track.id in selectedTrackIds
                            MeloSwipeableItem(
                                onSwipeLeft = { if (!isSelectionMode) onSwipeLeft(item.track) },
                                onSwipeRight = { if (!isSelectionMode) onSwipeRight(item.track) },
                                swipeRightIcon = if (isLiked(item.track)) Icons.Default.HeartBroken else Icons.Default.Favorite
                            ) {
                                TrackRow(
                                    track = item.track,
                                    onClick = { queueViewModel.playTrack(item.track) },
                                    onMoreClick = if (isSelectionMode) null else {
                                        { onMoreClick(item.track) }
                                    },
                                    isSelectionMode = isSelectionMode,
                                    isSelected = isSelected,
                                    onSelectionToggle = { onToggleSelectTrack?.invoke(item.track) },
                                    onLongClick = onTrackLongClick?.let { onLong -> { onLong(item.track) } }
                                )
                            }
                        }

                        is SearchResult.Album -> AlbumCard(
                            item.title,
                            item.author,
                            item.artworkUrl,
                            onClick = { onAlbumClick(item.id) },
                            cardWidth = 140.dp
                        )

                        is SearchResult.Artist -> ArtistCircle(
                            item.name,
                            item.artworkUrl,
                            onClick = { onArtistClick(item.id) },
                            size = 80.dp
                        )

                        is SearchResult.Playlist -> AlbumCard(
                            item.title,
                            item.author,
                            item.artworkUrl,
                            onClick = {
                                onPlaylistClick(
                                    item.id,
                                    item.title,
                                    item.artworkUrl,
                                    item.author
                                )
                            },
                            cardWidth = 140.dp
                        )
                    }
                }
            }
        }
    }
}
