package com.github.adriianh.melo.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.ui.library.LibraryViewMode

@Composable
fun PlaylistsTabContent(
    customPlaylists: List<Playlist>,
    remotePlaylists: List<SearchResult.Playlist>,
    userArtists: List<String> = emptyList(),
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlaylistClick: (String, String, String?, String) -> Unit,
    modifier: Modifier = Modifier,
    viewMode: LibraryViewMode = LibraryViewMode.GRID,
    isOffline: Boolean = false,
    offlineCustomPlaylistIds: Set<Long> = emptySet(),
    offlineRemotePlaylistIds: Set<String> = emptySet(),
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = "Nueva playlist",
            placeholder = "Nombre de la playlist",
            confirmLabel = "Crear",
            existingPlaylistNames = customPlaylists.map { it.name } + remotePlaylists.map { it.title },
            userArtists = userArtists,
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    playlistToRename?.let { target ->
        PlaylistNameDialog(
            title = "Renombrar playlist",
            placeholder = "Nuevo nombre",
            confirmLabel = "Guardar",
            initialValue = target.name,
            onConfirm = { newName ->
                onRenamePlaylist(target.id, newName)
                playlistToRename = null
            },
            onDismiss = { playlistToRename = null },
        )
    }

    playlistToDelete?.let { target ->
        PlaylistDeleteDialog(
            playlistName = target.name,
            onConfirm = {
                onDeletePlaylist(target.id)
                playlistToDelete = null
            },
            onDismiss = { playlistToDelete = null },
        )
    }

    if (viewMode == LibraryViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            item(key = "create_playlist_card") {
                CreatePlaylistGridCard(onClick = { showCreateDialog = true })
            }

            items(customPlaylists, key = { "custom_${it.id}" }) { playlist ->
                val hasDownloaded = isOffline && playlist.id in offlineCustomPlaylistIds
                val isAvailable = !isOffline || hasDownloaded
                CustomPlaylistGridCard(
                    playlist = playlist,
                    onClick = {
                        onPlaylistClick(
                            "local:${playlist.id}",
                            playlist.name,
                            playlist.artworks.firstOrNull(),
                            "Tú",
                        )
                    },
                    onRename = { playlistToRename = playlist },
                    onDelete = { playlistToDelete = playlist },
                    isAvailable = isAvailable,
                    hasDownloaded = hasDownloaded,
                )
            }

            items(remotePlaylists, key = { "remote_${it.id}" }) { playlist ->
                val isAvailable = !isOffline || playlist.id in offlineRemotePlaylistIds
                val hasDownloaded = isOffline && playlist.id in offlineRemotePlaylistIds
                RemotePlaylistGridCard(
                    playlist = playlist,
                    onClick = {
                        onPlaylistClick(
                            playlist.id,
                            playlist.title,
                            playlist.artworkUrl,
                            playlist.author,
                        )
                    },
                    isAvailable = isAvailable,
                    hasDownloaded = hasDownloaded,
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            item(key = "create_playlist_compact") {
                CreatePlaylistCompactRow(onClick = { showCreateDialog = true })
            }

            items(customPlaylists, key = { "custom_compact_${it.id}" }) { playlist ->
                val hasDownloaded = isOffline && playlist.id in offlineCustomPlaylistIds
                val isAvailable = !isOffline || hasDownloaded
                CustomPlaylistCompactRow(
                    playlist = playlist,
                    onClick = {
                        onPlaylistClick(
                            "local:${playlist.id}",
                            playlist.name,
                            playlist.artworks.firstOrNull(),
                            "Tú",
                        )
                    },
                    onRename = { playlistToRename = playlist },
                    onDelete = { playlistToDelete = playlist },
                    isAvailable = isAvailable,
                    hasDownloaded = hasDownloaded,
                )
            }

            items(remotePlaylists, key = { "remote_compact_${it.id}" }) { playlist ->
                val isAvailable = !isOffline || playlist.id in offlineRemotePlaylistIds
                val hasDownloaded = isOffline && playlist.id in offlineRemotePlaylistIds
                RemotePlaylistCompactRow(
                    playlist = playlist,
                    onClick = {
                        onPlaylistClick(
                            playlist.id,
                            playlist.title,
                            playlist.artworkUrl,
                            playlist.author,
                        )
                    },
                    isAvailable = isAvailable,
                    hasDownloaded = hasDownloaded,
                )
            }
        }
    }
}
