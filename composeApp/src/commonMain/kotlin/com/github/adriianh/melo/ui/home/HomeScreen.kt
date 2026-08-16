package com.github.adriianh.melo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HomeFeedChip
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloColors
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> SkeletonLoading(
                chips = uiState.chips,
                onChipClick = viewModel::toggleChip,
                selectedChip = uiState.selectedChip
            )

            uiState.error != null -> ErrorState(uiState.error)
            else -> HomeContent(
                uiState = uiState,
                onChipClick = viewModel::toggleChip,
                onLoadMore = viewModel::loadMore,
                queueViewModel = queueViewModel,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onChipClick: (HomeFeedChip) -> Unit,
    onLoadMore: () -> Unit,
    queueViewModel: QueueViewModel,
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.continuation != null && !uiState.isLoadingMore) {
            onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Mood / Activity Chips
        if (uiState.chips.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(uiState.chips) { chip ->
                        FilterChip(
                            selected = uiState.selectedChip == chip,
                            onClick = { onChipClick(chip) },
                            label = { Text(chip.title) }
                        )
                    }
                }
            }
        }

        uiState.sections.forEach { section ->
            item(key = section.title) {
                SectionHeader(title = section.title)
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    items(section.items) { item ->
                        when (item) {
                            is SearchResult.Song -> AlbumCard(
                                title = item.track.title,
                                subtitle = item.track.artist,
                                artworkUrl = item.track.artworkUrl,
                                onClick = { queueViewModel.playTrack(item.track) },
                            )

                            is SearchResult.Album -> AlbumCard(
                                title = item.title,
                                subtitle = item.author,
                                artworkUrl = item.artworkUrl,
                                onClick = { },
                            )

                            is SearchResult.Artist -> AlbumCard(
                                title = item.name,
                                subtitle = item.subscriberCountText ?: "",
                                artworkUrl = item.artworkUrl,
                                onClick = { },
                            )

                            is SearchResult.Playlist -> AlbumCard(
                                title = item.title,
                                subtitle = item.author,
                                artworkUrl = item.artworkUrl,
                                onClick = { },
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkeletonLoading(
    chips: List<HomeFeedChip> = emptyList(),
    onChipClick: (HomeFeedChip) -> Unit = {},
    selectedChip: HomeFeedChip? = null,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (chips.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(chips) { chip ->
                        FilterChip(
                            selected = selectedChip == chip,
                            onClick = { onChipClick(chip) },
                            label = { Text(chip.title) }
                        )
                    }
                }
            }
        } else {
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        repeat(4) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(24.dp)
                        .width(160.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MeloColors.surface2)
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    items(6) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MeloColors.surface2)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MeloColors.surface2)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MeloColors.surface2)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(error: String?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Error: $error",
            color = MaterialTheme.colorScheme.error,
        )
    }
}