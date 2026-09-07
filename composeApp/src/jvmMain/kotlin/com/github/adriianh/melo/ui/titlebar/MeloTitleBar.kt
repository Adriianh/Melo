package com.github.adriianh.melo.ui.titlebar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.github.adriianh.melo.util.LocalMeloColors

@Composable
fun WindowScope.MeloTitleBar(
    windowManager: DesktopWindowManager,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMaximized = windowManager.isMaximized
    val meloColors = LocalMeloColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WindowDraggableArea(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                windowManager.toggleMaximize()
                            }
                        )
                    }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TitleBarButton(
                onClick = { windowManager.minimize() },
                hoverBackground = if (meloColors.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(
                    alpha = 0.08f
                ),
                hoverContentColor = meloColors.textPrimary,
                normalTint = meloColors.textMuted
            ) { tint ->
                Canvas(modifier = Modifier.size(11.dp)) {
                    val y = size.height / 2f
                    val stroke = 1.8.dp.toPx()
                    drawLine(
                        color = tint,
                        start = Offset(1.2.dp.toPx(), y),
                        end = Offset(size.width - 1.2.dp.toPx(), y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            TitleBarButton(
                onClick = { windowManager.toggleMaximize() },
                hoverBackground = if (meloColors.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(
                    alpha = 0.08f
                ),
                hoverContentColor = meloColors.textPrimary,
                normalTint = meloColors.textMuted
            ) { tint ->
                Canvas(modifier = Modifier.size(11.dp)) {
                    if (isMaximized) {
                        val stroke = 1.3.dp.toPx()
                        val offset = 2.4.dp.toPx()
                        val rWidth = size.width - offset
                        val rHeight = size.height - offset
                        val cr = CornerRadius(1.8.dp.toPx(), 1.8.dp.toPx())

                        val backPath = Path().apply {
                            moveTo(offset + cr.x, 0f)
                            lineTo(size.width - cr.x, 0f)
                            quadraticTo(size.width, 0f, size.width, cr.y)
                            lineTo(size.width, rHeight)
                        }
                        drawPath(
                            path = backPath,
                            color = tint.copy(alpha = 0.75f),
                            style = Stroke(
                                width = stroke,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(0f, offset),
                            size = Size(rWidth, rHeight),
                            cornerRadius = cr,
                            style = Stroke(width = stroke)
                        )
                    } else {
                        val stroke = 1.4.dp.toPx()
                        val halfStroke = stroke / 2f
                        val cr = CornerRadius(2.2.dp.toPx(), 2.2.dp.toPx())
                        drawRoundRect(
                            color = tint,
                            topLeft = Offset(halfStroke, halfStroke),
                            size = Size(size.width - stroke, size.height - stroke),
                            cornerRadius = cr,
                            style = Stroke(width = stroke)
                        )
                    }
                }
            }

            TitleBarButton(
                onClick = onClose,
                hoverBackground = Color(0xFFFF2D55),
                hoverContentColor = Color.White,
                normalTint = meloColors.textMuted
            ) { tint ->
                Canvas(modifier = Modifier.size(11.dp)) {
                    val pad = 1.2.dp.toPx()
                    val stroke = 1.6.dp.toPx()
                    drawLine(
                        color = tint,
                        start = Offset(pad, pad),
                        end = Offset(size.width - pad, size.height - pad),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = tint,
                        start = Offset(size.width - pad, pad),
                        end = Offset(pad, size.height - pad),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleBarButton(
    onClick: () -> Unit,
    hoverBackground: Color,
    hoverContentColor: Color,
    normalTint: Color,
    content: @Composable (tint: Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val currentBg by animateColorAsState(
        targetValue = if (isHovered) hoverBackground else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "TitleBarButtonBg"
    )
    val currentTint by animateColorAsState(
        targetValue = if (isHovered) hoverContentColor else normalTint,
        animationSpec = tween(durationMillis = 150),
        label = "TitleBarButtonTint"
    )

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(currentBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(currentTint)
    }
}