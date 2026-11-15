package com.github.adriianh.melo.ui.titlebar

import com.sun.jna.platform.win32.ShellAPI
import java.awt.Insets
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopWindowManagerTest {
    @Test
    fun `computeMaximizedBounds subtracts standard screen insets when no auto-hide edges exist`() {
        val result =
            computeMaximizedBounds(
                screenBounds = Rectangle(0, 0, 1920, 1080),
                screenInsets = Insets(0, 0, 48, 0),
                autoHideEdges = emptySet(),
            )

        assertEquals(Rectangle(0, 0, 1920, 1032), result)
    }

    @Test
    fun `computeMaximizedBounds leaves 2px gap on bottom edge for auto-hide taskbar at 100 percent scale`() {
        val result =
            computeMaximizedBounds(
                screenBounds = Rectangle(0, 0, 1920, 1080),
                screenInsets = Insets(0, 0, 0, 0),
                scaleX = 1.0,
                scaleY = 1.0,
                autoHideEdges = setOf(TaskbarEdge.BOTTOM),
            )

        assertEquals(Rectangle(0, 0, 1920, 1078), result)
    }

    @Test
    fun `computeMaximizedBounds scales auto-hide gap cleanly across HiDPI factors`() {
        val at150 =
            computeMaximizedBounds(
                screenBounds = Rectangle(0, 0, 1280, 720),
                screenInsets = Insets(0, 0, 0, 0),
                scaleX = 1.5,
                scaleY = 1.5,
                autoHideEdges = setOf(TaskbarEdge.BOTTOM),
            )
        val at200 =
            computeMaximizedBounds(
                screenBounds = Rectangle(0, 0, 1920, 1080),
                screenInsets = Insets(0, 0, 0, 0),
                scaleX = 2.0,
                scaleY = 2.0,
                autoHideEdges = setOf(TaskbarEdge.BOTTOM),
            )

        assertEquals(Rectangle(0, 0, 1280, 718), at150)
        assertEquals(Rectangle(0, 0, 1920, 1079), at200)
    }

    @Test
    fun `computeMaximizedBounds handles top left and right auto-hide edges`() {
        val top =
            computeMaximizedBounds(
                screenBounds = Rectangle(100, 50, 1920, 1080),
                screenInsets = Insets(0, 0, 0, 0),
                autoHideEdges = setOf(TaskbarEdge.TOP),
            )
        val left =
            computeMaximizedBounds(
                screenBounds = Rectangle(100, 50, 1920, 1080),
                screenInsets = Insets(0, 0, 0, 0),
                autoHideEdges = setOf(TaskbarEdge.LEFT),
            )
        val right =
            computeMaximizedBounds(
                screenBounds = Rectangle(100, 50, 1920, 1080),
                screenInsets = Insets(0, 0, 0, 0),
                autoHideEdges = setOf(TaskbarEdge.RIGHT),
            )

        assertEquals(Rectangle(100, 52, 1920, 1078), top)
        assertEquals(Rectangle(102, 50, 1918, 1080), left)
        assertEquals(Rectangle(100, 50, 1918, 1080), right)
    }

    @Test
    fun `computeMaximizedBounds does not shrink edge that already has non-zero inset`() {
        val result =
            computeMaximizedBounds(
                screenBounds = Rectangle(0, 0, 1920, 1080),
                screenInsets = Insets(0, 0, 48, 0),
                autoHideEdges = setOf(TaskbarEdge.BOTTOM),
            )

        assertEquals(Rectangle(0, 0, 1920, 1032), result)
    }

    @Test
    fun `TaskbarEdge maps Win32 ABE constants`() {
        assertEquals(TaskbarEdge.LEFT, TaskbarEdge.fromWin32Edge(ShellAPI.ABE_LEFT))
        assertEquals(TaskbarEdge.TOP, TaskbarEdge.fromWin32Edge(ShellAPI.ABE_TOP))
        assertEquals(TaskbarEdge.RIGHT, TaskbarEdge.fromWin32Edge(ShellAPI.ABE_RIGHT))
        assertEquals(TaskbarEdge.BOTTOM, TaskbarEdge.fromWin32Edge(ShellAPI.ABE_BOTTOM))
        assertNull(TaskbarEdge.fromWin32Edge(99))
    }
}
