package com.github.adriianh.melo.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.github.adriianh.core.domain.model.NowPlayingBackdropScrim
import com.github.adriianh.core.domain.model.NowPlayingBlurIntensity
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.getCachedBlurredArtwork
import com.github.adriianh.melo.util.getOrLoadBlurredArtworkBitmap
import com.github.adriianh.melo.util.normalizeArtworkUrl
import kotlin.math.max

private const val BACKDROP_SOURCE_URL_SIZE_PX = 544

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
fun NowPlayingBackground(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    blurEnabled: Boolean = true,
    blurIntensity: NowPlayingBlurIntensity = NowPlayingBlurIntensity.MEDIUM,
    backdropScrim: NowPlayingBackdropScrim = NowPlayingBackdropScrim.NORMAL,
    accentColor: Color = MeloColors.brandAccent,
) {
    val surface0 = MeloColors.surface0
    val isDark = MeloColors.isDark
    val isBlurActive = blurEnabled && blurIntensity != NowPlayingBlurIntensity.OFF

    Box(modifier = modifier.fillMaxSize().background(surface0)) {
        if (!isBlurActive) {
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
                    blurSigmaPx = blurIntensity.sigmaPx,
                    backdropScrim = backdropScrim,
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
    blurSigmaPx: Float,
    backdropScrim: NowPlayingBackdropScrim,
    isDark: Boolean,
) {
    val backdropUrl = remember(url) { normalizeArtworkUrl(url, BACKDROP_SOURCE_URL_SIZE_PX) ?: url }
    val blurredBitmap by produceState<ImageBitmap?>(
        initialValue = remember(backdropUrl, blurSigmaPx) { getCachedBlurredArtwork(backdropUrl, blurSigmaPx) },
        key1 = backdropUrl,
        key2 = blurSigmaPx,
    ) {
        if (value == null) {
            value = getOrLoadBlurredArtworkBitmap(backdropUrl, blurSigmaPx = blurSigmaPx)
        }
    }
    val scrimColor = if (isDark) Color.Black else Color.White
    val scrimBrush =
        remember(scrimColor, backdropScrim) {
            Brush.verticalGradient(
                colors =
                    listOf(
                        scrimColor.copy(alpha = backdropScrim.topAlpha),
                        scrimColor.copy(alpha = backdropScrim.upperMidAlpha),
                        scrimColor.copy(alpha = backdropScrim.lowerMidAlpha),
                        scrimColor.copy(alpha = backdropScrim.bottomAlpha),
                    ),
            )
        }

    Box(modifier = Modifier.fillMaxSize()) {
        val bitmap = blurredBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium,
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
    val glowAlpha = if (isDark) 0.24f else 0.16f
    val secondaryAlpha = if (isDark) 0.10f else 0.07f

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val radius = max(size.width, size.height) * 0.85f
                    val brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    animatedAccent.copy(alpha = glowAlpha),
                                    animatedAccent.copy(alpha = secondaryAlpha),
                                    Color.Transparent,
                                ),
                            center = Offset(size.width * 0.35f, size.height * 0.28f),
                            radius = radius,
                        )
                    onDrawBehind {
                        drawRect(brush = brush)
                    }
                },
    )
}
