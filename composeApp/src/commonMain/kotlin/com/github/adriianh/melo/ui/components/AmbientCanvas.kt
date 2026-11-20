package com.github.adriianh.melo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.withTransform
import com.github.adriianh.melo.util.LocalMeloColors
import com.github.adriianh.melo.util.LocalWindowActivity
import com.github.adriianh.melo.util.rememberSharedAnimationTime
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

private const val TWO_PI = (2.0 * PI).toFloat()

@Composable
fun AmbientCanvas(
    accentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    animate: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val meloColors = LocalMeloColors.current
    val isDark = meloColors.isDark
    val isWindowVisible = LocalWindowActivity.current.isVisible

    Box(modifier = modifier.fillMaxSize()) {
        val baseModifier =
            Modifier
                .fillMaxSize()
                .ambientBaseBackground(
                    isDark = isDark,
                    surface0 = meloColors.surface0,
                    surface2 = meloColors.surface2,
                )

        if (enabled) {
            val animatedAccent by animateColorAsState(
                targetValue = accentColor,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                label = "AmbientCanvasAccent",
            )
            val shouldAnimate = animate && isWindowVisible
            val sharedTime = rememberSharedAnimationTime(enabled = shouldAnimate)

            Box(
                modifier =
                    baseModifier.ambientOrbsOverlay(
                        animatedAccent = animatedAccent,
                        isDark = isDark,
                        elapsedSeconds = if (shouldAnimate) sharedTime else null,
                    ),
            )
        } else {
            Box(modifier = baseModifier)
        }
        content()
    }
}

private fun Modifier.ambientBaseBackground(
    isDark: Boolean,
    surface0: Color,
    surface2: Color,
): Modifier =
    background(
        if (isDark) {
            SolidColor(surface0)
        } else {
            Brush.verticalGradient(listOf(surface0, surface2.copy(alpha = 0.9f)))
        },
    )

private fun Modifier.ambientOrbsOverlay(
    animatedAccent: Color,
    isDark: Boolean,
    elapsedSeconds: FloatState?,
): Modifier =
    drawWithCache {
        val orb1Radius = max(size.width, size.height) * 0.65f
        val orb2Radius = orb1Radius * 0.9f
        val orb1Alpha = if (isDark) 0.30f else 0.14f
        val orb2Alpha = if (isDark) 0.20f else 0.10f
        val baseOrb1X = size.width * 0.72f
        val baseOrb1Y = size.height * 0.28f
        val baseOrb2X = size.width * 0.24f
        val baseOrb2Y = size.height * 0.76f

        val orb1Brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        animatedAccent.copy(alpha = orb1Alpha),
                        animatedAccent.copy(alpha = orb1Alpha * 0.35f),
                        Color.Transparent,
                    ),
                center = Offset.Zero,
                radius = orb1Radius,
            )
        val orb2Brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        animatedAccent.copy(alpha = orb2Alpha),
                        animatedAccent.copy(alpha = orb2Alpha * 0.35f),
                        Color.Transparent,
                    ),
                center = Offset.Zero,
                radius = orb2Radius,
            )

        onDrawBehind {
            val t = elapsedSeconds?.floatValue ?: 0f
            val dx1 = size.width * 0.22f * sin(t * (TWO_PI / 18f))
            val dy1 = size.height * 0.20f * sin(t * (TWO_PI / 24f) + 0.4f)
            val scale1 = 1f + 0.14f * (0.5f + 0.5f * sin(t * (TWO_PI / 7f)))

            val dx2 = size.width * -0.20f * sin(t * (TWO_PI / 24f) + 1.2f)
            val dy2 = size.height * 0.22f * sin(t * (TWO_PI / 18f) + 0.3f)
            val scale2 = 1f + 0.12f * (0.5f + 0.5f * sin(t * (TWO_PI / 10f)))

            withTransform({
                translate(left = baseOrb1X + dx1, top = baseOrb1Y + dy1)
                scale(scaleX = scale1, scaleY = scale1, pivot = Offset.Zero)
            }) {
                drawCircle(brush = orb1Brush, radius = orb1Radius, center = Offset.Zero)
            }

            withTransform({
                translate(left = baseOrb2X + dx2, top = baseOrb2Y + dy2)
                scale(scaleX = scale2, scaleY = scale2, pivot = Offset.Zero)
            }) {
                drawCircle(brush = orb2Brush, radius = orb2Radius, center = Offset.Zero)
            }
        }
    }
