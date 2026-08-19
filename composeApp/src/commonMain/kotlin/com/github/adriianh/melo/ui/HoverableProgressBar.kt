package com.github.adriianh.melo.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import kotlin.math.roundToInt

@Composable
internal fun HoverableProgressBar(
    progressFraction: Float,
    accentColor: Color,
    elapsedLabel: String,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    lineCenterY: Dp = 22.dp,
    containerHeight: Dp = 44.dp,
    showHoverControls: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val activeHover = isHovered && showHoverControls
    val barHeight by animateDpAsState(
        targetValue = if (activeHover) 6.dp else 2.5.dp,
        animationSpec = tween(durationMillis = 150),
        label = "barHeight"
    )
    val markerSize = 12.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight)
            .hoverable(interactionSource)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val topY = with(density) { lineCenterY.toPx() }
        val rPx = with(density) { cornerRadius.toPx() }

        val fullPath = remember(widthPx, topY, rPx) {
            Path().apply {
                moveTo(0f, topY + rPx)
                arcTo(
                    rect = Rect(0f, topY, 2f * rPx, topY + 2f * rPx),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo((widthPx - rPx).coerceAtLeast(rPx), topY)
                arcTo(
                    rect = Rect(widthPx - 2f * rPx, topY, widthPx, topY + 2f * rPx),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
            }
        }

        val pathMeasure = remember { PathMeasure() }
        pathMeasure.setPath(fullPath, false)
        val totalLength = pathMeasure.length
        val progressDistance =
            (totalLength * progressFraction.coerceIn(0f, 1f)).coerceIn(0f, totalLength)

        val activePath = remember(fullPath, progressDistance) {
            Path().apply {
                if (progressDistance > 0f) {
                    pathMeasure.getSegment(0f, progressDistance, this, true)
                }
            }
        }

        val playHeadPos = remember(pathMeasure, progressDistance) {
            pathMeasure.getPosition(progressDistance)
        }

        val trackBgColor = MeloColors.borderStrong
        Canvas(modifier = Modifier.fillMaxWidth().height(containerHeight)) {
            drawPath(
                path = fullPath,
                color = trackBgColor,
                style = Stroke(width = barHeight.toPx(), cap = StrokeCap.Round)
            )

            if (progressDistance > 0f) {
                drawPath(
                    path = activePath,
                    color = accentColor,
                    style = Stroke(width = barHeight.toPx(), cap = StrokeCap.Round)
                )
            }

            if (activeHover) {
                drawCircle(
                    color = Color.White,
                    radius = markerSize.toPx() / 2f + 1.5.dp.toPx(),
                    center = playHeadPos
                )
                drawCircle(
                    color = accentColor,
                    radius = markerSize.toPx() / 2f,
                    center = playHeadPos
                )
            }
        }

        if (activeHover) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MeloColors.surface2,
                border = BorderStroke(1.dp, MeloColors.borderStrong),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val xPx = (playHeadPos.x - placeable.width / 2f)
                            .coerceIn(
                                8.dp.toPx(),
                                (maxWidth.toPx() - placeable.width - 8.dp.toPx()).coerceAtLeast(0f)
                            )
                        val yPx =
                            (playHeadPos.y - markerSize.toPx() / 2f - placeable.height - 4.dp.toPx())
                                .coerceAtLeast(0f)
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(xPx.roundToInt(), yPx.roundToInt())
                        }
                    }
            ) {
                Text(
                    text = elapsedLabel,
                    style = MeloType.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}