package com.github.adriianh.melo.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloAsyncImage
import com.github.adriianh.melo.util.MeloColors

@Composable
fun NowPlayingBackground(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(MeloColors.surface0)) {
        Crossfade(
            targetState = artworkUrl,
            animationSpec = tween(durationMillis = 650),
            label = "NowPlayingBgCrossfade"
        ) { url ->
            if (url != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MeloAsyncImage(
                        url = url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.4f)
                            .blur(28.dp),
                        shape = RoundedCornerShape(0.dp)
                    )

                    val scrimColor = if (MeloColors.isDark) Color.Black else Color.White
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        scrimColor.copy(alpha = 0.50f),
                                        scrimColor.copy(alpha = 0.35f),
                                        scrimColor.copy(alpha = 0.75f),
                                        scrimColor.copy(alpha = 0.95f),
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}