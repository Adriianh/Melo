package com.github.adriianh.melo.ui.library

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformAsyncImage
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onLoginClick: () -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(0.dp),
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    if (!state.isLoggedIn) {
        NotLoggedInLibrary(onLoginClick = onLoginClick)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(
                top = 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = state.profile?.name?.take(1)?.uppercase() ?: "Y",
                            style = MeloType.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MeloColors.textPrimary
                        )
                    }
                }
                Column {
                    Text(
                        text = state.profile?.name ?: "Mi Biblioteca",
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary
                    )
                    if (state.profile?.channelHandle != null) {
                        Text(
                            text = state.profile?.channelHandle.orEmpty(),
                            style = MeloType.labelSmall,
                            color = MeloColors.textSecondary
                        )
                    }
                }
            }

            IconButton(onClick = viewModel::refreshAll) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(LibraryTab.entries.toTypedArray()) { tab ->
                val selected = state.selectedTab == tab
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.selectTab(tab) },
                    label = { Text(tab.label, style = MeloType.labelMedium) },
                    leadingIcon = {
                        val icon = when (tab) {
                            LibraryTab.PLAYLISTS -> Icons.AutoMirrored.Filled.PlaylistPlay
                            LibraryTab.LIKED -> Icons.Default.Favorite
                            LibraryTab.ARTISTS -> Icons.Default.Person
                            LibraryTab.ALBUMS -> Icons.Default.Album
                            LibraryTab.HISTORY -> Icons.Default.History
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MeloColors.surface1,
                        labelColor = MeloColors.textPrimary,
                        iconColor = MeloColors.textSecondary,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MeloColors.borderStrong,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            when (state.selectedTab) {
                LibraryTab.PLAYLISTS -> PlaylistsContent(
                    playlists = state.playlists,
                    onPlaylistClick = onPlaylistClick
                )

                LibraryTab.LIKED -> LikedSongsContent(
                    songs = state.likedSongs,
                    onPlayTrack = { track ->
                        viewModel.playTrack(track, state.likedSongs)
                    }
                )

                LibraryTab.ARTISTS -> ArtistsContent(
                    artists = state.artists,
                    onArtistClick = onArtistClick
                )

                LibraryTab.ALBUMS -> AlbumsContent(
                    albums = state.albums,
                    onAlbumClick = onAlbumClick
                )

                LibraryTab.HISTORY -> HistoryContent(
                    history = state.history,
                    onPlayTrack = { entry ->
                        viewModel.playTrack(entry.track)
                    }
                )
            }
        }
    }
}

@Composable
private fun NotLoggedInLibrary(onLoginClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tu Biblioteca de YouTube Music",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Inicia sesión para sincronizar y reproducir tus playlists guardadas, canciones con 'Me Gusta', artistas favoritos e historial.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onLoginClick) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Iniciar sesión")
                }
            }
        }
    }
}

@Composable
private fun PlaylistsContent(
    playlists: List<SearchResult.Playlist>,
    onPlaylistClick: (String) -> Unit,
) {
    if (playlists.isEmpty()) {
        EmptyLibrarySection("No se encontraron playlists en tu cuenta.")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(playlists) { playlist ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlaylistClick(playlist.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    PlatformAsyncImage(
                        url = playlist.artworkUrl,
                        contentDescription = playlist.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        size = 140.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = playlist.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${playlist.trackCount ?: 0} canciones • ${playlist.author}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LikedSongsContent(
    songs: List<Track>,
    onPlayTrack: (Track) -> Unit,
) {
    if (songs.isEmpty()) {
        EmptyLibrarySection("Aún no tienes canciones en tus 'Me Gusta'.")
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(songs) { track ->
            ListItem(
                headlineContent = {
                    Text(
                        track.title,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    PlatformAsyncImage(
                        url = track.artworkUrl,
                        contentDescription = track.title,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                        size = 48.dp,
                        shape = RoundedCornerShape(6.dp)
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPlayTrack(track) }
            )
        }
    }
}

@Composable
private fun ArtistsContent(
    artists: List<SearchResult.Artist>,
    onArtistClick: (String) -> Unit,
) {
    if (artists.isEmpty()) {
        EmptyLibrarySection("No sigues a ningún artista actualmente.")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(130.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(artists) { artist ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onArtistClick(artist.id) }
                    .padding(4.dp)
            ) {
                PlatformAsyncImage(
                    url = artist.artworkUrl,
                    contentDescription = artist.name,
                    modifier = Modifier.size(100.dp).clip(CircleShape),
                    size = 100.dp,
                    shape = CircleShape
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = artist.name,
                    style = MeloType.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AlbumsContent(
    albums: List<SearchResult.Album>,
    onAlbumClick: (String) -> Unit,
) {
    if (albums.isEmpty()) {
        EmptyLibrarySection("No tienes álbumes guardados.")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums) { album ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAlbumClick(album.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    PlatformAsyncImage(
                        url = album.artworkUrl,
                        contentDescription = album.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        size = 130.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryContent(
    history: List<HistoryEntry>,
    onPlayTrack: (HistoryEntry) -> Unit,
) {
    if (history.isEmpty()) {
        EmptyLibrarySection("No hay reproducciones recientes registradas.")
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(history) { entry ->
            ListItem(
                headlineContent = {
                    Text(
                        entry.track.title,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        entry.track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    PlatformAsyncImage(
                        url = entry.track.artworkUrl,
                        contentDescription = entry.track.title,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                        size = 48.dp,
                        shape = RoundedCornerShape(6.dp)
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onPlayTrack(entry) }
            )
        }
    }
}

@Composable
private fun EmptyLibrarySection(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}