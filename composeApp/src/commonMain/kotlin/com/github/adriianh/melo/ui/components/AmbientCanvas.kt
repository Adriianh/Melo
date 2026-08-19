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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.github.adriianh.melo.util.LocalMeloColors
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AmbientCanvas(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val animatedAccent by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "AmbientCanvasAccent"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "AmbientCanvasMotion")

    val orbConfigs = remember {
        listOf(
            OrbConfig(
                phaseDuration = 16000, pulseDuration = 6500,
                xFreq = 1.0f, xPhase = 0.2f,
                yFreq = 0.75f, yPhase = 0.4f,
                baseRadius = 0.55f, radiusPulse = 0.08f,
                baseAlpha = 0.32f, alphaPulse = 0.10f
            ),
            OrbConfig(
                phaseDuration = 21000, pulseDuration = 9000,
                xFreq = 0.85f, xPhase = 1.1f,
                yFreq = 0.90f, yPhase = 0.2f,
                baseRadius = 0.48f, radiusPulse = 0.07f,
                baseAlpha = 0.26f, alphaPulse = 0.09f
            ),
            OrbConfig(
                phaseDuration = 27000, pulseDuration = 11500,
                xFreq = 1.10f, xPhase = 2.2f,
                yFreq = 0.70f, yPhase = 1.6f,
                baseRadius = 0.42f, radiusPulse = 0.06f,
                baseAlpha = 0.22f, alphaPulse = 0.08f
            )
        )
    }

    val orbAnimations = orbConfigs.map { config ->
        val phase = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(config.phaseDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "OrbPhase"
        )
        val pulse = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(config.pulseDuration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "OrbPulse"
        )
        OrbState(phase, pulse)
    }

    val meloColors = LocalMeloColors.current
    val isDark = meloColors.isDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(meloColors.surface0)
            .drawBehind {
                val w = size.width
                val h = size.height
                val minDim = size.minDimension

                orbConfigs.zip(orbAnimations).forEach { (config, state) ->
                    val p = state.phase.value * 2f * PI.toFloat()
                    val pulseValue = state.pulse.value

                    val xOffset = 0.45f * sin(p * config.xFreq + config.xPhase)
                    val yOffset = 0.42f * sin(p * config.yFreq + config.yPhase)

                    val x = w * (0.5f + xOffset)
                    val y = h * (0.5f + yOffset)

                    val radius = minDim * (config.baseRadius + config.radiusPulse * pulseValue)
                    val rawAlpha = config.baseAlpha + config.alphaPulse * pulseValue
                    val alpha = if (isDark) rawAlpha else rawAlpha * 0.65f

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedAccent.copy(alpha = alpha),
                                animatedAccent.copy(alpha = alpha * 0.50f),
                                animatedAccent.copy(alpha = alpha * 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(x, y),
                            radius = radius
                        )
                    )
                }
            }
    ) {
        content()
    }
}

private data class OrbConfig(
    val phaseDuration: Int,
    val pulseDuration: Int,
    val xFreq: Float, val xPhase: Float,
    val yFreq: Float, val yPhase: Float,
    val baseRadius: Float,
    val radiusPulse: Float,
    val baseAlpha: Float,
    val alphaPulse: Float,
    val xFreq2: Float = 0f, val xPhase2: Float = 0f, val xAmp2: Float = 0f,
    val yFreq2: Float = 0f, val yPhase2: Float = 0f, val yAmp2: Float = 0f
)

private data class OrbState(val phase: State<Float>, val pulse: State<Float>)
