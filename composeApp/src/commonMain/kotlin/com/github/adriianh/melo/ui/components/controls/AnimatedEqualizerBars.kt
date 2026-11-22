package com.github.adriianh.melo.ui.components.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.LocalWindowActivity
import com.github.adriianh.melo.util.rememberSharedAnimationTime
import kotlin.math.PI
import kotlin.math.sin

private const val TWO_PI = (2.0 * PI).toFloat()

@Suppress("FunctionNaming", "ktlint:standard:function-naming")
@Composable
fun AnimatedEqualizerBars(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val isWindowVisible = LocalWindowActivity.current.isVisible
    val elapsedSeconds = rememberSharedAnimationTime(enabled = isWindowVisible)

    Canvas(
        modifier =
            modifier
                .padding(horizontal = 6.dp)
                .width(17.5.dp)
                .height(16.dp),
    ) {
        if (!isWindowVisible) {
            drawEqualizerBars(6f, 11f, 8f, 12f, accentColor)
        } else {
            val t = elapsedSeconds.floatValue
            drawEqualizerBars(
                h0 = waveHeight(t = t, periodSec = 0.90f, phase = 0f, minDp = 4f, maxDp = 14f),
                h1 = waveHeight(t = t, periodSec = 0.70f, phase = 1.6f, minDp = 6f, maxDp = 14f),
                h2 = waveHeight(t = t, periodSec = 1.00f, phase = 3.1f, minDp = 6f, maxDp = 16f),
                h3 = waveHeight(t = t, periodSec = 0.80f, phase = 4.5f, minDp = 4f, maxDp = 12f),
                color = accentColor,
            )
        }
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
    h0: Float,
    h1: Float,
    h2: Float,
    h3: Float,
    color: Color,
) {
    val barWidthPx = 2.5.dp.toPx()
    val stepPx = barWidthPx + 2.5.dp.toPx()
    val cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())

    drawBar(0, h0, barWidthPx, stepPx, cornerRadius, color)
    drawBar(1, h1, barWidthPx, stepPx, cornerRadius, color)
    drawBar(2, h2, barWidthPx, stepPx, cornerRadius, color)
    drawBar(3, h3, barWidthPx, stepPx, cornerRadius, color)
}

@Suppress("LongParameterList")
private fun DrawScope.drawBar(
    index: Int,
    heightDp: Float,
    barWidthPx: Float,
    stepPx: Float,
    cornerRadius: CornerRadius,
    color: Color,
) {
    val barHeightPx = heightDp.dp.toPx().coerceAtMost(size.height)
    drawRoundRect(
        color = color,
        topLeft = Offset(index * stepPx, size.height - barHeightPx),
        size = Size(barWidthPx, barHeightPx),
        cornerRadius = cornerRadius,
    )
}
