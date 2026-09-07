package com.github.adriianh.melo.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.melo.util.MeloColors

@Composable
fun MeloSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = Color.White,
    inactiveColor: Color = MeloColors.borderStrong,
    thumbColor: Color = Color.White,
    trackHeight: Dp = 3.5.dp,
    hoverTrackHeight: Dp = 5.dp,
    thumbRadius: Dp = 5.5.dp,
    alwaysShowThumb: Boolean = false,
    hitTargetHeight: Dp = 22.dp,
) {
    var isDragging by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isInteracting = enabled && (isHovered || isDragging || alwaysShowThumb)

    val currentTrackHeight by animateDpAsState(
        targetValue = if (isInteracting) hoverTrackHeight else trackHeight,
        animationSpec = tween(durationMillis = 150),
        label = "MeloSliderTrackHeight"
    )

    val currentThumbRadius by animateDpAsState(
        targetValue = if (isInteracting) thumbRadius else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "MeloSliderThumbRadius"
    )

    val thumbAlpha by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "MeloSliderThumbAlpha"
    )

    val rangeSpan = (valueRange.endInclusive - valueRange.start).let { if (it <= 0f) 1f else it }
    val fraction = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(hitTargetHeight)
            .hoverable(interactionSource, enabled = enabled)
            .pointerInput(valueRange, enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isDragging = true

                    fun updatePosition(x: Float) {
                        val w = size.width.toFloat()
                        if (w > 0f) {
                            val f = (x / w).coerceIn(0f, 1f)
                            val newVal = valueRange.start + f * rangeSpan
                            onValueChange(newVal)
                        }
                    }

                    updatePosition(down.position.x)

                    try {
                        var currentPointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == currentPointerId }
                                ?: event.changes.firstOrNull { it.pressed }
                                ?: break

                            if (!change.pressed) break
                            change.consume()
                            currentPointerId = change.id
                            updatePosition(change.position.x)
                        }
                    } finally {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(hitTargetHeight)) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val trackH = currentTrackHeight.toPx()
            val trackR = trackH / 2f
            val currentThumbR = currentThumbRadius.toPx()

            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, centerY - trackR),
                size = Size(width, trackH),
                cornerRadius = CornerRadius(trackR, trackR)
            )

            val activeWidth = (width * fraction).coerceIn(0f, width)
            if (activeWidth > 0f) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(0f, centerY - trackR),
                    size = Size(activeWidth, trackH),
                    cornerRadius = CornerRadius(trackR, trackR)
                )
            }

            if (currentThumbR > 0.5f && thumbAlpha > 0.01f) {
                val thumbX = activeWidth.coerceIn(0f, width)

                drawCircle(
                    color = Color.Black.copy(alpha = 0.25f * thumbAlpha),
                    radius = currentThumbR + 1.2.dp.toPx(),
                    center = Offset(thumbX, centerY + 0.8.dp.toPx())
                )

                drawCircle(
                    color = thumbColor.copy(alpha = thumbAlpha),
                    radius = currentThumbR,
                    center = Offset(thumbX, centerY)
                )
            }
        }
    }
}