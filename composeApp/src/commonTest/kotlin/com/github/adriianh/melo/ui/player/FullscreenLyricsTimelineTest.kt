package com.github.adriianh.melo.ui.player

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.LyricsFontScale
import com.github.adriianh.core.domain.model.SyncedLine
import com.github.adriianh.melo.ui.player.components.FullscreenLyricEntry
import com.github.adriianh.melo.ui.player.components.FullscreenLyricsStyle
import com.github.adriianh.melo.ui.player.components.NowPlayingLyricsConfig
import com.github.adriianh.melo.ui.player.components.buildFullscreenLyricEntries
import com.github.adriianh.melo.ui.player.components.lyricAlphaForDistance
import com.github.adriianh.melo.ui.player.components.lyricBlurForDistance
import com.github.adriianh.melo.ui.player.components.resolveActiveFullscreenEntryIndex
import com.github.adriianh.melo.ui.player.components.resolveFullscreenLyricsStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FullscreenLyricsTimelineTest {
    @Test
    fun `empty lines produce empty entries and -1 active index`() {
        val entries = buildFullscreenLyricEntries(emptyList())
        assertTrue(entries.isEmpty())
        assertEquals(-1, resolveActiveFullscreenEntryIndex(entries, 1_000L))
    }

    @Test
    fun `intro longer than threshold inserts leading instrumental gap`() {
        val lines =
            listOf(
                SyncedLine(timeMs = 6_000L, text = "First line"),
                SyncedLine(timeMs = 9_000L, text = "Second line"),
            )

        val entries = buildFullscreenLyricEntries(lines)

        assertEquals(3, entries.size)
        val intro = assertIs<FullscreenLyricEntry.InstrumentalGap>(entries[0])
        assertEquals(0L, intro.startTimeMs)
        assertEquals(6_000L, intro.endTimeMs)
        assertEquals(0, resolveActiveFullscreenEntryIndex(entries, 1_000L))
        assertEquals(1, resolveActiveFullscreenEntryIndex(entries, 6_000L))
    }

    @Test
    fun `intro at or below threshold does not insert leading gap`() {
        val lines =
            listOf(
                SyncedLine(timeMs = 4_000L, text = "First line"),
                SyncedLine(timeMs = 7_000L, text = "Second line"),
            )

        val entries = buildFullscreenLyricEntries(lines)

        assertEquals(2, entries.size)
        assertIs<FullscreenLyricEntry.LyricLine>(entries[0])
        assertEquals(-1, resolveActiveFullscreenEntryIndex(entries, 1_000L))
    }

    @Test
    fun `blank LRC line maps to explicit instrumental gap`() {
        val lines =
            listOf(
                SyncedLine(timeMs = 1_000L, text = "Line one"),
                SyncedLine(timeMs = 3_000L, text = "   "),
                SyncedLine(timeMs = 9_000L, text = "Line two"),
            )

        val entries = buildFullscreenLyricEntries(lines)

        assertEquals(3, entries.size)
        val gap = assertIs<FullscreenLyricEntry.InstrumentalGap>(entries[1])
        assertEquals(3_000L, gap.startTimeMs)
        assertEquals(9_000L, gap.endTimeMs)
        assertEquals(1, gap.sourceLineIndex)
        assertEquals(1, resolveActiveFullscreenEntryIndex(entries, 5_000L))
    }

    @Test
    fun `normal lines with large delta do not insert spurious virtual gaps`() {
        val lines =
            listOf(
                SyncedLine(timeMs = 2_000L, text = "Verse end"),
                SyncedLine(timeMs = 10_000L, text = "Chorus start"),
            )

        val entries = buildFullscreenLyricEntries(lines)

        assertEquals(2, entries.size)
        assertIs<FullscreenLyricEntry.LyricLine>(entries[0])
        assertIs<FullscreenLyricEntry.LyricLine>(entries[1])
        assertEquals(0, resolveActiveFullscreenEntryIndex(entries, 5_000L))
        assertEquals(1, resolveActiveFullscreenEntryIndex(entries, 10_000L))
    }

    @Test
    fun `alpha and blur follow progressive distance falloff`() {
        assertEquals(1.0f, lyricAlphaForDistance(0))
        assertEquals(0.55f, lyricAlphaForDistance(1))
        assertEquals(0.40f, lyricAlphaForDistance(2))
        assertEquals(0.30f, lyricAlphaForDistance(3))
        assertEquals(0.20f, lyricAlphaForDistance(4))
        assertEquals(0.20f, lyricAlphaForDistance(8))

        assertEquals(0.dp, lyricBlurForDistance(0))
        assertEquals(0.8.dp, lyricBlurForDistance(1))
        assertEquals(1.4.dp, lyricBlurForDistance(2))
        assertEquals(2.0.dp, lyricBlurForDistance(3))
        assertEquals(2.0.dp, lyricBlurForDistance(6))
    }

    @Test
    fun `disabling active line zoom matches neighbor typography`() {
        val resolved =
            resolveFullscreenLyricsStyle(
                base = FullscreenLyricsStyle.Expanded,
                config = NowPlayingLyricsConfig(activeLineZoom = false),
            )
        assertEquals(resolved.neighborFontSize, resolved.activeFontSize)
        assertEquals(resolved.neighborLineHeight, resolved.activeLineHeight)
    }

    @Test
    fun `font scale adjusts expanded typography proportionally`() {
        val compact =
            resolveFullscreenLyricsStyle(
                base = FullscreenLyricsStyle.Expanded,
                config = NowPlayingLyricsConfig(fontScale = LyricsFontScale.COMPACT),
            )
        val large =
            resolveFullscreenLyricsStyle(
                base = FullscreenLyricsStyle.Expanded,
                config = NowPlayingLyricsConfig(fontScale = LyricsFontScale.LARGE),
            )
        assertEquals((42f * 0.85f).sp, compact.activeFontSize)
        assertEquals((42f * 1.18f).sp, large.activeFontSize)
    }
}
