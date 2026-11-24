package com.github.adriianh.melo.ui.player.components

import com.github.adriianh.core.domain.model.SyncedLine

internal const val INSTRUMENTAL_GAP_THRESHOLD_MS = 4_000L
private const val VIRTUAL_GAP_LEAD_IN_MS = 2_500L
private const val LYRIC_SYNC_LEAD_MS = 150L

internal sealed interface FullscreenLyricEntry {
    val key: String
    val timeMs: Long

    data class LyricLine(
        val lineIndex: Int,
        val line: SyncedLine,
        override val key: String = "line_${line.timeMs}_$lineIndex",
    ) : FullscreenLyricEntry {
        override val timeMs: Long get() = line.timeMs
    }

    data class InstrumentalGap(
        val startTimeMs: Long,
        val endTimeMs: Long,
        val sourceLineIndex: Int? = null,
        override val key: String,
    ) : FullscreenLyricEntry {
        override val timeMs: Long get() = startTimeMs
    }
}

internal fun buildFullscreenLyricEntries(
    lines: List<SyncedLine>,
    gapThresholdMs: Long = INSTRUMENTAL_GAP_THRESHOLD_MS,
): List<FullscreenLyricEntry> {
    if (lines.isEmpty()) return emptyList()

    val entries = ArrayList<FullscreenLyricEntry>(lines.size + 4)
    val first = lines.first()
    if (first.text.isNotBlank() && first.timeMs > gapThresholdMs) {
        entries +=
            FullscreenLyricEntry.InstrumentalGap(
                startTimeMs = 0L,
                endTimeMs = first.timeMs,
                sourceLineIndex = null,
                key = "gap_intro_0",
            )
    }

    for (index in lines.indices) {
        val line = lines[index]
        val next = lines.getOrNull(index + 1)

        if (line.text.isBlank()) {
            entries +=
                FullscreenLyricEntry.InstrumentalGap(
                    startTimeMs = line.timeMs,
                    endTimeMs = next?.timeMs ?: Long.MAX_VALUE,
                    sourceLineIndex = index,
                    key = "gap_blank_${line.timeMs}_$index",
                )
        } else {
            entries +=
                FullscreenLyricEntry.LyricLine(
                    lineIndex = index,
                    line = line,
                )

            if (next != null && next.text.isNotBlank()) {
                val deltaMs = next.timeMs - line.timeMs
                if (deltaMs > gapThresholdMs) {
                    val gapStartMs = line.timeMs + minOf(VIRTUAL_GAP_LEAD_IN_MS, deltaMs / 2L)
                    entries +=
                        FullscreenLyricEntry.InstrumentalGap(
                            startTimeMs = gapStartMs,
                            endTimeMs = next.timeMs,
                            sourceLineIndex = null,
                            key = "gap_virtual_${gapStartMs}_$index",
                        )
                }
            }
        }
    }

    return entries
}

internal fun resolveActiveFullscreenEntryIndex(
    entries: List<FullscreenLyricEntry>,
    positionMs: Long,
    leadMs: Long = LYRIC_SYNC_LEAD_MS,
): Int {
    if (entries.isEmpty()) return -1
    val adjustedMs = positionMs + leadMs
    var active = -1
    for (index in entries.indices) {
        if (entries[index].timeMs <= adjustedMs) {
            active = index
        } else {
            break
        }
    }
    return active
}
