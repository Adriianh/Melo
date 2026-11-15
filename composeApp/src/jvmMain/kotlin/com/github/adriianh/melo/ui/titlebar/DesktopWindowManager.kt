package com.github.adriianh.melo.ui.titlebar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sun.jna.Native
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.ShellAPI
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Insets
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.Window
import kotlin.math.ceil

private const val ABM_GETAUTOHIDEBAREX = 0x0000000B
private const val ABS_AUTOHIDE = 0x00000001
private const val AUTO_HIDE_INSET_PHYSICAL_PX = 2.0

/**
 * Manages window placement (maximize, restore, minimize) for undecorated Compose Desktop windows.
 * Avoids AWT MAXIMIZED_BOTH Win32 bugs on undecorated frames by calculating exact usable screen bounds.
 */
class DesktopWindowManager(
    private val window: Window,
) {
    var isMaximized by mutableStateOf(false)
        private set

    private var savedBounds: Rectangle? = null

    fun toggleMaximize() {
        val gc =
            window.graphicsConfiguration
                ?: GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
        val screenBounds = gc.bounds

        if (!isMaximized) {
            savedBounds = Rectangle(window.bounds)

            val transform = gc.defaultTransform
            val autoHideEdges = detectWindowsAutoHideTaskbarEdges(window)
            val maxBounds =
                computeMaximizedBounds(
                    screenBounds = screenBounds,
                    screenInsets = insets,
                    scaleX = transform.scaleX,
                    scaleY = transform.scaleY,
                    autoHideEdges = autoHideEdges,
                )
            window.bounds = maxBounds
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
        (window as? Frame)?.extendedState = Frame.ICONIFIED
    }
}

internal enum class TaskbarEdge(
    val win32Edge: Int,
) {
    LEFT(ShellAPI.ABE_LEFT),
    TOP(ShellAPI.ABE_TOP),
    RIGHT(ShellAPI.ABE_RIGHT),
    BOTTOM(ShellAPI.ABE_BOTTOM),
    ;

    companion object {
        fun fromWin32Edge(edge: Int): TaskbarEdge? = entries.firstOrNull { it.win32Edge == edge }
    }
}

/**
 * Computes maximized window bounds inside [screenBounds] minus [screenInsets].
 *
 * When a Windows auto-hide taskbar sits on an edge with zero AWT screen inset, shrinks that edge
 * by ~2 physical pixels so Explorer does not classify the undecorated window as fullscreen and
 * still reveals the taskbar on hover.
 */
internal fun computeMaximizedBounds(
    screenBounds: Rectangle,
    screenInsets: Insets,
    scaleX: Double = 1.0,
    scaleY: Double = 1.0,
    autoHideEdges: Set<TaskbarEdge> = emptySet(),
): Rectangle {
    var x = screenBounds.x + screenInsets.left
    var y = screenBounds.y + screenInsets.top
    var width = screenBounds.width - screenInsets.left - screenInsets.right
    var height = screenBounds.height - screenInsets.top - screenInsets.bottom

    val insetX = ceil(AUTO_HIDE_INSET_PHYSICAL_PX / scaleX.coerceAtLeast(1.0)).toInt().coerceAtLeast(1)
    val insetY = ceil(AUTO_HIDE_INSET_PHYSICAL_PX / scaleY.coerceAtLeast(1.0)).toInt().coerceAtLeast(1)

    if (TaskbarEdge.LEFT in autoHideEdges && screenInsets.left == 0 && width > insetX) {
        x += insetX
        width -= insetX
    }
    if (TaskbarEdge.TOP in autoHideEdges && screenInsets.top == 0 && height > insetY) {
        y += insetY
        height -= insetY
    }
    if (TaskbarEdge.RIGHT in autoHideEdges && screenInsets.right == 0 && width > insetX) {
        width -= insetX
    }
    if (TaskbarEdge.BOTTOM in autoHideEdges && screenInsets.bottom == 0 && height > insetY) {
        height -= insetY
    }

    return Rectangle(x, y, width, height)
}

private fun detectWindowsAutoHideTaskbarEdges(window: Window): Set<TaskbarEdge> {
    val isWindows = System.getProperty("os.name")?.contains("win", ignoreCase = true) == true
    if (!isWindows || !window.isDisplayable) return emptySet()

    return runCatching {
        val hwnd = WinDef.HWND(Native.getWindowPointer(window))
        val monitor =
            User32.INSTANCE.MonitorFromWindow(hwnd, WinUser.MONITOR_DEFAULTTONEAREST)
                ?: return@runCatching emptySet()
        val monitorInfo = WinUser.MONITORINFO()
        if (!User32.INSTANCE.GetMonitorInfo(monitor, monitorInfo).booleanValue()) {
            return@runCatching emptySet()
        }

        val edges = detectAutoHideEdgesForMonitor(monitorInfo.rcMonitor)
        if (edges.isNotEmpty()) {
            return@runCatching edges
        }

        detectPrimaryAutoHideFallback(monitorInfo)
    }.getOrElse { emptySet() }
}

private fun detectAutoHideEdgesForMonitor(monitorRect: WinDef.RECT): Set<TaskbarEdge> {
    val edges = mutableSetOf<TaskbarEdge>()
    for (edge in TaskbarEdge.entries) {
        val data =
            ShellAPI.APPBARDATA().apply {
                cbSize = WinDef.DWORD(size().toLong())
                uEdge = WinDef.UINT(edge.win32Edge.toLong())
                rc = copyRect(monitorRect)
            }
        val barHwnd =
            Shell32.INSTANCE.SHAppBarMessage(
                WinDef.DWORD(ABM_GETAUTOHIDEBAREX.toLong()),
                data,
            )
        if (barHwnd.toLong() != 0L) {
            edges += edge
        }
    }
    return edges
}

private fun detectPrimaryAutoHideFallback(monitorInfo: WinUser.MONITORINFO): Set<TaskbarEdge> {
    val isPrimaryMonitor = (monitorInfo.dwFlags and WinUser.MONITORINFOF_PRIMARY) != 0
    if (!isPrimaryMonitor) return emptySet()

    val stateData =
        ShellAPI.APPBARDATA().apply {
            cbSize = WinDef.DWORD(size().toLong())
        }
    val state =
        Shell32.INSTANCE.SHAppBarMessage(
            WinDef.DWORD(ShellAPI.ABM_GETSTATE.toLong()),
            stateData,
        )
    val isAutoHide = (state.toInt() and ABS_AUTOHIDE) != 0

    var detectedEdges = emptySet<TaskbarEdge>()
    if (isAutoHide) {
        val posData =
            ShellAPI.APPBARDATA().apply {
                cbSize = WinDef.DWORD(size().toLong())
            }
        val result =
            Shell32.INSTANCE.SHAppBarMessage(
                WinDef.DWORD(ShellAPI.ABM_GETTASKBARPOS.toLong()),
                posData,
            )
        if (result.toLong() != 0L) {
            detectedEdges = TaskbarEdge.fromWin32Edge(posData.uEdge.toInt())?.let(::setOf) ?: emptySet()
        }
    }
    return detectedEdges
}

private fun copyRect(source: WinDef.RECT): WinDef.RECT =
    WinDef.RECT().apply {
        left = source.left
        top = source.top
        right = source.right
        bottom = source.bottom
    }
