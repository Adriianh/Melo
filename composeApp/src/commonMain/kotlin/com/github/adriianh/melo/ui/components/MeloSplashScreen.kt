package com.github.adriianh.melo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.util.LocalMeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun MeloSplashScreen(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val meloColors = LocalMeloColors.current
    val isDark = meloColors.isDark
    val transition = rememberInfiniteTransition(label = "SplashWaveTransition")

    val s1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale1"
    )
    val s2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale2"
    )
    val s3 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 540, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale3"
    )
    val s4 by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale4"
    )
    val s5 by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.80f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 460, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveScale5"
    )

    val glowPulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(meloColors.surface0)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {},
        contentAlignment = Alignment.Center
    ) {
        val glowAlpha = if (isDark) 0.22f else 0.12f
        val glowBrush = remember(accentColor) {
            Brush.radialGradient(
                colors = listOf(
                    accentColor,
                    accentColor.copy(alpha = 0.4f),
                    Color.Transparent
                )
            )
        }
        Box(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer {
                    scaleX = glowPulse
                    scaleY = glowPulse
                    alpha = glowAlpha
                }
                .background(glowBrush)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplashWaveBar(scaleProvider = { s1 }, color = accentColor)
                SplashWaveBar(scaleProvider = { s2 }, color = accentColor)
                SplashWaveBar(scaleProvider = { s3 }, color = accentColor)
                SplashWaveBar(scaleProvider = { s4 }, color = accentColor)
                SplashWaveBar(scaleProvider = { s5 }, color = accentColor)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Melo",
                style = MeloType.titleLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp
                ),
                color = meloColors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "MUSIC",
                style = MeloType.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 4.sp
                ),
                color = meloColors.textMuted
            )
        }
    }
}

@Composable
private fun SplashWaveBar(
    scaleProvider: () -> Float,
    color: Color,
) {
    val barBrush = remember(color) {
        Brush.verticalGradient(
            colors = listOf(
                color,
                color.copy(alpha = 0.70f)
            )
        )
    }
    Box(
        modifier = Modifier
            .width(5.dp)
            .height(48.dp)
            .graphicsLayer {
                scaleY = scaleProvider()
            }
            .clip(RoundedCornerShape(percent = 50))
            .background(barBrush)
    )
}