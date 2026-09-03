package com.github.adriianh.melo.ui.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformAsyncImage

@Composable
fun PlaylistsTabContent(
    customPlaylists: List<Playlist>,
    remotePlaylists: List<SearchResult.Playlist>,
    onCreatePlaylist: (String) -> Unit,
    onRenamePlaylist: (Long, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlaylistClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    "Nueva playlist",
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text("Nombre de la playlist") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MeloColors.border,
                        focusedTextColor = MeloColors.textPrimary,
                        unfocusedTextColor = MeloColors.textPrimary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreatePlaylist(playlistName.trim())
                            showCreateDialog = false
                        }
                    },
                    enabled = playlistName.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Crear", style = MeloType.labelMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar", color = MeloColors.textSecondary)
                }
            },
            containerColor = MeloColors.surface1
        )
    }

    playlistToRename?.let { target ->
        var newName by remember(target) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            title = {
                Text(
                    "Renombrar playlist",
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Nuevo nombre") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MeloColors.border,
                        focusedTextColor = MeloColors.textPrimary,
                        unfocusedTextColor = MeloColors.textPrimary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onRenamePlaylist(target.id, newName.trim())
                            playlistToRename = null
                        }
                    },
                    enabled = newName.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar", style = MeloType.labelMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToRename = null }) {
                    Text("Cancelar", color = MeloColors.textSecondary)
                }
            },
            containerColor = MeloColors.surface1
        )
    }

    playlistToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            title = {
                Text(
                    "Eliminar playlist",
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar \"${target.name}\"? Esta acción no se puede deshacer.",
                    style = MeloType.body,
                    color = MeloColors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlaylist(target.id)
                        playlistToDelete = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        "Eliminar",
                        style = MeloType.labelMedium,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancelar", color = MeloColors.textSecondary)
                }
            },
            containerColor = MeloColors.surface1
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "create_playlist_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showCreateDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MeloColors.surface1)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Crear playlist",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Crear playlist",
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Lista personalizada",
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        items(customPlaylists, key = { "custom_${it.id}" }) { playlist ->
            var showMenu by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlaylistClick("local:${playlist.id}") },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MeloColors.surface1)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                    ) {
                        com.github.adriianh.melo.ui.components.PlaylistMosaicCover(
                            artworks = playlist.artworks,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        ) {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opciones",
                                    tint = MeloColors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(MeloColors.surface1)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Renombrar", style = MeloType.body) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = MeloColors.textPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        playlistToRename = playlist
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Eliminar",
                                            style = MeloType.body,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        playlistToDelete = playlist
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = playlist.name,
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MeloColors.textPrimary
                    )
                    Text(
                        text = "${playlist.trackCount} canciones • Tuya",
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        items(remotePlaylists, key = { "remote_${it.id}" }) { playlist ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPlaylistClick(playlist.id) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MeloColors.surface1)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    PlatformAsyncImage(
                        url = playlist.artworkUrl,
                        contentDescription = playlist.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        size = 140.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = playlist.title,
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MeloColors.textPrimary
                    )
                    Text(
                        text = "${playlist.trackCount ?: 0} canciones • ${playlist.author}",
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
