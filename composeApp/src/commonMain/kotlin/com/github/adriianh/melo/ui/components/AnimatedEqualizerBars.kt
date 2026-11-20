package com.github.adriianh.melo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.LocalWindowActive

@Composable
fun AnimatedEqualizerBars(
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val isWindowActive = LocalWindowActive.current
    val canvasModifier =
        modifier
            .padding(horizontal = 6.dp)
            .width(17.5.dp)
            .height(16.dp)

    if (!isWindowActive) {
        Canvas(modifier = canvasModifier) {
            drawEqualizerBars(
                heightsDp = floatArrayOf(6f, 11f, 8f, 12f),
                color = accentColor,
            )
        }
        return
    }

    val barHeights = rememberEqualizerBarHeights()
    Canvas(modifier = canvasModifier) {
        drawEqualizerBars(
            heightsDp = FloatArray(barHeights.size) { index -> barHeights[index].value },
            color = accentColor,
        )
    }
}

@Composable
private fun rememberEqualizerBarHeights(): List<State<Float>> {
    val transition = rememberInfiniteTransition(label = "equalizer")
    return listOf(
        animateBar(transition, initial = 4f, target = 14f, durationMs = 450, label = "bar1"),
        animateBar(transition, initial = 14f, target = 6f, durationMs = 350, label = "bar2"),
        animateBar(transition, initial = 6f, target = 16f, durationMs = 500, label = "bar3"),
        animateBar(transition, initial = 12f, target = 4f, durationMs = 400, label = "bar4"),
    )
}

@Composable
private fun animateBar(
    transition: androidx.compose.animation.core.InfiniteTransition,
    initial: Float,
    target: Float,
    durationMs: Int,
    label: String,
): State<Float> =
    transition.animateFloat(
        initialValue = initial,
        targetValue = target,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = label,
    )

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
