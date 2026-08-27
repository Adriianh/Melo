package com.github.adriianh.melo.ui.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun ArtistHeaderCard(
    artworkUrl: String?,
    name: String,
    subtitleText: String?,
    isSaved: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRadioClick: () -> Unit,
    onToggleFollow: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    hasTracks: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MeloAsyncImage(
            url = artworkUrl,
            contentDescription = name,
            modifier = Modifier
                .size(160.dp)
                .shadow(16.dp, CircleShape)
                .clip(CircleShape)
                .border(1.5.dp, MeloColors.borderStrong, CircleShape),
            size = 160.dp,
            shape = CircleShape
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = name,
            style = MeloType.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MeloColors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        if (!subtitleText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitleText,
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary,
                textAlign = TextAlign.Center
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

            Button(
                onClick = onRadioClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MeloColors.surface2,
                    contentColor = MeloColors.textPrimary
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = "Radio",
                    tint = MeloColors.textPrimary
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Radio", color = MeloColors.textPrimary)
            }

            Button(
                onClick = onToggleFollow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) accentColor.copy(alpha = 0.15f) else MeloColors.surface2,
                    contentColor = if (isSaved) accentColor else MeloColors.textPrimary
                ),
                border = BorderStroke(
                    0.5.dp,
                    if (isSaved) accentColor.copy(alpha = 0.4f) else MeloColors.border
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    if (isSaved) Icons.Default.Check else Icons.Default.PersonAdd,
                    contentDescription = if (isSaved) "Siguiendo" else "Seguir",
                    tint = if (isSaved) accentColor else MeloColors.textPrimary
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    if (isSaved) "Siguiendo" else "Seguir",
                    color = if (isSaved) accentColor else MeloColors.textPrimary
                )
            }
        }
    }
}