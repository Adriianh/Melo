package com.github.adriianh.melo.ui.components.controls

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeRevealStateTest {
    @Test
    fun `opening second item in group closes the first`() {
        val group = SwipeRevealGroup()
        val first =
            SwipeRevealState(initialOffsetPx = 0f, group = group).apply {
                maxRevealPx = 152f
            }
        val second =
            SwipeRevealState(initialOffsetPx = 0f, group = group).apply {
                maxRevealPx = 152f
            }

        first.open()
        assertTrue(first.isOpen)

        second.open()
        assertFalse(first.isOpen)
        assertTrue(second.isOpen)
    }

    @Test
    fun `closeAll closes currently open item`() {
        val group = SwipeRevealGroup()
        val state =
            SwipeRevealState(initialOffsetPx = 0f, group = group).apply {
                maxRevealPx = 76f
            }

        state.open()
        assertTrue(state.isOpen)

        group.closeAll()
        assertFalse(state.isOpen)
    }

    @Test
    fun `open does nothing when maxRevealPx is zero`() {
        val group = SwipeRevealGroup()
        val state =
            SwipeRevealState(initialOffsetPx = 0f, group = group).apply {
                maxRevealPx = 0f
            }

        state.open()
        assertFalse(state.isOpen)
    }
}
