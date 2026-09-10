package com.github.adriianh.melo.ui.titlebar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window

/**
 * Manages window placement (maximize, restore, minimize) for undecorated Compose Desktop windows.
 * Avoids AWT MAXIMIZED_BOTH Win32 bugs on undecorated frames by calculating exact usable screen bounds.
 */
class DesktopWindowManager(private val window: Window) {

    var isMaximized by mutableStateOf(false)
        private set

    private var savedBounds: Rectangle? = null

    fun toggleMaximize() {
        val gc = window.graphicsConfiguration
            ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
        val screenBounds = gc.bounds

        if (!isMaximized) {
            savedBounds = Rectangle(window.bounds)

            val maxBounds = Rectangle(
                screenBounds.x + insets.left,
                screenBounds.y + insets.top,
                screenBounds.width - insets.left - insets.right,
                screenBounds.height - insets.top - insets.bottom
            )
            window.bounds = maxBounds
            isMaximized = true
        } else {
            val restored = savedBounds ?: Rectangle(
                screenBounds.x + insets.left + 40,
                screenBounds.y + insets.top + 40,
                (screenBounds.width * 0.8).toInt().coerceAtLeast(800),
                (screenBounds.height * 0.8).toInt().coerceAtLeast(540)
            )
            window.bounds = restored
            isMaximized = false
        }
    }

    fun minimize() {
        (window as? Frame)?.extendedState = Frame.ICONIFIED
    }
}