package com.github.adriianh.melo.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.HomeSectionType
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.components.AdaptiveLazyRow
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.SectionHeader
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.SearchUiState

@Composable
fun AllResultsContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    queueViewModel: QueueViewModel,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
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
    val allSongs = remember(uiState.summarySections) {
        uiState.summarySections
            .flatMap { it.items }
            .filterIsInstance<SearchResult.Song>()
            .map { it.track }
            .distinctBy { it.id }
    }

    val topResult = uiState.summarySections.firstOrNull()?.items?.firstOrNull()
    val otherSections = uiState.summarySections.filter { section ->
        section.type != HomeSectionType.SONGS && section.type != HomeSectionType.VIDEOS
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 760.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = searchResultContentPadding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                if (topResult != null) {
                    if (isWide) {
                        WideTopResultRow(
                            topResult = topResult,
                            allSongs = allSongs,
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            onPlaylistClick = onPlaylistClick,
                            queueViewModel = queueViewModel,
                            onMoreClick = onMoreClick,
                            onSwipeLeft = onSwipeLeft,
                            onSwipeRight = onSwipeRight,
                            isLiked = isLiked,
                            isSelectionMode = isSelectionMode,
                            selectedTrackIds = selectedTrackIds,
                            onToggleSelectTrack = onToggleSelectTrack,
                            onTrackLongClick = onTrackLongClick
                        )
                    } else {
                        NarrowTopResultColumn(
                            topResult = topResult,
                            allSongs = allSongs,
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            onPlaylistClick = onPlaylistClick,
                            queueViewModel = queueViewModel,
                            onMoreClick = onMoreClick,
                            onSwipeLeft = onSwipeLeft,
                            onSwipeRight = onSwipeRight,
                            isLiked = isLiked,
                            isSelectionMode = isSelectionMode,
                            selectedTrackIds = selectedTrackIds,
                            onToggleSelectTrack = onToggleSelectTrack,
                            onTrackLongClick = onTrackLongClick
                        )
                    }
                } else if (allSongs.isNotEmpty()) {
                    SongListSections(
                        listOf(
                            "Canciones" to allSongs.take(5)
                        ),
                        onMoreClick = onMoreClick,
                        onSwipeLeft = onSwipeLeft,
                        onSwipeRight = onSwipeRight,
                        isLiked = isLiked,
                        queueViewModel = queueViewModel,
                        isSelectionMode = isSelectionMode,
                        selectedTrackIds = selectedTrackIds,
                        onToggleSelectTrack = onToggleSelectTrack,
                        onTrackLongClick = onTrackLongClick
                    )
                }
            }

            if (allSongs.size > 4) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SearchResultSongList(
                        title = "Más canciones",
                        tracks = allSongs.drop(4).take(6),
                        spaced = true,
                        onPlay = queueViewModel::playTrack,
                        onMoreClick = onMoreClick,
                        onSwipeLeft = onSwipeLeft,
                        onSwipeRight = onSwipeRight,
                        isLiked = isLiked,
                        isSelectionMode = isSelectionMode,
                        selectedTrackIds = selectedTrackIds,
                        onToggleSelectTrack = onToggleSelectTrack,
                        onTrackLongClick = onTrackLongClick
                    )
                }
            }

            otherSections.forEach { section ->
                when (section.type) {
                    HomeSectionType.ALBUMS -> item {
                        AlbumResultCarousel(
                            section = section,
                            onAlbumClick = onAlbumClick
                        )
                    }

                    HomeSectionType.ARTISTS -> item {
                        ArtistResultCarousel(
                            section = section,
                            onArtistClick = onArtistClick
                        )
                    }

                    HomeSectionType.PLAYLISTS -> item {
                        PlaylistResultCarousel(
                            section = section,
                            onPlaylistClick = onPlaylistClick
                        )
                    }

                    HomeSectionType.MIXED -> item {
                        MixedResultSection(
                            section = section,
                            queueViewModel = queueViewModel,
                            onAlbumClick = onAlbumClick,
                            onArtistClick = onArtistClick,
                            onPlaylistClick = onPlaylistClick,
                            onMoreClick = onMoreClick,
                            onSwipeLeft = onSwipeLeft,
                            isLiked = isLiked,
                            isSelectionMode = isSelectionMode,
                            selectedTrackIds = selectedTrackIds,
                            onToggleSelectTrack = onToggleSelectTrack,
                            onTrackLongClick = onTrackLongClick
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun WideTopResultRow(
    topResult: SearchResult,
    allSongs: List<Track>,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    queueViewModel: QueueViewModel,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    isLiked: (Track) -> Boolean,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
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
                SongListSections(
                    listOf("" to allSongs.take(4)),
                    onMoreClick = onMoreClick,
                    onSwipeLeft = onSwipeLeft,
                    onSwipeRight = onSwipeRight,
                    isLiked = isLiked,
                    queueViewModel = queueViewModel,
                    isSelectionMode = isSelectionMode,
                    selectedTrackIds = selectedTrackIds,
                    onToggleSelectTrack = onToggleSelectTrack,
                    onTrackLongClick = onTrackLongClick
                )
            }
        }
    }
}

@Composable
private fun NarrowTopResultColumn(
    topResult: SearchResult,
    allSongs: List<Track>,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    queueViewModel: QueueViewModel,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    isLiked: (Track) -> Boolean,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
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
                SongListSections(
                    listOf("" to allSongs.take(4)),
                    onMoreClick = onMoreClick,
                    onSwipeLeft = onSwipeLeft,
                    onSwipeRight = onSwipeRight,
                    isLiked = isLiked,
                    queueViewModel = queueViewModel,
                    isSelectionMode = isSelectionMode,
                    selectedTrackIds = selectedTrackIds,
                    onToggleSelectTrack = onToggleSelectTrack,
                    onTrackLongClick = onTrackLongClick
                )
            }
        }
    }
}

