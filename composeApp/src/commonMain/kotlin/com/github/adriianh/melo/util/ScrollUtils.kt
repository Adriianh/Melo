package com.github.adriianh.melo.util

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

@Composable
fun Modifier.desktopScroll(state: LazyListState): Modifier {
    val platform = remember { getPlatform() }
    if (platform.type != PlatformType.DESKTOP) return this

    val scope = rememberCoroutineScope()

    return this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val deltaX = event.changes.first().scrollDelta.x
                        val deltaY = event.changes.first().scrollDelta.y
                        val isShiftPressed = event.keyboardModifiers.isShiftPressed

                        when {
                            deltaX != 0f -> {
                                event.changes.forEach { it.consume() }
                                scope.launch {
                                    state.scrollBy(deltaX * 40f)
                                }
                            }

                            deltaY != 0f && isShiftPressed -> {
                                event.changes.forEach { it.consume() }
                                scope.launch {
                                    state.animateScrollBy(
                                        value = deltaY * 150f,
                                        animationSpec = tween(150, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                scope.launch {
                    state.scrollBy(-dragAmount.x)
                }
            }
        }
}