package com.github.adriianh.melo.ui.components

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

class ReorderableState(
    val lazyListState: LazyListState,
    val density: Density,
    val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    var draggingItemKey by mutableStateOf<Any?>(null)
        private set

    var draggingIndex by mutableStateOf<Int?>(null)
        private set

    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    fun isDragging(key: Any): Boolean = draggingItemKey == key

    fun onDragStart(key: Any, index: Int) {
        draggingItemKey = key
        draggingIndex = index
        dragOffsetY = 0f
    }

    fun onDrag(dragAmount: Float, totalItemsCount: Int) {
        val currentIdx = draggingIndex ?: return
        dragOffsetY += dragAmount

        val estimatedItemHeight = with(density) { 56.dp.toPx() }
        val itemHeight = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == draggingItemKey }?.size?.toFloat()
            ?: estimatedItemHeight

        val threshold = itemHeight * 0.5f

        if (dragOffsetY > threshold && currentIdx < totalItemsCount - 1) {
            val targetIdx = currentIdx + 1
            onMove(currentIdx, targetIdx)
            draggingIndex = targetIdx
            dragOffsetY -= itemHeight
        } else if (dragOffsetY < -threshold && currentIdx > 0) {
            val targetIdx = currentIdx - 1
            onMove(currentIdx, targetIdx)
            draggingIndex = targetIdx
            dragOffsetY += itemHeight
        }
    }

    fun onDragStopped() {
        draggingItemKey = null
        draggingIndex = null
        dragOffsetY = 0f
    }
}

@Composable
fun rememberReorderableState(
    lazyListState: LazyListState,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
): ReorderableState {
    val density = LocalDensity.current
    return remember(lazyListState, onMove, density) {
        ReorderableState(lazyListState, density, onMove)
    }
}

@Composable
fun Modifier.reorderDragHandle(
    state: ReorderableState,
    key: Any,
    index: Int,
    totalItemsCount: Int,
    hapticFeedback: HapticFeedback? = null
): Modifier {
    val currentKey by rememberUpdatedState(key)
    val currentIndex by rememberUpdatedState(index)
    val currentCount by rememberUpdatedState(totalItemsCount)
    val currentHaptic by rememberUpdatedState(hapticFeedback)

    return this.pointerInput(state) {
        detectVerticalDragGestures(
            onDragStart = {
                currentHaptic?.performHapticFeedback(HapticFeedbackType.LongPress)
                state.onDragStart(currentKey, currentIndex)
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                state.onDrag(dragAmount, currentCount)
            },
            onDragEnd = {
                state.onDragStopped()
            },
            onDragCancel = {
                state.onDragStopped()
            }
        )
    }
}

@Composable
fun ReorderableItem(
    state: ReorderableState,
    key: Any,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val isDragging = state.isDragging(key)
    Box(
        modifier = modifier
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) state.dragOffsetY else 0f
                scaleX = if (isDragging) 1.03f else 1.0f
                scaleY = if (isDragging) 1.03f else 1.0f
                shadowElevation = if (isDragging) 16f else 0f
            }
    ) {
        content(isDragging)
    }
}