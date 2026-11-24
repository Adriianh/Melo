package com.github.adriianh.melo.util

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MeloWindowSizeClassTest {
    @Test
    fun `compact width below 600dp`() {
        val sizeClass = windowSizeClassForDimensions(width = 412.dp, height = 915.dp)
        assertEquals(WindowWidthClass.Compact, sizeClass.widthClass)
        assertTrue(sizeClass.isCompact)
        assertFalse(sizeClass.isMediumPortrait)
        assertFalse(sizeClass.isExpandedOrMediumLandscape)
    }

    @Test
    fun `medium portrait between 600dp and 839dp with height greater than width`() {
        val sizeClass = windowSizeClassForDimensions(width = 720.dp, height = 1024.dp)
        assertEquals(WindowWidthClass.Medium, sizeClass.widthClass)
        assertFalse(sizeClass.isLandscape)
        assertTrue(sizeClass.isMediumPortrait)
        assertFalse(sizeClass.isExpandedOrMediumLandscape)
    }

    @Test
    fun `medium landscape uses expanded layout mode`() {
        val sizeClass = windowSizeClassForDimensions(width = 800.dp, height = 540.dp)
        assertEquals(WindowWidthClass.Medium, sizeClass.widthClass)
        assertTrue(sizeClass.isLandscape)
        assertFalse(sizeClass.isMediumPortrait)
        assertTrue(sizeClass.isExpandedOrMediumLandscape)
    }

    @Test
    fun `expanded width at or above 840dp`() {
        val sizeClass = windowSizeClassForDimensions(width = 1280.dp, height = 720.dp)
        assertEquals(WindowWidthClass.Expanded, sizeClass.widthClass)
        assertTrue(sizeClass.isExpandedOrMediumLandscape)
    }
}
