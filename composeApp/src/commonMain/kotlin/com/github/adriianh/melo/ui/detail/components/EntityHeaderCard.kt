package com.github.adriianh.melo.ui.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
    onSubtitleClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(24.dp),
                enabled = hasTracks
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
                enabled = hasTracks
            ) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Aleatorio",
                    tint = MeloColors.textPrimary
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Aleatorio", color = MeloColors.textPrimary)
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isSaved) accentColor.copy(alpha = 0.15f) else MeloColors.surface2,
                border = BorderStroke(
                    0.5.dp,
                    if (isSaved) accentColor.copy(alpha = 0.4f) else MeloColors.border
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onToggleSave)
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
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
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onAddToQueue)
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
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
        }
    }
}