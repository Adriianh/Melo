package com.github.adriianh.melo.ui.library.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.platform.PlatformFileSystem
import com.github.adriianh.melo.ui.components.LocalFolderPathsEditor
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.desktopScroll
import com.github.adriianh.melo.util.formatDuration
import com.github.adriianh.melo.util.rememberAudioPermissionRequester

@Composable
fun LocalTabContent(
    localTracks: List<Track>,
    localLibraryPaths: List<String>,
    isScanning: Boolean,
    onRescan: () -> Unit,
    onAddFolder: (String) -> Unit,
    onRemoveFolder: (String) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onMoreClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val requestAudioPermission = rememberAudioPermissionRequester { isGranted ->
        if (isGranted) {
            onRescan()
        }
    }

    LaunchedEffect(Unit) {
        requestAudioPermission()
    }

    var showFoldersDialog by remember { mutableStateOf(false) }

    if (showFoldersDialog) {
        ManageFoldersDialog(
            configuredPaths = localLibraryPaths,
            onDismiss = { showFoldersDialog = false },
            onAddPath = onAddFolder,
            onRemovePath = onRemoveFolder,
            onRescan = {
                requestAudioPermission()
                showFoldersDialog = false
            }
        )
    }

    if (isScanning) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Buscando archivos locales...",
                    style = MeloType.body,
                    color = MeloColors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    if (localTracks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MeloColors.textMuted,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "No se encontraron archivos locales",
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Añade tus carpetas de música o escanea las carpetas por defecto para reproducir tus archivos MP3, FLAC, M4A y AAC.",
                    style = MeloType.body,
                    color = MeloColors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showFoldersDialog = true },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Carpetas")
                    }
                    Button(
                        onClick = requestAudioPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Escanear")
                    }
                }
            }
        }
        return
    }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize().desktopScroll(listState)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${localTracks.size} archivos locales",
                            style = MeloType.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MeloColors.textPrimary
                        )
                        Text(
                            text = "Archivos de audio en tu dispositivo",
                            style = MeloType.labelSmall,
                            color = MeloColors.textSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showFoldersDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Gestionar carpetas",
                                tint = MeloColors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = requestAudioPermission,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Volver a escanear",
                                tint = MeloColors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (localTracks.isNotEmpty()) {
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Reproducir todo")
                    }
                }
            }
        }

        items(localTracks, key = { it.id }) { track ->
            LocalTrackRow(
                track = track,
                onClick = { onPlayTrack(track) },
                onMoreClick = { onMoreClick(track) }
            )
        }
    }
}

@Composable
private fun ManageFoldersDialog(
    configuredPaths: List<String>,
    onDismiss: () -> Unit,
    onAddPath: (String) -> Unit,
    onRemovePath: (String) -> Unit,
    onRescan: () -> Unit,
) {
    val defaultPaths = remember { PlatformFileSystem.getDefaultMusicPaths() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MeloColors.surface1,
        title = {
            Text(
                text = "Carpetas de música local",
                style = MeloType.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MeloColors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Añade rutas de carpetas de tu dispositivo para incluir tus pistas locales.",
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary
                )

                LocalFolderPathsEditor(
                    configuredPaths = configuredPaths,
                    onAddPath = onAddPath,
                    onRemovePath = onRemovePath
                )

                if (defaultPaths.isNotEmpty()) {
                    Text(
                        text = "Carpetas por defecto del sistema:",
                        style = MeloType.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MeloColors.textPrimary
                    )
                    defaultPaths.forEach { path ->
                        Surface(
                            color = MeloColors.surface2.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = path,
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
        },
        confirmButton = {
            Button(
                onClick = onRescan,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Escanear ahora")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = MeloColors.textSecondary)
            }
        }
    )
}

@Composable
private fun LocalTrackRow(
    track: Track,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val durationMinSec = formatDuration(track.durationMs)

    Surface(
        color = MeloColors.surface1,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MeloAsyncImage(
                url = track.artworkUrl,
                contentDescription = track.title,
                size = 48.dp,
                shape = RoundedCornerShape(8.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MeloType.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MeloColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist}${if (track.album.isNotBlank()) " • ${track.album}" else ""}${if (durationMinSec.isNotBlank()) " • $durationMinSec" else ""}",
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Más opciones",
                    tint = MeloColors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}