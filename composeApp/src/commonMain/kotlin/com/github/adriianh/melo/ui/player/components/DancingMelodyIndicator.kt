package com.github.adriianh.melo.ui.player.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.util.MeloType

@Composable
fun DancingMelodyIndicator(
    isActive: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    baseFontSize: TextUnit = 17.sp,
    noteCount: Int = 3
) {
    val notes = listOf("♪", "♫", "♩", "♬")

    if (isActive) {
        val infiniteTransition = rememberInfiniteTransition(label = "DancingMelodyTransition")

        val iconScale by infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "IconScaleAnim"
        )

        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Melodía",
                tint = tint,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
            )

            val delays = listOf(0, 160, 320, 480)
            for (i in 0 until noteCount.coerceAtMost(notes.size)) {
                val delay = delays.getOrElse(i) { i * 160 }
                val bounceY by infiniteTransition.animateFloat(
                    initialValue = 1.5f,
                    targetValue = -5.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 420,
                            delayMillis = delay,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "NoteBounce_$i"
                )
                val rotation by infiniteTransition.animateFloat(
                    initialValue = -10f,
                    targetValue = 10f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 480,
                            delayMillis = delay,
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "NoteRotate_$i"
                )
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.90f,
                    targetValue = 1.20f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 380,
                            delayMillis = delay,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "NoteScale_$i"
                )

                Text(
                    text = notes[i % notes.size],
                    style = MeloType.body.copy(
                        fontSize = baseFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    color = tint,
                    modifier = Modifier
                        .offset(y = bounceY.dp)
                        .graphicsLayer(
                            rotationZ = rotation,
                            scaleX = scale,
                            scaleY = scale
                        )
                )
            }
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Melodía",
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "♪ ♫ ♪",
                style = MeloType.body.copy(
                    fontSize = baseFontSize,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                ),
                color = tint
            )
        }
    }
}