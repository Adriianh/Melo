package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlayerUiState
import com.github.adriianh.core.domain.model.Track

@Composable
internal fun NowPlayingMobileActions(
    activeAccent: Color,
    state: PlayerUiState,
    onOpenQueue: () -> Unit,
    onStartRadio: (Track) -> Unit,
    onOpenArtist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MobileActionChip(
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = activeAccent,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = "Cola",
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                .clickable { onOpenQueue() }
        )

        MobileActionChip(
            icon = {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = activeAccent,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = "Radio",
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable {
                state.currentTrack?.let { onStartRadio(it) }
            }
        )

        MobileActionChip(
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = activeAccent,
                    modifier = Modifier.size(18.dp)
                )
            },
            label = "Artista",
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                .clickable { onOpenArtist() }
        )
    }
}

@Composable
private fun MobileActionChip(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MeloColors.surface1.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MeloColors.border),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label,
                style = MeloType.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MeloColors.textPrimary
            )
        }
    }
}