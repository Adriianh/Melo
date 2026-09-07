package com.github.adriianh.melo.ui.titlebar

import java.awt.Cursor
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/**
 * Enables smooth 8-direction edge and corner resizing for undecorated Compose Desktop windows.
 */
class WindowResizer(
    private val window: Window,
    private val isMaximized: () -> Boolean,
    private val minWidth: Int = 800,
    private val minHeight: Int = 540,
    private val borderThickness: Int = 6
) : MouseAdapter() {

    private var activeCursor: Int = Cursor.DEFAULT_CURSOR
    private var dragStartScreen: Point? = null
    private var dragStartBounds: Rectangle? = null

    override fun mouseMoved(e: MouseEvent) {
        if (isMaximized()) {
            if (window.cursor.type != Cursor.DEFAULT_CURSOR) {
                window.cursor = Cursor.getDefaultCursor()
            }
            return
        }

        val cursorType = getCursorForPosition(e.x, e.y, window.width, window.height)
        if (window.cursor.type != cursorType) {
            window.cursor = Cursor.getPredefinedCursor(cursorType)
        }
    }

    override fun mousePressed(e: MouseEvent) {
        if (!SwingUtilities.isLeftMouseButton(e)) return
        if (isMaximized()) return

        val cursorType = getCursorForPosition(e.x, e.y, window.width, window.height)
        if (cursorType != Cursor.DEFAULT_CURSOR) {
            activeCursor = cursorType
            dragStartScreen = e.locationOnScreen
            dragStartBounds = window.bounds
        }
    }

    override fun mouseDragged(e: MouseEvent) {
        val startScreen = dragStartScreen ?: return
        val startBounds = dragStartBounds ?: return

        val dx = e.locationOnScreen.x - startScreen.x
        val dy = e.locationOnScreen.y - startScreen.y

        var newX = startBounds.x
        var newY = startBounds.y
        var newW = startBounds.width
        var newH = startBounds.height

        when (activeCursor) {
            Cursor.E_RESIZE_CURSOR -> {
                newW = (startBounds.width + dx).coerceAtLeast(minWidth)
            }
            Cursor.S_RESIZE_CURSOR -> {
                newH = (startBounds.height + dy).coerceAtLeast(minHeight)
            }
            Cursor.SE_RESIZE_CURSOR -> {
                newW = (startBounds.width + dx).coerceAtLeast(minWidth)
                newH = (startBounds.height + dy).coerceAtLeast(minHeight)
            }
            Cursor.W_RESIZE_CURSOR -> {
                val proposedW = startBounds.width - dx
                if (proposedW >= minWidth) {
                    newX = startBounds.x + dx
                    newW = proposedW
                } else {
                    newX = startBounds.x + (startBounds.width - minWidth)
                    newW = minWidth
                }
            }
            Cursor.N_RESIZE_CURSOR -> {
                val proposedH = startBounds.height - dy
                if (proposedH >= minHeight) {
                    newY = startBounds.y + dy
                    newH = proposedH
                } else {
                    newY = startBounds.y + (startBounds.height - minHeight)
                    newH = minHeight
                }
            }
            Cursor.NW_RESIZE_CURSOR -> {
                val proposedW = startBounds.width - dx
                if (proposedW >= minWidth) {
                    newX = startBounds.x + dx
                    newW = proposedW
                } else {
                    newX = startBounds.x + (startBounds.width - minWidth)
                    newW = minWidth
                }
                val proposedH = startBounds.height - dy
                if (proposedH >= minHeight) {
                    newY = startBounds.y + dy
                    newH = proposedH
                } else {
                    newY = startBounds.y + (startBounds.height - minHeight)
                    newH = minHeight
                }
            }
            Cursor.NE_RESIZE_CURSOR -> {
                newW = (startBounds.width + dx).coerceAtLeast(minWidth)
                val proposedH = startBounds.height - dy
                if (proposedH >= minHeight) {
                    newY = startBounds.y + dy
                    newH = proposedH
                } else {
                    newY = startBounds.y + (startBounds.height - minHeight)
                    newH = minHeight
                }
            }
            Cursor.SW_RESIZE_CURSOR -> {
                val proposedW = startBounds.width - dx
                if (proposedW >= minWidth) {
                    newX = startBounds.x + dx
                    newW = proposedW
                } else {
                    newX = startBounds.x + (startBounds.width - minWidth)
                    newW = minWidth
                }
                newH = (startBounds.height + dy).coerceAtLeast(minHeight)
            }
        }

        window.setBounds(newX, newY, newW, newH)
        window.revalidate()
    }

    override fun mouseReleased(e: MouseEvent) {
        dragStartScreen = null
        dragStartBounds = null
        activeCursor = Cursor.DEFAULT_CURSOR
    }

    private fun getCursorForPosition(x: Int, y: Int, w: Int, h: Int): Int {
        val onLeft = x in 0..borderThickness
        val onRight = x in (w - borderThickness)..w
        val onTop = y in 0..borderThickness
        val onBottom = y in (h - borderThickness)..h

        return when {
            onTop && onLeft -> Cursor.NW_RESIZE_CURSOR
            onTop && onRight -> Cursor.NE_RESIZE_CURSOR
            onBottom && onLeft -> Cursor.SW_RESIZE_CURSOR
            onBottom && onRight -> Cursor.SE_RESIZE_CURSOR
            onLeft -> Cursor.W_RESIZE_CURSOR
            onRight -> Cursor.E_RESIZE_CURSOR
            onTop -> Cursor.N_RESIZE_CURSOR
            onBottom -> Cursor.S_RESIZE_CURSOR
            else -> Cursor.DEFAULT_CURSOR
        }
    }
}