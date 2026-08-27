package com.github.adriianh.melo.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.AdaptiveLazyRow
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.CategoryCard
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.components.SongFourRowCarousel
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.SearchUiState
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun ExploreContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onBrowseCategory: (String, String?) -> Unit,
    queueViewModel: QueueViewModel,
    onMoreClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (uiState.recentHistory.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Búsquedas recientes",
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                SongFourRowCarousel(
                    tracks = uiState.recentHistory,
                    onTrackClick = { track -> queueViewModel.playTrack(track) },
                    modifier = Modifier.padding(top = 8.dp),
                    onMoreClick = onMoreClick
                )
            }
        }

        uiState.exploreSections.forEach { section ->
            when (section.type) {
                HomeSectionType.SONGS -> {
                    val songs =
                        section.items.filterIsInstance<SearchResult.Song>().map { it.track }
                    if (songs.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = section.title,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            SongFourRowCarousel(
                                tracks = songs,
                                onTrackClick = { track -> queueViewModel.playTrack(track) },
                                modifier = Modifier.padding(top = 8.dp),
                                onMoreClick = onMoreClick
                            )
                        }
                    }
                }

                HomeSectionType.MIXED -> {
                    item {
                        SectionHeader(
                            title = section.title,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        AdaptiveLazyRow(
                            items = section.items,
                            minCardWidth = 140.dp,
                            spacing = 12.dp,
                            horizontalPadding = 24.dp
                        ) { item, cardWidth ->
                            when (item) {
                                is SearchResult.Playlist -> AlbumCard(
                                    item.title,
                                    item.author,
                                    item.artworkUrl,
                                    onClick = { onPlaylistClick(item.id) },
                                    cardWidth = cardWidth
                                )

                                is SearchResult.Album -> AlbumCard(
                                    item.title,
                                    item.author,
                                    item.artworkUrl,
                                    onClick = { onAlbumClick(item.id) },
                                    cardWidth = cardWidth
                                )

                                else -> {}
                            }
                        }
                    }
                }

                else -> {}
            }
        }

        if (uiState.isLoadingMoodAndGenres) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (uiState.moodAndGenres.isNotEmpty()) {
            uiState.moodAndGenres.forEach { group ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            group.title,
                            style = MeloType.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MeloColors.textPrimary,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        val columns = 5
                        group.items.chunked(columns).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { category ->
                                    CategoryCard(
                                        title = category.title,
                                        color = Color(category.stripeColor.toInt()),
                                        onClick = {
                                            onBrowseCategory(
                                                category.browseId,
                                                category.params
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(columns - rowItems.size) {
                                    Spacer(
                                        modifier = Modifier.weight(
                                            1f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
