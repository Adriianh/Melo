package com.github.adriianh.melo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackContextMenu(
    track: Track,
    onDismissRequest: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onRemoveFromQueue: () -> Unit = {},
    inQueue: Boolean = false,
    onToggleLike: () -> Unit,
    isLiked: Boolean = false,
    onAddToPlaylist: () -> Unit,
    onGoToArtist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onShare: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MeloColors.surface1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MeloColors.textMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MeloAsyncImage(
                    url = track.artworkUrl,
                    contentDescription = track.title,
                    size = 56.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MeloType.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MeloColors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        style = MeloType.body,
                        color = MeloColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
                thickness = DividerDefaults.Thickness,
                color = MeloColors.glassBorder
            )

            if (inQueue) {
                ContextMenuItem(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    text = "Eliminar de la cola",
                    onClick = { onRemoveFromQueue(); onDismissRequest() }
                )
            } else {
                ContextMenuItem(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    text = "Reproducir a continuación",
                    onClick = { onPlayNext(); onDismissRequest() }
                )
                ContextMenuItem(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    text = "Añadir a la cola",
                    onClick = { onAddToQueue(); onDismissRequest() }
                )
            }
            ContextMenuItem(
                icon = if (isLiked) Icons.Default.HeartBroken else Icons.Default.FavoriteBorder,
                text = if (isLiked) "Quitar de Me gusta" else "Me gusta",
                onClick = { onToggleLike(); onDismissRequest() },
                iconTint = if (isLiked) MaterialTheme.colorScheme.primary else MeloColors.textPrimary
            )
            ContextMenuItem(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                text = "Añadir a playlist...",
                onClick = { onAddToPlaylist(); onDismissRequest() }
            )
            ContextMenuItem(
                icon = Icons.Default.Person,
                text = "Ir al artista",
                onClick = { onGoToArtist(); onDismissRequest() }
            )
            ContextMenuItem(
                icon = Icons.Default.Album,
                text = "Ir al álbum",
                onClick = { onGoToAlbum(); onDismissRequest() }
            )
            ContextMenuItem(
                icon = Icons.Default.Share,
                text = "Compartir",
                onClick = { onShare(); onDismissRequest() }
            )
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    iconTint: Color = MeloColors.textPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            style = MeloType.body,
            color = MeloColors.textPrimary
        )
    }
}