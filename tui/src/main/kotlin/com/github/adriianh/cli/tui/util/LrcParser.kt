package com.github.adriianh.cli.tui.util

import com.github.adriianh.core.domain.model.SyncedLine
import com.github.adriianh.core.util.LrcParser as DefaultLrcParser

typealias LrcLine = SyncedLine

object LrcParser {
    fun parse(lrc: String): List<LrcLine> = DefaultLrcParser.parse(lrc)
    fun currentLineIndex(lines: List<LrcLine>, positionMs: Long): Int =
        DefaultLrcParser.currentLineIndex(lines, positionMs)
}