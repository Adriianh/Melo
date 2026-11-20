package com.github.adriianh.melo.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.PlatformAsyncImage
import com.github.adriianh.melo.util.normalizeArtworkUrl
import kotlin.math.max

private val BLUR_SOURCE_SIZE = 160.dp

@Composable
fun NowPlayingBackground(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    blurEnabled: Boolean = true,
    accentColor: Color = MeloColors.brandAccent,
) {
    val surface0 = MeloColors.surface0
    val isDark = MeloColors.isDark

    Box(modifier = modifier.fillMaxSize().background(surface0)) {
        if (!blurEnabled) {
            NowPlayingGradientFallback(
                accentColor = accentColor,
                isDark = isDark,
            )
            return@Box
        }

        Crossfade(
            targetState = artworkUrl,
            animationSpec = tween(durationMillis = 650),
            label = "NowPlayingBgCrossfade",
        ) { url ->
            if (url != null) {
                BlurredArtworkBackdrop(
                    url = url,
                    isDark = isDark,
                )
            }
        }
    }
}

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
private fun BlurredArtworkBackdrop(
    url: String,
    isDark: Boolean,
) {
    val lowResUrl = remember(url) { normalizeArtworkUrl(url, 160) }
    val scrimColor = if (isDark) Color.Black else Color.White
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

        Box(modifier = Modifier.fillMaxSize().background(scrimBrush))
    }
}

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
private fun NowPlayingGradientFallback(
    accentColor: Color,
    isDark: Boolean,
) {
    val animatedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "NowPlayingFallbackAccent",
    )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val radius = max(size.width, size.height) * 0.85f
                    val topAlpha = if (isDark) 0.32f else 0.16f
                    val gradient =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    animatedAccent.copy(alpha = topAlpha),
                                    animatedAccent.copy(alpha = topAlpha * 0.35f),
                                    Color.Transparent,
                                ),
                            center = Offset(size.width * 0.5f, size.height * 0.25f),
                            radius = radius,
                        )
                    onDrawBehind {
                        drawRect(brush = gradient)
                    }
                },
    )
}
