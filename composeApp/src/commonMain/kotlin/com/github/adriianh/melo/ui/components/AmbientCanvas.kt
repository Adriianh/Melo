package com.github.adriianh.melo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import com.github.adriianh.melo.util.LocalMeloColors
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AmbientCanvas(
    accentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val meloColors = LocalMeloColors.current
    val isDark = meloColors.isDark

    Box(modifier = modifier.fillMaxSize()) {
        if (enabled) {
            val animatedAccent by animateColorAsState(
                targetValue = accentColor,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
                label = "AmbientCanvasAccent"
            )

            val infiniteTransition = rememberInfiniteTransition(label = "AmbientCanvasMotion")

            val phase1 = infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(18000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Orb1Phase"
            )
            val pulse1 = infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(7000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Orb1Pulse"
            )

            val phase2 = infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(24000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Orb2Phase"
            )
            val pulse2 = infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(10000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Orb2Pulse"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDark) SolidColor(meloColors.surface0)
                        else Brush.verticalGradient(
                            listOf(
                                meloColors.surface0,
                                meloColors.surface2.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            val orb1Brush = remember(animatedAccent, isDark) {
                val alpha = if (isDark) 0.32f else 0.14f
                Brush.radialGradient(
                    colors = listOf(
                        animatedAccent.copy(alpha = alpha),
                        animatedAccent.copy(alpha = alpha * 0.35f),
                        Color.Transparent
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p1 = phase1.value * 2f * PI.toFloat()
                        translationX = size.width * (0.35f * sin(p1 + 0.2f))
                        translationY = size.height * (0.32f * sin(p1 * 0.75f + 0.4f))
                        val s = 1.15f + 0.15f * pulse1.value
                        scaleX = s
                        scaleY = s
                    }
                    .background(orb1Brush)
            )

            val orb2Brush = remember(animatedAccent, isDark) {
                val alpha = if (isDark) 0.22f else 0.10f
                Brush.radialGradient(
                    colors = listOf(
                        animatedAccent.copy(alpha = alpha),
                        animatedAccent.copy(alpha = alpha * 0.35f),
                        Color.Transparent
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p2 = phase2.value * 2f * PI.toFloat()
                        translationX = size.width * (-0.32f * sin(p2 * 0.85f + 1.2f))
                        translationY = size.height * (0.35f * sin(p2 * 0.90f + 0.3f))
                        val s = 1.05f + 0.12f * pulse2.value
                        scaleX = s
                        scaleY = s
                    }
                    .background(orb2Brush)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDark) SolidColor(meloColors.surface0)
                        else Brush.verticalGradient(
                            listOf(
                                meloColors.surface0,
                                meloColors.surface2.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
        }
        content()
    }
}