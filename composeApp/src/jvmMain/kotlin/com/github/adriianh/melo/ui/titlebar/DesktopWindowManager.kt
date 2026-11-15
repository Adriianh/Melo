package com.github.adriianh.melo.ui.titlebar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sun.jna.platform.win32.WinUser
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Insets
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.WindowEvent
import java.awt.event.WindowStateListener

/**
 * Manages window placement (maximize, restore, minimize) for undecorated Compose Desktop windows.
 * Uses native Win32 `SW_MAXIMIZE` / `SW_RESTORE` via [WindowsWindowProcHook] on Windows and
 * falls back to usable screen bounds on other desktop platforms.
 */
class DesktopWindowManager(
    private val window: Window,
) {
    var isMaximized by mutableStateOf(false)
        private set

    private var savedBounds: Rectangle? = null
    private val win32Hook =
        WindowsWindowProcHook(
            window = window,
            onMaximizedChanged = { maximized ->
                isMaximized = maximized
            },
        )
    private val windowStateListener =
        WindowStateListener { event: WindowEvent ->
            val newState = event.newState
            if ((newState and Frame.ICONIFIED) == 0) {
                isMaximized = (newState and Frame.MAXIMIZED_BOTH) != 0
            }
        }
    private var listenersAttached = false

    fun attach() {
        if (!listenersAttached) {
            window.addWindowStateListener(windowStateListener)
            listenersAttached = true
        }
        win32Hook.attach()
    }

    fun detach() {
        win32Hook.detach()
        if (listenersAttached) {
            window.removeWindowStateListener(windowStateListener)
            listenersAttached = false
        }
    }

    fun toggleMaximize() {
        val zoomed = win32Hook.isZoomed()
        if (zoomed != null) {
            win32Hook.showWindow(if (zoomed) WinUser.SW_RESTORE else WinUser.SW_MAXIMIZE)
            return
        }

        val gc =
            window.graphicsConfiguration
                ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
        val screenBounds = gc.bounds

        if (!isMaximized) {
            savedBounds = Rectangle(window.bounds)
            window.bounds = computeFallbackMaximizedBounds(screenBounds, insets)
            isMaximized = true
        } else {
            val restored =
                savedBounds ?: Rectangle(
                    screenBounds.x + insets.left + 40,
                    screenBounds.y + insets.top + 40,
                    (screenBounds.width * 0.8).toInt().coerceAtLeast(800),
                    (screenBounds.height * 0.8).toInt().coerceAtLeast(540),
                )
            window.bounds = restored
            isMaximized = false
        }
    }

    fun minimize() {
        if (!win32Hook.showWindow(WinUser.SW_MINIMIZE)) {
            (window as? Frame)?.extendedState = Frame.ICONIFIED
        }
    }
}

internal fun computeFallbackMaximizedBounds(
    screenBounds: Rectangle,
    screenInsets: Insets,
): Rectangle =
    Rectangle(
        screenBounds.x + screenInsets.left,
        screenBounds.y + screenInsets.top,
        screenBounds.width - screenInsets.left - screenInsets.right,
        screenBounds.height - screenInsets.top - screenInsets.bottom,
    )
