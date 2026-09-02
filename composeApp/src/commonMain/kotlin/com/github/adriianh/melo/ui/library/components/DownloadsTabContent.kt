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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownloadOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.desktopScroll

@Composable
fun DownloadsTabContent(
    downloadedTracks: List<OfflineTrack>,
    onPlayTrack: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onDeleteDownload: (String) -> Unit,
    onMoreClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    if (downloadedTracks.isEmpty()) {
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
                    imageVector = Icons.Default.FileDownloadOff,
                    contentDescription = null,
                    tint = MeloColors.textMuted,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "No tienes canciones descargadas",
                    style = MeloType.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Descarga tus canciones, álbumes o playlists favoritas para escucharlas sin conexión y sin consumir datos.",
                    style = MeloType.body,
                    color = MeloColors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val listState = rememberLazyListState()
    val totalSizeMb = downloadedTracks.sumOf { it.fileSize } / (1024.0 * 1024.0)

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
                            text = "${downloadedTracks.size} canciones descargadas",
                            style = MeloType.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MeloColors.textPrimary
                        )
                        Text(
                            text = "Espacio ocupado: ${"%.1f".format(totalSizeMb)} MB",
                            style = MeloType.labelSmall,
                            color = MeloColors.textSecondary
                        )
                    }
                }

                if (downloadedTracks.isNotEmpty()) {
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

        items(downloadedTracks, key = { it.track.id }) { offlineTrack ->
            DownloadedTrackRow(
                offlineTrack = offlineTrack,
                onClick = { onPlayTrack(offlineTrack.track) },
                onDelete = { onDeleteDownload(offlineTrack.track.id) },
                onMoreClick = { onMoreClick(offlineTrack.track) }
            )
        }
    }
}

@Composable
private fun DownloadedTrackRow(
    offlineTrack: OfflineTrack,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMoreClick: () -> Unit
) {
    val track = offlineTrack.track
    val sizeMb = offlineTrack.fileSize / (1024.0 * 1024.0)

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Descargado",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${track.artist} • ${"%.1f".format(sizeMb)} MB",
                        style = MeloType.labelSmall,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Eliminar descarga",
                    tint = MeloColors.textSecondary,
                    modifier = Modifier.size(20.dp)
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