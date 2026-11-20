package com.github.adriianh.melo.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.util.LocalWindowActive
import com.github.adriianh.melo.util.MeloType
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private const val MELODY_FRAME_DELAY_MS = 85L
private const val MELODY_FRAME_STEP_SEC = 0.085f
private const val MELODY_CYCLE_SEC = 60f
private const val MELODY_TWO_PI = (2.0 * PI).toFloat()
private val MELODY_NOTES = listOf("♪", "♫", "♩", "♬")

@Composable
fun DancingMelodyIndicator(
    isActive: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    baseFontSize: TextUnit = 17.sp,
    noteCount: Int = 3,
) {
    val shouldAnimate = isActive && LocalWindowActive.current
    if (shouldAnimate) {
        val elapsedSeconds = remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(MELODY_FRAME_DELAY_MS.milliseconds)
                elapsedSeconds.floatValue =
                    (elapsedSeconds.floatValue + MELODY_FRAME_STEP_SEC) % MELODY_CYCLE_SEC
            }
        }
        AnimatedMelodyRow(
            elapsedSeconds = elapsedSeconds,
            tint = tint,
            baseFontSize = baseFontSize,
            noteCount = noteCount,
            modifier = modifier,
        )
    } else {
        StaticMelodyRow(
            tint = tint,
            baseFontSize = baseFontSize,
            modifier = modifier,
        )
    }
}

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
private fun AnimatedMelodyRow(
    elapsedSeconds: FloatState,
    tint: Color,
    baseFontSize: TextUnit,
    noteCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Melodía",
            tint = tint,
            modifier =
                Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        val wave = 0.5f + 0.5f * sin(elapsedSeconds.floatValue * (MELODY_TWO_PI / 0.80f))
                        val s = 0.92f + 0.23f * wave
                        scaleX = s
                        scaleY = s
                    },
        )

        for (i in 0 until noteCount.coerceAtMost(MELODY_NOTES.size)) {
            val delaySec = i * 0.16f
            Text(
                text = MELODY_NOTES[i % MELODY_NOTES.size],
                style =
                    MeloType.body.copy(
                        fontSize = baseFontSize,
                        fontWeight = FontWeight.Bold,
                    ),
                color = tint,
                modifier =
                    Modifier.graphicsLayer {
                        val t = (elapsedSeconds.floatValue - delaySec).coerceAtLeast(0f)
                        val bounceWave = 0.5f + 0.5f * sin(t * (MELODY_TWO_PI / 0.84f))
                        val rotWave = sin(t * (MELODY_TWO_PI / 0.96f))
                        val scaleWave = 0.5f + 0.5f * sin(t * (MELODY_TWO_PI / 0.76f))
                        translationY = (1.5f - 7.0f * bounceWave).dp.toPx()
                        rotationZ = 10f * rotWave
                        val s = 0.90f + 0.30f * scaleWave
                        scaleX = s
                        scaleY = s
                    },
            )
        }
    }
}

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
private fun StaticMelodyRow(
    tint: Color,
    baseFontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Melodía",
            tint = tint,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = "♪ ♫ ♪",
            style =
                MeloType.body.copy(
                    fontSize = baseFontSize,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                ),
            color = tint,
        )
    }
}
