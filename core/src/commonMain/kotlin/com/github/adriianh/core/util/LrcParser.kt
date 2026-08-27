package com.github.adriianh.core.util

import com.github.adriianh.core.domain.model.SyncedLine

/**
 * Parses an LRC-formatted string into a list of [SyncedLine].
 *
 * Supports standard LRC timestamps:
 *   [mm:ss.xx] lyric text
 *   [mm:ss.xxx] lyric text
 *
 * Headers (e.g. [ar:Artist], [ti:Title]) and non-timestamped lines are ignored.
 * The result is sorted by timestamp ascending.
 */
object LrcParser {

    private val LRC_LINE_REGEX = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})](.*)$""")

    fun parse(lrc: String?): List<SyncedLine> {
        if (lrc.isNullOrBlank()) return emptyList()
        return lrc.lines()
            .mapNotNull { line -> parseLine(line.trim()) }
            .sortedBy { it.timeMs }
    }

    private fun parseLine(line: String): SyncedLine? {
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
        return SyncedLine(timeMs, text.trim())
    }

    /**
     * Returns the index of the current active line given [positionMs].
     * Returns -1 if [lines] is empty or playback hasn't reached the first line yet.
     */
    fun currentLineIndex(lines: List<SyncedLine>, positionMs: Long, leadMs: Long = 150L): Int {
        if (lines.isEmpty()) return -1
        val adjustedMs = positionMs + leadMs
        var index = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= adjustedMs) {
                index = i
            } else {
                break
            }
        }
        return index
    }
}