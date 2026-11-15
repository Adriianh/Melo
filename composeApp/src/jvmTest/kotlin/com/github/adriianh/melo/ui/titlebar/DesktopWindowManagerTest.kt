package com.github.adriianh.melo.ui.titlebar

import java.awt.Insets
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopWindowManagerTest {
    @Test
    fun `computeMaxTrackBounds expands maximized bounds by frame border metrics around work area`() {
        val frameBorder = FrameBorderInsets(horizontal = 8, vertical = 8)
        val primary =
            computeMaxTrackBounds(
                monitorRect = Win32Rect(0, 0, 1920, 1080),
                workRect = Win32Rect(0, 0, 1920, 1032),
                frameBorder = frameBorder,
            )
        val autoHide =
            computeMaxTrackBounds(
                monitorRect = Win32Rect(1920, 100, 3840, 1180),
                workRect = Win32Rect(1920, 100, 3840, 1180),
                frameBorder = frameBorder,
            )

        assertEquals(MaxTrackBounds(x = -8, y = -8, width = 1936, height = 1048), primary)
        assertEquals(MaxTrackBounds(x = -8, y = -8, width = 1936, height = 1096), autoHide)
    }

    @Test
    fun `computeMaximizedClientRect insets maximized window rect to align flush with monitor work area`() {
        val frameBorder = FrameBorderInsets(horizontal = 8, vertical = 8)
        val fixedTaskbar =
            computeMaximizedClientRect(
                windowRect = Win32Rect(-8, -8, 1928, 1040),
                frameBorder = frameBorder,
            )
        val autoHideTaskbar =
            computeMaximizedClientRect(
                windowRect = Win32Rect(-8, -8, 1928, 1088),
                frameBorder = frameBorder,
            )

        assertEquals(Win32Rect(0, 0, 1920, 1032), fixedTaskbar)
        assertEquals(Win32Rect(0, 0, 1920, 1080), autoHideTaskbar)
    }

    @Test
    fun `computeMaximizedClientRect scales cleanly with HiDPI frame border insets`() {
        val hidpiBorder = FrameBorderInsets(horizontal = 12, vertical = 12)
        val result =
            computeMaximizedClientRect(
                windowRect = Win32Rect(1908, -12, 3852, 1092),
                frameBorder = hidpiBorder,
            )

        assertEquals(Win32Rect(1920, 0, 3840, 1080), result)
    }

    @Test
    fun `computeFallbackMaximizedBounds fills usable screen bounds without extra margin`() {
        val result =
            computeFallbackMaximizedBounds(
                screenBounds = Rectangle(0, 0, 1920, 1080),
                screenInsets = Insets(0, 0, 48, 0),
            )

        assertEquals(Rectangle(0, 0, 1920, 1032), result)
    }
}
