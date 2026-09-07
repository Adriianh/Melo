package com.github.adriianh.melo.ui.titlebar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.github.adriianh.melo.util.DarkMeloColors
import com.github.adriianh.melo.util.MeloColors

@Composable
fun WindowScope.MeloTitleBar(
    windowManager: DesktopWindowManager,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMaximized = windowManager.isMaximized

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(DarkMeloColors.surface0),
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
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TitleBarButton(
                onClick = { windowManager.minimize() },
                hoverBackground = Color.White.copy(alpha = 0.08f)
            ) { tint ->
                Canvas(modifier = Modifier.size(10.dp)) {
                    val y = size.height * 0.85f
                    drawLine(
                        color = tint,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5f
                    )
                }
            }

            TitleBarButton(
                onClick = { windowManager.toggleMaximize() },
                hoverBackground = Color.White.copy(alpha = 0.08f)
            ) { tint ->
                Canvas(modifier = Modifier.size(10.dp)) {
                    if (isMaximized) {
                        // Restore icon: two overlapping rectangles
                        val offset = 2.5f
                        val rectSize = size.width - offset

                        drawLine(tint, Offset(offset, 0f), Offset(size.width, 0f), 1.5f)
                        drawLine(tint, Offset(size.width, 0f), Offset(size.width, rectSize), 1.5f)
                        drawLine(
                            tint,
                            Offset(rectSize, rectSize),
                            Offset(size.width, rectSize),
                            1.5f
                        )
                        drawLine(tint, Offset(offset, 0f), Offset(offset, offset), 1.5f)
                        drawRect(
                            color = tint,
                            topLeft = Offset(0f, offset),
                            size = Size(rectSize, rectSize),
                            style = Stroke(width = 1.5f)
                        )
                    } else {
                        drawRect(
                            color = tint,
                            topLeft = Offset.Zero,
                            size = size,
                            style = Stroke(width = 1.5f)
                        )
                    }
                }
            }

            TitleBarButton(
                onClick = onClose,
                hoverBackground = Color(0xFFE81123),
                hoverContentColor = Color.White
            ) { tint ->
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawLine(
                        color = tint,
                        start = Offset(0.5f, 0.5f),
                        end = Offset(size.width - 0.5f, size.height - 0.5f),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = tint,
                        start = Offset(size.width - 0.5f, 0.5f),
                        end = Offset(0.5f, size.height - 0.5f),
                        strokeWidth = 1.5f
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
    hoverContentColor: Color = Color.White,
    content: @Composable (tint: Color) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val currentBg = if (isHovered) hoverBackground else Color.Transparent
    val currentTint = if (isHovered) hoverContentColor else MeloColors.textMuted

    Box(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
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