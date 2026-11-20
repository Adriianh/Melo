package com.github.adriianh.melo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.LocalWindowActive
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private const val EQUALIZER_FRAME_DELAY_MS = 100L
private const val EQUALIZER_FRAME_STEP_SEC = 0.10f
private const val EQUALIZER_CYCLE_SEC = 63f
private const val TWO_PI = (2.0 * PI).toFloat()

@Composable
fun AnimatedEqualizerBars(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val isWindowActive = LocalWindowActive.current
    val elapsedSeconds = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isWindowActive) {
        if (isWindowActive) {
            while (true) {
                delay(EQUALIZER_FRAME_DELAY_MS.milliseconds)
                elapsedSeconds.floatValue =
                    (elapsedSeconds.floatValue + EQUALIZER_FRAME_STEP_SEC) % EQUALIZER_CYCLE_SEC
            }
        }
    }

    Canvas(
        modifier =
            modifier
                .padding(horizontal = 6.dp)
                .width(17.5.dp)
                .height(16.dp),
    ) {
        val heightsDp =
            if (!isWindowActive) {
                floatArrayOf(6f, 11f, 8f, 12f)
            } else {
                val t = elapsedSeconds.floatValue
                floatArrayOf(
                    waveHeight(t = t, periodSec = 0.90f, phase = 0f, minDp = 4f, maxDp = 14f),
                    waveHeight(t = t, periodSec = 0.70f, phase = 1.6f, minDp = 6f, maxDp = 14f),
                    waveHeight(t = t, periodSec = 1.00f, phase = 3.1f, minDp = 6f, maxDp = 16f),
                    waveHeight(t = t, periodSec = 0.80f, phase = 4.5f, minDp = 4f, maxDp = 12f),
                )
            }
        drawEqualizerBars(heightsDp = heightsDp, color = accentColor)
    }
}

private fun waveHeight(
    t: Float,
    periodSec: Float,
    phase: Float,
    minDp: Float,
    maxDp: Float,
): Float {
    val normalized = 0.5f + 0.5f * sin(t * (TWO_PI / periodSec) + phase)
    return minDp + (maxDp - minDp) * normalized
}

private fun DrawScope.drawEqualizerBars(
    heightsDp: FloatArray,
    color: Color,
) {
    val barWidthPx = 2.5.dp.toPx()
    val gapPx = 2.5.dp.toPx()
    val cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())

    for (index in heightsDp.indices) {
        val barHeightPx = heightsDp[index].dp.toPx().coerceAtMost(size.height)
        val x = index * (barWidthPx + gapPx)
        val y = size.height - barHeightPx
        drawRoundRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(barWidthPx, barHeightPx),
            cornerRadius = cornerRadius,
        )
    }
}
