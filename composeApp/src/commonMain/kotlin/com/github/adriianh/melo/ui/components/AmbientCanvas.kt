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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.github.adriianh.melo.util.MeloColors
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
                xFreq = 1.3f, xPhase = 0.2f,
                yFreq = 0.75f, yPhase = 0.4f,
                baseRadius = 0.34f, radiusPulse = 0.05f,
                baseAlpha = 0.45f, alphaPulse = 0.10f,
                xFreq2 = 0.0f, xPhase2 = 0.0f, xAmp2 = 0.0f,
                yFreq2 = 0.0f, yPhase2 = 0.0f, yAmp2 = 0.0f
            ),
            OrbConfig(
                phaseDuration = 21000, pulseDuration = 9000,
                xFreq = 0.85f, xPhase = 1.1f,
                yFreq = 0.90f, yPhase = 0.2f,
                baseRadius = 0.29f, radiusPulse = 0.045f,
                baseAlpha = 0.40f, alphaPulse = 0.10f,
                xFreq2 = 0.0f, xPhase2 = 0.0f, xAmp2 = 0.0f,
                yFreq2 = 0.0f, yPhase2 = 0.0f, yAmp2 = 0.0f
            ),
            OrbConfig(
                phaseDuration = 27000, pulseDuration = 11500,
                xFreq = 1.10f, xPhase = 2.2f,
                yFreq = 0.70f, yPhase = 1.6f,
                baseRadius = 0.25f, radiusPulse = 0.04f,
                baseAlpha = 0.35f, alphaPulse = 0.08f,
                xFreq2 = 0.0f, xPhase2 = 0.0f, xAmp2 = 0.0f,
                yFreq2 = 0.0f, yPhase2 = 0.0f, yAmp2 = 0.0f
            )
        )
    }

    val orbAnimations = orbConfigs.map { config ->
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(config.phaseDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "OrbPhase_${config.hashCode()}"
        )
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(config.pulseDuration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "OrbPulse_${config.hashCode()}"
        )
        OrbAnimation(phase, pulse)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MeloColors.surface0)
            .drawBehind {
                val w = size.width
                val h = size.height
                val maxDim = size.maxDimension

                orbConfigs.zip(orbAnimations).forEach { (config, anim) ->
                    val p = anim.phase * 2f * PI.toFloat()
                    val pulse = anim.pulse

                    var xOffset = 0.85f * sin(p * config.xFreq + config.xPhase)
                    var yOffset = 0.85f * sin(p * config.yFreq + config.yPhase)

                    if (config.xAmp2 != 0f) {
                        xOffset += config.xAmp2 * sin(p * config.xFreq2 + config.xPhase2)
                    }
                    if (config.yAmp2 != 0f) {
                        yOffset += config.yAmp2 * sin(p * config.yFreq2 + config.yPhase2)
                    }

                    val x = w * (0.5f + xOffset)
                    val y = h * (0.5f + yOffset)

                    val diagonal = kotlin.math.sqrt(w * w + h * h)
                    val coreFraction =
                        config.baseRadius + config.radiusPulse * pulse
                    val alpha = config.baseAlpha + config.alphaPulse * pulse

                    drawRect(
                        brush = Brush.radialGradient(
                            0f to animatedAccent.copy(alpha = alpha),
                            coreFraction * 0.5f to animatedAccent.copy(alpha = alpha * 0.55f),
                            coreFraction to animatedAccent.copy(alpha = alpha * 0.18f),
                            1f to Color.Transparent,
                            center = Offset(x, y),
                            radius = diagonal
                        ),
                        blendMode = BlendMode.Screen
                    )

                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = alpha * 0.5f),
                                animatedAccent.copy(alpha = alpha * 0.3f),
                                Color.Transparent
                            ),
                            center = Offset(x, y),
                            radius = diagonal * coreFraction * 0.25f
                        ),
                        blendMode = BlendMode.Screen
                    )
                }

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.30f)),
                        center = Offset(w / 2f, h / 2f),
                        radius = maxDim * 0.75f
                    )
                )
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

private data class OrbAnimation(val phase: Float, val pulse: Float)