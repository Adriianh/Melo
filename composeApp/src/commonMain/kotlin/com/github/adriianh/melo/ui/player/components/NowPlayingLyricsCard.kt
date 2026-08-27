package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun NowPlayingLyricsCard(
    lyrics: String?,
    activeAccent: Color,
    onToggleArtwork: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MeloColors.surface1.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MeloColors.borderStrong),
        shadowElevation = 16.dp,
        modifier = modifier.clickable(onClick = onToggleArtwork)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                tint = activeAccent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (!lyrics.isNullOrBlank()) lyrics else "Letras sincronizadas disponibles próximamente",
                    style = MeloType.body,
                    fontWeight = FontWeight.Medium,
                    color = MeloColors.textPrimary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Toca para ver la portada",
                style = MeloType.labelSmall.copy(fontSize = 11.sp),
                color = MeloColors.textMuted
            )
        }
    }
}

@Composable
fun NowPlayingLyricsSection(
    lyrics: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (!lyrics.isNullOrBlank()) lyrics else "Letras sincronizadas disponibles próximamente",
            style = MeloType.body,
            color = MeloColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.verticalScroll(rememberScrollState())
        )
    }
}
