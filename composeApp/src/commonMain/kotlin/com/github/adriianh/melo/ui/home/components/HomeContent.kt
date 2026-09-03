package com.github.adriianh.melo.ui.home.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HomeFeedChip
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.AdaptiveLazyRow
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.components.SongFourRowCarousel
import com.github.adriianh.melo.ui.components.SpeedDialGrid
import com.github.adriianh.melo.ui.components.VideoCard
import com.github.adriianh.melo.ui.home.HomeUiState
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.desktopScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    uiState: HomeUiState,
    onChipClick: (HomeFeedChip) -> Unit,
    onLoadMore: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    queueViewModel: QueueViewModel,
    paddingValues: PaddingValues,
    onMoreClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
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
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        )
    ) {
        if (uiState.isOfflineFeed && uiState.sections.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MeloColors.textMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sin música disponible sin conexión",
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Descarga canciones o añade carpetas locales en la Biblioteca para escuchar cuando no tengas internet.",
                        style = MeloType.body,
                        color = MeloColors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (uiState.chips.isNotEmpty()) {
            item {
                val chipState = rememberLazyListState()
                LazyRow(
                    state = chipState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .desktopScroll(chipState)
                ) {
                    items(uiState.chips) { chip ->
                        FilterChip(
                            selected = uiState.selectedChip == chip,
                            onClick = { onChipClick(chip) },
                            label = { Text(chip.title, style = MeloType.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MeloColors.glassSurface.copy(alpha = 0.5f),
                                labelColor = MeloColors.textPrimary,
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.25f
                                ),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.selectedChip == chip,
                                borderColor = MeloColors.glassBorder,
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.blur(0.dp)
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
                            onTrackClick = queueViewModel::playTrack,
                            onMoreClick = onMoreClick,
                            isSelectionMode = isSelectionMode,
                            selectedTrackIds = selectedTrackIds,
                            onToggleSelectTrack = onToggleSelectTrack,
                            onTrackLongClick = onTrackLongClick
                        )
                    }
                }

                HomeSectionType.ALBUMS -> {
                    item(key = section.title) {
                        SectionHeader(title = section.title)
                        AdaptiveLazyRow(
                            items = section.items,
                            minCardWidth = 140.dp,
                            spacing = 12.dp
                        ) { item, cardWidth ->
                            when (item) {
                                is SearchResult.Album -> AlbumCard(
                                    title = item.title,
                                    subtitle = item.author,
                                    artworkUrl = item.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { onAlbumClick(item.id) }
                                )

                                is SearchResult.Song -> AlbumCard(
                                    title = item.track.title,
                                    subtitle = item.track.artist,
                                    artworkUrl = item.track.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { queueViewModel.playTrack(item.track) }
                                )

                                is SearchResult.Playlist -> AlbumCard(
                                    title = item.title,
                                    subtitle = item.author,
                                    artworkUrl = item.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { onPlaylistClick(item.id) }
                                )

                                is SearchResult.Artist -> ArtistCircle(
                                    name = item.name,
                                    artworkUrl = item.artworkUrl,
                                    size = cardWidth,
                                    onClick = { onArtistClick(item.id) }
                                )
                            }
                        }
                    }
                }

                HomeSectionType.PLAYLISTS -> {
                    item(key = section.title) {
                        SectionHeader(title = section.title)
                        AdaptiveLazyRow(
                            items = section.items,
                            minCardWidth = 140.dp,
                            spacing = 14.dp
                        ) { item, cardWidth ->
                            when (item) {
                                is SearchResult.Playlist -> AlbumCard(
                                    title = item.title,
                                    subtitle = item.author,
                                    artworkUrl = item.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { onPlaylistClick(item.id) }
                                )

                                is SearchResult.Album -> AlbumCard(
                                    title = item.title,
                                    subtitle = item.author,
                                    artworkUrl = item.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { onAlbumClick(item.id) }
                                )

                                is SearchResult.Song -> AlbumCard(
                                    title = item.track.title,
                                    subtitle = item.track.artist,
                                    artworkUrl = item.track.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { queueViewModel.playTrack(item.track) }
                                )

                                is SearchResult.Artist -> ArtistCircle(
                                    name = item.name,
                                    artworkUrl = item.artworkUrl,
                                    size = cardWidth,
                                    onClick = { onArtistClick(item.id) }
                                )
                            }
                        }
                    }
                }

                HomeSectionType.ARTISTS -> {
                    item(key = section.title) {
                        SectionHeader(title = section.title)
                        AdaptiveLazyRow(
                            items = section.items,
                            minCardWidth = 140.dp,
                            spacing = 12.dp
                        ) { item, cardWidth ->
                            when (item) {
                                is SearchResult.Artist -> ArtistCircle(
                                    name = item.name,
                                    artworkUrl = item.artworkUrl,
                                    size = cardWidth,
                                    onClick = { onArtistClick(item.id) }
                                )

                                is SearchResult.Song -> AlbumCard(
                                    title = item.track.title,
                                    subtitle = item.track.artist,
                                    artworkUrl = item.track.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { queueViewModel.playTrack(item.track) }
                                )

                                is SearchResult.Album -> AlbumCard(
                                    title = item.title,
                                    subtitle = item.author,
                                    artworkUrl = item.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { onAlbumClick(item.id) }
                                )

                                is SearchResult.Playlist -> AlbumCard(
                                    title = item.title,
                                    subtitle = item.author,
                                    artworkUrl = item.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { onPlaylistClick(item.id) }
                                )
                            }
                        }
                    }
                }

                HomeSectionType.VIDEOS -> {
                    item(key = section.title) {
                        SectionHeader(title = section.title)
                        AdaptiveLazyRow(
                            items = section.items,
                            minCardWidth = 220.dp,
                            spacing = 12.dp
                        ) { item, cardWidth ->
                            when (item) {
                                is SearchResult.Song -> VideoCard(
                                    title = item.track.title,
                                    subtitle = item.track.artist,
                                    artworkUrl = item.track.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { queueViewModel.playTrack(item.track) }
                                )

                                is SearchResult.Album -> VideoCard(
                                    title = item.title,
                                    subtitle = item.author,
                                    artworkUrl = item.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { onAlbumClick(item.id) }
                                )

                                is SearchResult.Playlist -> VideoCard(
                                    title = item.title,
                                    subtitle = item.author,
                                    artworkUrl = item.artworkUrl,
                                    cardWidth = cardWidth,
                                    onClick = { onPlaylistClick(item.id) }
                                )

                                is SearchResult.Artist -> ArtistCircle(
                                    name = item.name,
                                    artworkUrl = item.artworkUrl,
                                    size = cardWidth,
                                    onClick = { onArtistClick(item.id) }
                                )
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
                                when (item) {
                                    is SearchResult.Song -> queueViewModel.playTrack(item.track)
                                    is SearchResult.Album -> onAlbumClick(item.id)
                                    is SearchResult.Playlist -> onPlaylistClick(item.id)
                                    is SearchResult.Artist -> onArtistClick(item.id)
                                }
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