@Composable
private fun SongListSections(
    sections: List<Pair<String, List<Track>>>,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    isLiked: (Track) -> Boolean,
    queueViewModel: QueueViewModel,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
    Column {
        sections.forEach { (title, tracks) ->
            if (title.isNotEmpty()) {
                SectionHeader(title = title)
                Spacer(modifier = Modifier.height(8.dp))
            }
            tracks.forEach { track ->
                val isSelected = track.id in selectedTrackIds
                SwipeableTrackRow(
                    track = track,
                    isLiked = isLiked(track),
                    onPlay = { queueViewModel.playTrack(track) },
                    onSwipeLeft = onSwipeLeft,
                    onSwipeRight = onSwipeRight,
                    onMoreClick = { onMoreClick(track) },
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    onSelectionToggle = { onToggleSelectTrack?.invoke(track) },
                    onLongClick = onTrackLongClick?.let { onLong -> { onLong(track) } }
                )
            }
        }
    }
}

@Composable
private fun SearchResultSongList(
    title: String,
    tracks: List<Track>,
    spaced: Boolean,
    onPlay: (Track) -> Unit,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    onSwipeRight: (Track) -> Unit,
    isLiked: (Track) -> Boolean,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
    Column {
        SectionHeader(title = title)
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = if (spaced) Arrangement.spacedBy(4.dp) else Arrangement.Top) {
            tracks.forEach { track ->
                val isSelected = track.id in selectedTrackIds
                SwipeableTrackRow(
                    track = track,
                    isLiked = isLiked(track),
                    onPlay = { onPlay(track) },
                    onSwipeLeft = onSwipeLeft,
                    onSwipeRight = onSwipeRight,
                    onMoreClick = { onMoreClick(track) },
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    onSelectionToggle = { onToggleSelectTrack?.invoke(track) },
                    onLongClick = onTrackLongClick?.let { onLong -> { onLong(track) } }
                )
            }
        }
    }
}

@Composable
private fun AlbumResultCarousel(
    section: HomeSection,
    onAlbumClick: (String) -> Unit,
) {
    val albums = section.items.filterIsInstance<SearchResult.Album>()
    if (albums.isEmpty()) return

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

@Composable
private fun ArtistResultCarousel(
    section: HomeSection,
    onArtistClick: (String) -> Unit,
) {
    val artists = section.items.filterIsInstance<SearchResult.Artist>()
    if (artists.isEmpty()) return

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

@Composable
private fun PlaylistResultCarousel(
    section: HomeSection,
    onPlaylistClick: (String) -> Unit,
) {
    val playlists = section.items.filterIsInstance<SearchResult.Playlist>()
    if (playlists.isEmpty()) return

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

@Composable
private fun MixedResultSection(
    section: HomeSection,
    queueViewModel: QueueViewModel,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onMoreClick: (Track) -> Unit,
    onSwipeLeft: (Track) -> Unit,
    isLiked: (Track) -> Boolean,
    isSelectionMode: Boolean = false,
    selectedTrackIds: Set<String> = emptySet(),
    onToggleSelectTrack: ((Track) -> Unit)? = null,
    onTrackLongClick: ((Track) -> Unit)? = null,
) {
    SectionHeader(title = section.title)
    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        section.items.forEach { item ->
            when (item) {
                is SearchResult.Song -> {
                    val isSelected = item.track.id in selectedTrackIds
                    SwipeableTrackRow(
                        track = item.track,
                        isLiked = isLiked(item.track),
                        onPlay = { queueViewModel.playTrack(item.track) },
                        onSwipeLeft = { onSwipeLeft(item.track) },
                        onMoreClick = { onMoreClick(item.track) },
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onSelectionToggle = { onToggleSelectTrack?.invoke(item.track) },
                        onLongClick = onTrackLongClick?.let { onLong -> { onLong(item.track) } }
                    )
                }

                is SearchResult.Album -> AlbumCard(
                    item.title,
                    item.author,
                    item.artworkUrl,
                    onClick = { onAlbumClick(item.id) },
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
                )
            }
        }
    }
}