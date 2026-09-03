package com.github.adriianh.melo.ui.search.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.ui.components.AlbumCard
import com.github.adriianh.melo.ui.components.ArtistCircle
import com.github.adriianh.melo.ui.components.VideoCard
import com.github.adriianh.melo.ui.player.QueueViewModel
import com.github.adriianh.melo.ui.search.SearchUiState

@Composable
fun AlbumsResultsContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    onAlbumClick: (String) -> Unit,
) {
    SearchResultsGrid(
        items = uiState.albumResults,
        columns = GridCells.Adaptive(150.dp),
        paddingValues = paddingValues,
        horizontalSpacing = 14.dp,
        verticalSpacing = 14.dp,
        key = { index, album -> "album_${index}_${album.id}" }
    ) { album ->
        AlbumCard(
            title = album.title,
            subtitle = album.author,
            artworkUrl = album.artworkUrl,
            onClick = { onAlbumClick(album.id) }
        )
    }
}

@Composable
fun ArtistsResultsContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchResultsGrid(
        items = uiState.artistResults,
        columns = GridCells.Adaptive(120.dp),
        paddingValues = paddingValues,
        horizontalSpacing = 16.dp,
        verticalSpacing = 16.dp,
        modifier = modifier,
        key = { index, artist -> "artist_${index}_${artist.id}" }
    ) { artist ->
        ArtistCircle(
            name = artist.name,
            artworkUrl = artist.artworkUrl,
            onClick = { onArtistClick(artist.id) },
            size = 120.dp
        )
    }
}

@Composable
fun PlaylistsResultsContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    onPlaylistClick: (String, String, String?, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchResultsGrid(
        items = uiState.playlistResults,
        columns = GridCells.Adaptive(150.dp),
        paddingValues = paddingValues,
        horizontalSpacing = 14.dp,
        verticalSpacing = 14.dp,
        modifier = modifier,
        key = { index, playlist -> "pl_${index}_${playlist.id}" }
    ) { playlist ->
        AlbumCard(
            title = playlist.title,
            subtitle = playlist.author,
            artworkUrl = playlist.artworkUrl,
            onClick = {
                onPlaylistClick(
                    playlist.id,
                    playlist.title,
                    playlist.artworkUrl,
                    playlist.author
                )
            }
        )
    }
}

@Composable
fun VideosResultsContent(
    uiState: SearchUiState,
    paddingValues: PaddingValues,
    queueViewModel: QueueViewModel,
    modifier: Modifier = Modifier,
) {
    SearchResultsGrid(
        items = uiState.videoResults,
        columns = GridCells.Adaptive(260.dp),
        paddingValues = paddingValues,
        horizontalSpacing = 14.dp,
        verticalSpacing = 14.dp,
        modifier = modifier,
        key = { index, track -> "video_${index}_${track.id}" }
    ) { track ->
        VideoCard(
            title = track.title,
            subtitle = track.artist,
            artworkUrl = track.artworkUrl,
            onClick = { queueViewModel.playTrack(track) }
        )
    }
}