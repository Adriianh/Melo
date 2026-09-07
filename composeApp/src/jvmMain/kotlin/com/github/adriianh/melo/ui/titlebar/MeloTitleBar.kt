package com.github.adriianh.melo.ui.titlebar

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import com.github.adriianh.melo.util.DarkMeloColors
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
fun WindowScope.MeloTitleBar(
    windowState: WindowState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMaximized = windowState.placement == WindowPlacement.Maximized

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
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
                    .pointerInput(windowState.placement) {
                        detectTapGestures(
                            onDoubleTap = {
                                windowState.placement = if (isMaximized) {
                                    WindowPlacement.Floating
                                } else {
                                    WindowPlacement.Maximized
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFFFF2D55), CircleShape)
                    )
                    Text(
                        text = "Melo",
                        style = MeloType.labelMedium,
                        color = MeloColors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TitleBarButton(
                onClick = { windowState.isMinimized = true },
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
                onClick = {
                    windowState.placement = if (isMaximized) {
                        WindowPlacement.Floating
                    } else {
                        WindowPlacement.Maximized
                    }
                },
                hoverBackground = Color.White.copy(alpha = 0.08f)
            ) { tint ->
                Canvas(modifier = Modifier.size(10.dp)) {
                    if (isMaximized) {
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