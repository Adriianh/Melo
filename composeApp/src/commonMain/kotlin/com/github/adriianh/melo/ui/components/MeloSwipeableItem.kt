package com.github.adriianh.melo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MeloSwipeableItem(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    swipeLeftIcon: ImageVector = Icons.AutoMirrored.Filled.QueueMusic,
    swipeRightIcon: ImageVector = Icons.Default.Favorite,
    swipeLeftColor: Color = Color(0xFF4CAF50),
    swipeRightColor: Color = Color(0xFFE91E63),
    cornerRadius: Int = 8,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val maxDragPx = with(density) { 80.dp.toPx() }
    val thresholdPx = with(density) { 56.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val hasVibrated = remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .drawBehind {
                val dragAmount = offsetX.value
                if (dragAmount > 0f) {
                    drawRect(
                        color = swipeRightColor,
                        size = Size(dragAmount, size.height)
                    )
                } else if (dragAmount < 0f) {
                    drawRect(
                        color = swipeLeftColor,
                        topLeft = Offset(size.width + dragAmount, 0f),
                        size = Size(-dragAmount, size.height)
                    )
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newOffset =
                                (offsetX.value + dragAmount).coerceIn(-maxDragPx, maxDragPx)
                            offsetX.snapTo(newOffset)

                            if (!hasVibrated.value && kotlin.math.abs(newOffset) >= thresholdPx) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hasVibrated.value = true
                            } else if (hasVibrated.value && kotlin.math.abs(newOffset) < thresholdPx) {
                                hasVibrated.value = false
                            }
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            val currentOffset = offsetX.value
                            if (currentOffset >= thresholdPx) {
                                onSwipeRight()
                            } else if (currentOffset <= -thresholdPx) {
                                onSwipeLeft()
                            }
                            hasVibrated.value = false
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            hasVibrated.value = false
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                )
            }
    ) {
        val dragDp = with(density) { offsetX.value.toDp() }

        if (dragDp > 30.dp) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier.width(dragDp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(swipeRightIcon, tint = Color.White, contentDescription = null)
                }
            }
        } else if (dragDp < (-30).dp) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier.width(-dragDp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(swipeLeftIcon, tint = Color.White, contentDescription = null)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
        ) {
            content()
        }
    }
}