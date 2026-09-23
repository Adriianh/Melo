package com.github.adriianh.cli.tui.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EqualizerLogicTest {

    @Test
    fun testAlwaysProducesFourBarsWithinRange() {
        for (tick in 0L..50L) {
            for (progress in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
                for (volume in listOf(0, 1, 25, 50, 75, 100)) {
                    val heights = equalizerHeights(progress, volume, tick)
                    assertEquals(
                        EQUALIZER_COLUMNS,
                        heights.size,
                        "wrong column count at tick=$tick"
                    )
                    assertTrue(
                        heights.all { it in 0..EQUALIZER_MAX_LEVEL },
                        "height out of range at tick=$tick: $heights",
                    )
                }
            }
        }
    }

    @Test
    fun testIsDeterministic() {
        assertEquals(
            equalizerHeights(0.4f, 60, 12345L),
            equalizerHeights(0.4f, 60, 12345L),
        )
    }

    @Test
    fun testZeroVolumeRendersNearSilence() {
        val heights = equalizerHeights(0.5f, 0, 12345L)
        assertTrue(heights.all { it <= 1 }, "muted playback should be a flat line: $heights")
    }

    @Test
    fun testBarsAreIndependentAndAnimated() {
        // Heights must actually fluctuate across ticks (the equalizer is alive).
        val distinctHeights = (0L..60L)
            .flatMap { equalizerHeights(0.5f, 70, it) }
            .distinct()
        assertTrue(distinctHeights.size >= 3, "bars barely move: $distinctHeights")

        // At some instant the columns must differ from each other (independent bars).
        val hasVariedInstant = (0L..60L).any { equalizerHeights(0.5f, 70, it).distinct().size >= 2 }
        assertTrue(hasVariedInstant, "all bars always equal, they are not independent")
    }

    @Test
    fun testMidTrackIsLouderThanTrackEdges() {
        for (tick in 0L..60L) {
            val edge = equalizerHeights(0f, 100, tick)
            val middle = equalizerHeights(0.5f, 100, tick)
            for (i in edge.indices) {
                assertTrue(
                    middle[i] >= edge[i],
                    "tick=$tick col=$i mid=${middle[i]} edge=${edge[i]}",
                )
            }
        }
    }

    @Test
    fun testFrameIsFixedWidthAndUsesLevelCharacters() {
        val frame = equalizerFrame(0.5f, 60, 7L)
        assertEquals(EQUALIZER_COLUMNS, frame.length)
        assertTrue(frame.all { it in EQUALIZER_LEVEL_CHARS }, "unexpected frame: $frame")
    }
}