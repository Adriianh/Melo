package com.github.adriianh.cli.tui.util

import com.github.adriianh.cli.tui.*

/**
 * A single line of a synced (LRC) lyrics file.
 * [timeMs] is the timestamp in milliseconds, [text] is the lyric line.
 */
data class LrcLine(val timeMs: Long, val text: String)

/**
 * Parses an LRC-formatted string into a list of [LrcLine].
 *
 * LRC format:
 *   [mm:ss.xx] lyric text
 *   [mm:ss.xxx] lyric text   (milliseconds variant)
 *
 * Lines without a valid timestamp are discarded.
 * The result is sorted by time ascending.
 */
object LrcParser {

    private val LRC_LINE_REGEX = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})](.*)\s*$""")

    fun parse(lrc: String): List<LrcLine> {
        return lrc.lines()
            .mapNotNull { line -> parseLine(line.trim()) }
            .sortedBy { it.timeMs }
    }

    private fun parseLine(line: String): LrcLine? {
        val match = LRC_LINE_REGEX.matchEntire(line) ?: return null
        val (mm, ss, cs, text) = match.destructured
        val minutes = mm.toLongOrNull() ?: return null
        val seconds = ss.toLongOrNull() ?: return null
        val fractionMs = when (cs.length) {
            2 -> cs.toLong() * 10
            3 -> cs.toLong()
            else -> 0L
        }
        val timeMs = minutes * 60_000L + seconds * 1_000L + fractionMs
        return LrcLine(timeMs, text.trim())
    }

    /**
     * Returns the index of the current line given [positionMs].
     * Returns -1 if [lines] is empty or playback hasn't reached the first line yet.
     */
    fun currentLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var index = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) index = i else break
        }
        return index
    }
}