package com.github.adriianh.melo.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.Track
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
    queueViewModel: QueueViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = {
                            Text(
                                "Buscar canciones, artistas, álbumes...",
                                style = MeloType.body,
                                color = MeloColors.textMuted
                            )
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MeloColors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = viewModel::clearQuery) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Limpiar",
                                        tint = MeloColors.textMuted
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MeloColors.surface1,
                            unfocusedContainerColor = MeloColors.surface1,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MeloColors.textPrimary,
                            unfocusedTextColor = MeloColors.textPrimary,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MeloColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MeloColors.textPrimary
                )
            )
        }
    ) { paddingValues ->
        if (uiState.query.isNotBlank()) {
            if (uiState.isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
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
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
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
