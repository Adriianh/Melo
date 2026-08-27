package com.github.adriianh.melo.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.AdaptiveLazyRow
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.MeloEmptyState
import com.github.adriianh.melo.ui.components.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.components.SuggestionSkeletonCard
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.ui.components.VideoCard
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.SearchFilterType
import com.github.adriianh.melo.ui.search.SearchUiState

@Composable
fun SearchResultsContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    queueViewModel: QueueViewModel,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    isLiked: (Track) -> Boolean,
    modifier: Modifier = Modifier
) {
    if (uiState.isSearching) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(6) {
                SuggestionSkeletonCard()
            }
        }
    } else if (!uiState.hasResults) {
        MeloEmptyState(
            title = "No se encontraron resultados",
            message = "Intenta buscar con otras palabras o cambiar de filtro",
            modifier = modifier
        )
    } else {
        when (uiState.selectedFilter) {
            SearchFilterType.ALL -> {
                val allSongs = uiState.summarySections
                    .flatMap { it.items }
                    .filterIsInstance<SearchResult.Song>()
                    .map { it.track }
                    .distinctBy { it.id }

                val topResult = uiState.summarySections.firstOrNull()?.items?.firstOrNull()
                val otherSections = uiState.summarySections.filter { section ->
                    section.type != HomeSectionType.SONGS && section.type != HomeSectionType.VIDEOS
                }

                BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                    val isWide = maxWidth >= 760.dp

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 24.dp,
                            end = 24.dp,
                            top = 8.dp,
                            bottom = paddingValues.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item {
                            if (topResult != null) {
                                if (isWide) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1.1f)) {
                                            SectionHeader(title = "Mejor resultado")
                                            Spacer(modifier = Modifier.height(10.dp))
                                            TopResultHeroCard(
                                                item = topResult,
                                                onAlbumClick = onAlbumClick,
                                                onArtistClick = onArtistClick,
                                                onPlaylistClick = onPlaylistClick,
                                                queueViewModel = queueViewModel,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        if (allSongs.isNotEmpty()) {
                                            Column(modifier = Modifier.weight(1.5f)) {
                                                SectionHeader(title = "Canciones principales")
                                                Spacer(modifier = Modifier.height(10.dp))
                                                allSongs.take(4).forEach { track ->
                                                    MeloSwipeableItem(
                                                        onSwipeLeft = { onSwipeLeft(track) },
                                                        onSwipeRight = { onSwipeRight(track) },
                                                        swipeRightIcon = if (isLiked(track)) Icons.Default.HeartBroken else Icons.Default.Favorite
                                                    ) {
                                                        TrackRow(
                                                            track = track,
                                                            onClick = {
                                                                queueViewModel.playTrack(
                                                                    track
                                                                )
                                                            },
                                                            onMoreClick = { onMoreClick(track) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column {
                                            SectionHeader(title = "Mejor resultado")
                                            Spacer(modifier = Modifier.height(10.dp))
                                            TopResultHeroCard(
                                                item = topResult,
                                                onAlbumClick = onAlbumClick,
                                                onArtistClick = onArtistClick,
                                                onPlaylistClick = onPlaylistClick,
                                                queueViewModel = queueViewModel,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        if (allSongs.isNotEmpty()) {
                                            Column {
                                                SectionHeader(title = "Canciones")
                                                Spacer(modifier = Modifier.height(8.dp))
                                                allSongs.take(4).forEach { track ->
                                                    MeloSwipeableItem(
                                                        onSwipeLeft = { onSwipeLeft(track) },
                                                        onSwipeRight = { onSwipeRight(track) },
                                                        swipeRightIcon = if (isLiked(track)) Icons.Default.HeartBroken else Icons.Default.Favorite
                                                    ) {
                                                        TrackRow(
                                                            track = track,
                                                            onClick = {
                                                                queueViewModel.playTrack(
                                                                    track
                                                                )
                                                            },
                                                            onMoreClick = { onMoreClick(track) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (allSongs.isNotEmpty()) {
                                Column {
                                    SectionHeader(title = "Canciones")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    allSongs.take(5).forEach { track ->
                                        MeloSwipeableItem(
                                            onSwipeLeft = { onSwipeLeft(track) },
                                            onSwipeRight = { onSwipeRight(track) },
                                            swipeRightIcon = if (isLiked(track)) Icons.Default.HeartBroken else Icons.Default.Favorite
                                        ) {
                                            TrackRow(
                                                track = track,
                                                onClick = { queueViewModel.playTrack(track) },
                                                onMoreClick = { onMoreClick(track) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (allSongs.size > 4) {
                            item {
                                SectionHeader(title = "Más canciones")
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    allSongs.drop(4).take(6).forEach { track ->
                                        MeloSwipeableItem(
                                            onSwipeLeft = { onSwipeLeft(track) },
                                            onSwipeRight = { onSwipeRight(track) },
                                            swipeRightIcon = if (isLiked(track)) Icons.Default.HeartBroken else Icons.Default.Favorite
                                        ) {
                                            TrackRow(
                                                track = track,
                                                onClick = { queueViewModel.playTrack(track) },
                                                onMoreClick = { onMoreClick(track) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        otherSections.forEach { section ->
                            when (section.type) {
                                HomeSectionType.ALBUMS -> {
                                    item {
                                        val albums =
                                            section.items.filterIsInstance<SearchResult.Album>()
                                        if (albums.isNotEmpty()) {
                                            SectionHeader(title = section.title.ifBlank { "Álbumes" })
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AdaptiveLazyRow(
                                                items = albums,
                                                minCardWidth = 140.dp,
                                                spacing = 12.dp,
                                                horizontalPadding = 0.dp
                                            ) { album, width ->
                                                AlbumCard(
                                                    title = album.title,
                                                    subtitle = album.author,
                                                    artworkUrl = album.artworkUrl,
                                                    cardWidth = width,
                                                    onClick = { onAlbumClick(album.id) }
                                                )
                                            }
                                        }
                                    }
                                }

                                HomeSectionType.ARTISTS -> {
                                    item {
                                        val artists =
                                            section.items.filterIsInstance<SearchResult.Artist>()
                                        if (artists.isNotEmpty()) {
                                            SectionHeader(title = section.title.ifBlank { "Artistas" })
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AdaptiveLazyRow(
                                                items = artists,
                                                minCardWidth = 110.dp,
                                                spacing = 12.dp,
                                                horizontalPadding = 0.dp
                                            ) { artist, width ->
                                                ArtistCircle(
                                                    name = artist.name,
                                                    artworkUrl = artist.artworkUrl,
                                                    onClick = { onArtistClick(artist.id) },
                                                    size = width
                                                )
                                            }
                                        }
                                    }
                                }

                                HomeSectionType.PLAYLISTS -> {
                                    item {
                                        val playlists =
                                            section.items.filterIsInstance<SearchResult.Playlist>()
                                        if (playlists.isNotEmpty()) {
                                            SectionHeader(title = section.title.ifBlank { "Playlists" })
                                            Spacer(modifier = Modifier.height(8.dp))
                                            AdaptiveLazyRow(
                                                items = playlists,
                                                minCardWidth = 140.dp,
                                                spacing = 12.dp,
                                                horizontalPadding = 0.dp
                                            ) { playlist, width ->
                                                AlbumCard(
                                                    title = playlist.title,
                                                    subtitle = playlist.author,
                                                    artworkUrl = playlist.artworkUrl,
                                                    cardWidth = width,
                                                    onClick = { onPlaylistClick(playlist.id) }
                                                )
                                            }
                                        }
                                    }
                                }

                                HomeSectionType.MIXED -> {
                                    item {
                                        SectionHeader(title = section.title)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            section.items.forEach { item ->
                                                when (item) {
                                                    is SearchResult.Song -> {
                                                        MeloSwipeableItem(
                                                            onSwipeLeft = { onSwipeLeft(item.track) },
                                                            onSwipeRight = { /* TODO */ }
                                                        ) {
                                                            TrackRow(
                                                                item.track,
                                                                onClick = {
                                                                    queueViewModel.playTrack(
                                                                        item.track
                                                                    )
                                                                },
                                                                onMoreClick = { onMoreClick(item.track) }
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
                                                        onClick = { onPlaylistClick(item.id) },
                                                        cardWidth = 140.dp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }
                    }
                }
            }

            SearchFilterType.SONGS -> {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.songResults) { track ->
                        TrackRow(track = track, onClick = { queueViewModel.playTrack(track) })
                    }
                }
            }

            SearchFilterType.ALBUMS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(
                        uiState.albumResults,
                        key = { index, item -> "album_${index}_${item.id}" }) { _, album ->
                        AlbumCard(
                            title = album.title,
                            subtitle = album.author,
                            artworkUrl = album.artworkUrl,
                            onClick = { onAlbumClick(album.id) }
                        )
                    }
                }
            }

            SearchFilterType.ARTISTS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(120.dp),
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(
                        uiState.artistResults,
                        key = { index, item -> "artist_${index}_${item.id}" }) { _, artist ->
                        ArtistCircle(
                            name = artist.name,
                            artworkUrl = artist.artworkUrl,
                            onClick = { onArtistClick(artist.id) },
                            size = 120.dp
                        )
                    }
                }
            }

            SearchFilterType.PLAYLISTS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(
                        uiState.playlistResults,
                        key = { index, item -> "pl_${index}_${item.id}" }) { _, playlist ->
                        AlbumCard(
                            title = playlist.title,
                            subtitle = playlist.author,
                            artworkUrl = playlist.artworkUrl,
                            onClick = { onPlaylistClick(playlist.id) }
                        )
                    }
                }
            }

            SearchFilterType.VIDEOS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(260.dp),
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(
                        uiState.videoResults,
                        key = { index, track -> "video_${index}_${track.id}" }
                    ) { _, track ->
                        VideoCard(
                            title = track.title,
                            subtitle = track.artist,
                            artworkUrl = track.artworkUrl,
                            onClick = { queueViewModel.playTrack(track) }
                        )
                    }
                }
            }
        }
    }
}
