package com.github.adriianh.melo.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.PlatformAsyncImage
import com.github.adriianh.melo.util.normalizeArtworkUrl

private val BLUR_SOURCE_SIZE = 160.dp

@Composable
fun NowPlayingBackground(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(MeloColors.surface0)) {
        Crossfade(
            targetState = artworkUrl,
            animationSpec = tween(durationMillis = 650),
            label = "NowPlayingBgCrossfade",
        ) { url ->
            if (url != null) {
                val lowResUrl = remember(url) { normalizeArtworkUrl(url, 160) }
                val scrimColor = if (MeloColors.isDark) Color.Black else Color.White
                val scrimBrush =
                    remember(scrimColor) {
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    scrimColor.copy(alpha = 0.50f),
                                    scrimColor.copy(alpha = 0.35f),
                                    scrimColor.copy(alpha = 0.75f),
                                    scrimColor.copy(alpha = 0.95f),
                                ),
                        )
                    }

                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize().clipToBounds(),
                    contentAlignment = Alignment.Center,
                ) {
                    val scaleXFactor = (maxWidth / BLUR_SOURCE_SIZE).coerceAtLeast(1f) * 1.25f
                    val scaleYFactor = (maxHeight / BLUR_SOURCE_SIZE).coerceAtLeast(1f) * 1.25f

                    Box(
                        modifier =
                            Modifier.graphicsLayer {
                                scaleX = scaleXFactor
                                scaleY = scaleYFactor
                            },
                    ) {
                        PlatformAsyncImage(
                            url = lowResUrl,
                            contentDescription = null,
                            size = BLUR_SOURCE_SIZE,
                            modifier = Modifier.blur(14.dp),
                            shape = RoundedCornerShape(0.dp),
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxSize().background(scrimBrush),
                    )
                }
            }
        }
    }
}
