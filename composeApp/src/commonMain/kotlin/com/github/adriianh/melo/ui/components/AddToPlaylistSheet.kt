package com.github.adriianh.melo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    onDismissRequest: () -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreatePlaylistAndAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
    track: Track? = null,
    tracks: List<Track> = emptyList(),
    containingPlaylistIds: Set<Long> = emptySet(),
) {
    val effectiveTracks = remember(track, tracks) {
        if (track != null) listOf(track) else tracks
    }
    val singleTrack = effectiveTracks.singleOrNull()
    val isBatch = effectiveTracks.size > 1

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isCreatingNew by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MeloColors.surface1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MeloColors.textMuted) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isBatch) {
                    PlaylistMosaicCover(
                        artworks = effectiveTracks.mapNotNull { it.artworkUrl },
                        modifier = Modifier.size(50.dp),
                        placeholderIconSize = 26.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    MeloAsyncImage(
                        url = singleTrack?.artworkUrl,
                        contentDescription = singleTrack?.title,
                        size = 50.dp,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBatch) "Añadir selección a playlist" else "Añadir a playlist",
                        style = MeloType.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBatch) "${effectiveTracks.size} canciones seleccionadas" else (singleTrack?.title
                            ?: "Canción"),
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isBatch) {
                            val distinctArtists =
                                effectiveTracks.map { it.artist }.filter { it.isNotBlank() }
                                    .distinct()
                            if (distinctArtists.size == 1) distinctArtists.first() else "${
                                distinctArtists.take(
                                    2
                                ).joinToString(", ")
                            } y más"
                        } else (singleTrack?.artist ?: ""),
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = isCreatingNew,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Nombre de la playlist...", style = MeloType.body) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MeloColors.border,
                            focusedTextColor = MeloColors.textPrimary,
                            unfocusedTextColor = MeloColors.textPrimary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        )
                    )

                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreatePlaylistAndAdd(newPlaylistName.trim())
                                onDismissRequest()
                            }
                        },
                        enabled = newPlaylistName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Crear",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Crear", style = MeloType.labelMedium)
                    }

                    IconButton(onClick = { isCreatingNew = false; newPlaylistName = "" }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancelar",
                            tint = MeloColors.textSecondary
                        )
                    }
                }
            }

            if (!isCreatingNew) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isCreatingNew = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MeloColors.surface2
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "Crear nueva playlist",
                            style = MeloType.body,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val availablePlaylists = remember(playlists, containingPlaylistIds) {
                playlists.filter { it.id !in containingPlaylistIds }
            }

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aún no tienes playlists creadas. ¡Crea una para comenzar!",
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary
                    )
                }
            } else if (availablePlaylists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Esta canción ya está agregada en todas tus playlists.",
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                ) {
                    items(availablePlaylists, key = { it.id }) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectPlaylist(playlist)
                                    onDismissRequest()
                                }
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            PlaylistMosaicCover(
                                artworks = playlist.artworks,
                                modifier = Modifier.size(44.dp),
                                placeholderIconSize = 24.dp
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    style = MeloType.body,
                                    fontWeight = FontWeight.Medium,
                                    color = MeloColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${playlist.trackCount} canciones",
                                    style = MeloType.labelSmall,
                                    color = MeloColors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}