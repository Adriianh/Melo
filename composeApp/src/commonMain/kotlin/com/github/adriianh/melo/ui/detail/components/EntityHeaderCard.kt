package com.github.adriianh.melo.ui.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.ui.components.PlaylistMosaicCover
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun EntityHeaderCard(
    artworkUrl: String?,
    title: String,
    subtitle: String?,
    metadataText: String?,
    isSaved: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onToggleSave: () -> Unit,
    onAddToQueue: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    hasTracks: Boolean = true,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    downloadProgress: Float = 0f,
    downloadedCount: Int = 0,
    totalTrackCount: Int = 0,
    currentDownloadingTrackTitle: String? = null,
    artworkUrls: List<String>? = null,
    onDownloadClick: (() -> Unit)? = null,
    onSubtitleClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!artworkUrls.isNullOrEmpty()) {
            PlaylistMosaicCover(
                artworks = artworkUrls,
                shape = RoundedCornerShape(14.dp),
                placeholderIconSize = 64.dp,
                modifier = Modifier
                    .size(200.dp)
                    .shadow(16.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        MeloColors.borderStrong,
                        RoundedCornerShape(14.dp)
                    )
            )
        } else {
            MeloAsyncImage(
                url = artworkUrl,
                contentDescription = title,
                modifier = Modifier
                    .size(200.dp)
                    .shadow(16.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        MeloColors.borderStrong,
                        RoundedCornerShape(14.dp)
                    ),
                size = 200.dp,
                shape = RoundedCornerShape(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MeloType.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MeloColors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MeloType.labelMedium,
                color = if (onSubtitleClick != null) accentColor else MeloColors.textSecondary,
                fontWeight = if (onSubtitleClick != null) FontWeight.SemiBold else FontWeight.Normal,
                modifier = if (onSubtitleClick != null) Modifier.clickable(onClick = onSubtitleClick) else Modifier
            )
        }

        if (!metadataText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = metadataText,
                style = MeloType.labelSmall,
                color = MeloColors.textMuted
            )
        }

        if (isDownloaded) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.14f),
                border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.DownloadDone,
                        contentDescription = "Descargado",
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Descargado",
                        style = MeloType.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(24.dp),
                enabled = hasTracks,
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Reproducir",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Reproducir", color = MaterialTheme.colorScheme.onPrimary)
            }

            Button(
                onClick = onShuffleClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MeloColors.surface2,
                    contentColor = MeloColors.textPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                enabled = hasTracks,
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Aleatorio",
                    tint = MeloColors.textPrimary
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Aleatorio", color = MeloColors.textPrimary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isSaved) accentColor.copy(alpha = 0.15f) else MeloColors.surface2,
                border = BorderStroke(
                    0.5.dp,
                    if (isSaved) accentColor.copy(alpha = 0.4f) else MeloColors.border
                ),
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onToggleSave)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (isSaved) "Guardado" else "Guardar",
                        tint = if (isSaved) accentColor else MeloColors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MeloColors.surface2,
                border = BorderStroke(0.5.dp, MeloColors.border),
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onAddToQueue)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Añadir a la cola",
                        tint = MeloColors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (onDownloadClick != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (isDownloaded) accentColor.copy(alpha = 0.15f) else MeloColors.surface2,
                    border = BorderStroke(
                        0.5.dp,
                        if (isDownloaded) accentColor.copy(alpha = 0.4f) else MeloColors.border
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .then(if (isDownloading) Modifier else Modifier.size(44.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onDownloadClick)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = if (isDownloading) 12.dp else 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isDownloading -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { downloadProgress.coerceIn(0f, 1f) },
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = accentColor
                                    )
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        style = MeloType.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                            }
                            isDownloaded -> {
                                Icon(
                                    Icons.Default.DownloadDone,
                                    contentDescription = "Descargado",
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Descargar",
                                    tint = MeloColors.textPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isDownloading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MeloColors.surface1)
                    .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val remaining = (totalTrackCount - downloadedCount).coerceAtLeast(0)
                    Text(
                        text = "Descargando $downloadedCount de $totalTrackCount canciones ($remaining restantes)",
                        style = MeloType.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MeloColors.textPrimary
                    )
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        style = MeloType.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                LinearProgressIndicator(
                    progress = { downloadProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = MeloColors.surface2
                )

                if (!currentDownloadingTrackTitle.isNullOrBlank()) {
                    Text(
                        text = "Descargando: $currentDownloadingTrackTitle",
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