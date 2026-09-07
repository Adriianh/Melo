package com.github.adriianh.melo.ui.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.ui.components.MeloSwipeableItem
import com.github.adriianh.melo.ui.components.TrackRow
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState

@Composable
fun RenamePlaylistDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var newName by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Renombrar playlist", style = MeloType.titleMedium, fontWeight = FontWeight.Bold)
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
                    if (newName.isNotBlank()) onConfirm(newName.trim())
                },
                enabled = newName.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Guardar", style = MeloType.labelMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MeloColors.textSecondary)
            }
        },
        containerColor = MeloColors.surface1
    )
}

@Composable
fun DeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Eliminar playlist", style = MeloType.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(
                "¿Estás seguro de que deseas eliminar \"$playlistName\"? Esta acción no se puede deshacer.",
                style = MeloType.body,
                color = MeloColors.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
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
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MeloColors.textSecondary)
            }
        },
        containerColor = MeloColors.surface1
    )
}

@Composable
fun PlaylistTopBarActions(
    isSelectionMode: Boolean,
    selectedCount: Int,
    totalCount: Int,
    isCustomPlaylist: Boolean,
    accentColor: Color,
    onToggleSelectAll: () -> Unit,
    onRenameRequest: () -> Unit,
    onDeleteRequest: () -> Unit,
    onAddAllToPlaylist: () -> Unit,
    onStartSelection: () -> Unit,
) {
    if (isSelectionMode) {
        val isAllSelected = selectedCount == totalCount
        IconButton(onClick = onToggleSelectAll) {
            Icon(
                Icons.Default.SelectAll,
                contentDescription = if (isAllSelected) "Deseleccionar todo" else "Seleccionar todo",
                tint = if (isAllSelected) accentColor else MeloColors.textPrimary
            )
        }
    } else if (totalCount > 0) {
        var showMenu by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = MeloColors.textPrimary
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MeloColors.surface1)
            ) {
                if (isCustomPlaylist) {
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
                            onRenameRequest()
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Añadir todo a playlist local...", style = MeloType.body) },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            showMenu = false
                            onAddAllToPlaylist()
                        }
                    )
                }
                if (isCustomPlaylist) {
                    DropdownMenuItem(
                        text = { Text("Añadir / Fusionar con playlist...", style = MeloType.body) },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            showMenu = false
                            onAddAllToPlaylist()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Seleccionar canciones", style = MeloType.body) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Checklist,
                            contentDescription = null,
                            tint = MeloColors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        showMenu = false
                        onStartSelection()
                    }
                )
                if (isCustomPlaylist) {
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
                            onDeleteRequest()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LazyItemScope.ReorderablePlaylistTrackItem(
    song: Track,
    index: Int,
    reorderState: ReorderableLazyListState,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    itemKey: Any,
    accentColor: Color,
    activeAccent: Color,
    onSelectionToggle: () -> Unit,
    onLongPress: () -> Unit,
    onPlay: () -> Unit,
    onMoreClick: (() -> Unit)?,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    swipeRightIcon: ImageVector,
) {
    ReorderableItem(
        state = reorderState,
        key = itemKey,
        modifier = Modifier.fillMaxWidth()
    ) { isDragging ->
        MeloSwipeableItem(
            onSwipeLeft = { if (!isSelectionMode) onSwipeLeft() },
            onSwipeRight = { if (!isSelectionMode) onSwipeRight() },
            swipeRightIcon = swipeRightIcon,
            swipeLeftIcon = Icons.AutoMirrored.Filled.QueueMusic,
            swipeLeftColor = MeloColors.brandAccent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isDragging) activeAccent.copy(alpha = 0.25f)
                        else if (isSelected) accentColor.copy(alpha = 0.18f)
                        else if (isCurrent) accentColor.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
            ) {
                TrackRow(
                    track = song,
                    trackNumber = index + 1,
                    isCurrent = isCurrent,
                    isPlaying = isPlaying,
                    isDownloaded = isDownloaded,
                    isDownloading = isDownloading,
                    downloadProgress = downloadProgress,
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    onSelectionToggle = onSelectionToggle,
                    onLongClick = onLongPress,
                    onClick = onPlay,
                    onMoreClick = onMoreClick,
                    dragHandle = if (isSelectionMode) null else {
                        {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Reordenar pista",
                                tint = if (isDragging) activeAccent else MeloColors.textMuted,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(6.dp)
                                    .draggableHandle()
                            )
                        }
                    }
                )
            }
        }
    }
}