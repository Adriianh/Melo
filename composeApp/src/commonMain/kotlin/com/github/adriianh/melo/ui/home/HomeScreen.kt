package com.github.adriianh.melo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HomeFeedChip
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.components.SongFourRowCarousel
import com.github.adriianh.melo.ui.components.SpeedDialGrid
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.components.VideoCard
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        HomeSearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            onClear = viewModel::clearSearch,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.searchQuery.isNotBlank() -> {
                    SearchResultsView(
                        isSearching = uiState.isSearching,
                        results = uiState.searchResults,
                        onTrackClick = queueViewModel::playTrack
                    )
                }

                uiState.isLoading -> {
                    SkeletonLoading(
                        chips = uiState.chips,
                        onChipClick = viewModel::toggleChip,
                        selectedChip = uiState.selectedChip
                    )
                }

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
}

@Composable
private fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "¿Qué quieres escuchar?",
                style = MeloType.body,
                color = MeloColors.textMuted
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Buscar",
                tint = MeloColors.textMuted
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Limpiar",
                        tint = MeloColors.textMuted
                    )
                }
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MeloColors.surface2,
            unfocusedContainerColor = MeloColors.surface1,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = MeloColors.textPrimary,
            unfocusedTextColor = MeloColors.textPrimary,
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun SearchResultsView(
    isSearching: Boolean,
    results: List<Track>,
    onTrackClick: (Track) -> Unit,
) {
    if (isSearching) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No se encontraron resultados",
                style = MeloType.body,
                color = MeloColors.textMuted
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    "Canciones",
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(results) { track ->
                TrackRow(
                    track = track,
                    onClick = { onTrackClick(track) }
                )
            }
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (uiState.chips.isNotEmpty()) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    items(uiState.chips) { chip ->
                        FilterChip(
                            selected = uiState.selectedChip == chip,
                            onClick = { onChipClick(chip) },
                            label = { Text(chip.title, style = MeloType.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MeloColors.surface1,
                                labelColor = MeloColors.textPrimary,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.selectedChip == chip,
                                borderColor = MeloColors.borderStrong,
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        uiState.sections.forEach { section ->
            when (section.type) {
                HomeSectionType.SONGS -> {
                    val tracks =
                        section.items.filterIsInstance<SearchResult.Song>().map { it.track }
                    item(key = section.title) {
                        SectionHeader(title = section.title)
                        SongFourRowCarousel(
                            tracks = tracks,
                            onTrackClick = queueViewModel::playTrack
                        )
                    }
                }

                HomeSectionType.ALBUMS -> {
                    item(key = section.title) {
                        SectionHeader(title = section.title)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(section.items) { item ->
                                when (item) {
                                    is SearchResult.Album -> AlbumCard(
                                        title = item.title,
                                        subtitle = item.author,
                                        artworkUrl = item.artworkUrl,
                                        onClick = {}
                                    )

                                    is SearchResult.Song -> AlbumCard(
                                        title = item.track.title,
                                        subtitle = item.track.artist,
                                        artworkUrl = item.track.artworkUrl,
                                        onClick = { queueViewModel.playTrack(item.track) }
                                    )

                                    is SearchResult.Playlist -> AlbumCard(
                                        title = item.title,
                                        subtitle = item.author,
                                        artworkUrl = item.artworkUrl,
                                        onClick = {}
                                    )

                                    is SearchResult.Artist -> ArtistCircle(
                                        name = item.name,
                                        artworkUrl = item.artworkUrl,
                                        onClick = {}
                                    )
                                }
                            }
                        }
                    }
                }

                HomeSectionType.PLAYLISTS -> {
                    item(key = section.title) {
                        SectionHeader(title = section.title)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(section.items) { item ->
                                when (item) {
                                    is SearchResult.Playlist -> AlbumCard(
                                        title = item.title,
                                        subtitle = item.author,
                                        artworkUrl = item.artworkUrl,
                                        onClick = {}
                                    )

                                    is SearchResult.Album -> AlbumCard(
                                        title = item.title,
                                        subtitle = item.author,
                                        artworkUrl = item.artworkUrl,
                                        onClick = {}
                                    )

                                    is SearchResult.Song -> AlbumCard(
                                        title = item.track.title,
                                        subtitle = item.track.artist,
                                        artworkUrl = item.track.artworkUrl,
                                        onClick = { queueViewModel.playTrack(item.track) }
                                    )

                                    is SearchResult.Artist -> ArtistCircle(
                                        name = item.name,
                                        artworkUrl = item.artworkUrl,
                                        onClick = {}
                                    )
                                }
                            }
                        }
                    }
                }

                HomeSectionType.ARTISTS -> {
                    item(key = section.title) {
                        SectionHeader(title = section.title)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(section.items) { item ->
                                when (item) {
                                    is SearchResult.Artist -> ArtistCircle(
                                        name = item.name,
                                        artworkUrl = item.artworkUrl,
                                        onClick = {}
                                    )

                                    is SearchResult.Song -> AlbumCard(
                                        title = item.track.title,
                                        subtitle = item.track.artist,
                                        artworkUrl = item.track.artworkUrl,
                                        onClick = { queueViewModel.playTrack(item.track) }
                                    )

                                    is SearchResult.Album -> AlbumCard(
                                        title = item.title,
                                        subtitle = item.author,
                                        artworkUrl = item.artworkUrl,
                                        onClick = {}
                                    )

                                    is SearchResult.Playlist -> AlbumCard(
                                        title = item.title,
                                        subtitle = item.author,
                                        artworkUrl = item.artworkUrl,
                                        onClick = {}
                                    )
                                }
                            }
                        }
                    }
                }

                HomeSectionType.VIDEOS -> {
                    item(key = section.title) {
                        SectionHeader(title = section.title)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(section.items) { item ->
                                when (item) {
                                    is SearchResult.Song -> VideoCard(
                                        title = item.track.title,
                                        subtitle = item.track.artist,
                                        artworkUrl = item.track.artworkUrl,
                                        onClick = { queueViewModel.playTrack(item.track) }
                                    )

                                    is SearchResult.Album -> VideoCard(
                                        title = item.title,
                                        subtitle = item.author,
                                        artworkUrl = item.artworkUrl,
                                        onClick = {}
                                    )

                                    is SearchResult.Playlist -> VideoCard(
                                        title = item.title,
                                        subtitle = item.author,
                                        artworkUrl = item.artworkUrl,
                                        onClick = {}
                                    )

                                    is SearchResult.Artist -> ArtistCircle(
                                        name = item.name,
                                        artworkUrl = item.artworkUrl,
                                        onClick = {}
                                    )
                                }
                            }
                        }
                    }
                }

                HomeSectionType.MIXED -> {
                    item(key = section.title) {
                        SectionHeader(title = section.title)
                        SpeedDialGrid(
                            items = section.items,
                            onItemClick = { item ->
                                if (item is SearchResult.Song) queueViewModel.playTrack(item.track)
                            }
                        )
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
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
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(6) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
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