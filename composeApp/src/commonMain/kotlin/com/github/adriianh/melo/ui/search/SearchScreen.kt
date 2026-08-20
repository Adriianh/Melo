package com.github.adriianh.melo.ui.search

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.AdaptiveLazyRow
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.components.SongFourRowCarousel
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onOpenSettings: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: SearchViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SearchTopBar(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            onClear = viewModel::clearQuery,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            if (uiState.query.isNotBlank()) {
                if (uiState.isSearching) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (uiState.results.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No se encontraron resultados",
                            style = MeloType.body,
                            color = MeloColors.textMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 24.dp,
                            end = 24.dp,
                            top = 8.dp,
                            bottom = paddingValues.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.results) { track ->
                            TrackRow(
                                track = track,
                                onClick = { queueViewModel.playTrack(track) }
                            )
                        }
                    }
                }
            } else {
                if (uiState.isLoadingExplore) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = paddingValues.calculateBottomPadding() + 32.dp
                        )
                    ) {
                        uiState.exploreSections.forEach { section ->
                            when (section.type) {
                                HomeSectionType.SONGS -> {
                                    val tracks =
                                        section.items.filterIsInstance<SearchResult.Song>()
                                            .map { it.track }
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

                                                else -> {}
                                            }
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Buscar canciones, artistas, álbumes...",
                    style = MeloType.body,
                    color = MeloColors.textMuted
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MeloColors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Limpiar",
                                tint = MeloColors.textMuted
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Cuenta",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MeloColors.glassSurface,
                unfocusedContainerColor = MeloColors.glassSurface.copy(alpha = 0.45f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = MeloColors.textPrimary,
                unfocusedTextColor = MeloColors.textPrimary,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .blur(if (query.isEmpty()) 0.dp else 0.dp)
                .drawBehind {}
        )
    }
}
