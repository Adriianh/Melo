package com.github.adriianh.melo.ui.components.track

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned

sealed interface SongMenuAnchor {
    data class Bounds(
        val rect: Rect,
    ) : SongMenuAnchor

    data class Point(
        val offset: Offset,
    ) : SongMenuAnchor
}

internal object SongMenuAnchorRegistry {
    private var pendingAnchor: SongMenuAnchor? = null

    fun capture(anchor: SongMenuAnchor?) {
        pendingAnchor = anchor
    }

    fun consume(): SongMenuAnchor? = pendingAnchor.also { pendingAnchor = null }
}

fun Modifier.songMenuRightClick(
    enabled: Boolean = true,
    onOpenMenu: (SongMenuAnchor) -> Unit,
): Modifier =
    composed {
        var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

        this
            .onGloballyPositioned { coordinates = it }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val change = event.changes.firstOrNull() ?: continue
                            change.consume()
                            val windowOffset =
                                coordinates?.takeIf { it.isAttached }?.localToWindow(change.position)
                                    ?: change.position
                            onOpenMenu(SongMenuAnchor.Point(windowOffset))
                        }
                    }
                }
            }
    }
