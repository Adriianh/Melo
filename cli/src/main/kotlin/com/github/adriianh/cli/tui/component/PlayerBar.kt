package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element

fun buildPlayerBar(state: MeloState, formatDuration: (Long) -> String): Element {
    val nowPlaying = state.nowPlaying

    val trackInfo = if (nowPlaying != null) {
        row(
            text(if (state.isPlaying) "▶" else "⏸").fg(PRIMARY_COLOR).length(2),
            text(nowPlaying.title).bold().fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
            text(" — ").fg(TEXT_DIM).length(3),
            text(nowPlaying.artist).fg(TEXT_SECONDARY).ellipsis().fill()
        )
    } else {
        text("  No track selected").fg(TEXT_DIM)
    }

    val progressBar = if (nowPlaying != null) {
        val currentMs = (state.progress * nowPlaying.durationMs).toLong()
        val elapsed = formatDuration(currentMs)
        val total = formatDuration(nowPlaying.durationMs)
        row(
            text("$elapsed / $total").fg(TEXT_DIM).length(13),
            lineGauge((state.progress * 100).toInt())
                .filledColor(PRIMARY_COLOR)
                .unfilledColor(TEXT_DIM)
                .fill()
        )
    } else {
        lineGauge(0).filledColor(TEXT_DIM).unfilledColor(TEXT_DIM)
    }

    val volumeBar = row(
        text(if (state.volume > 50) "🔊" else if (state.volume > 0) "🔉" else "🔇").length(2),
        lineGauge(state.volume)
            .filledColor(TEXT_PRIMARY)
            .unfilledColor(TEXT_DIM)
            .length(8)
    )

    return panel(
        row(
            trackInfo.percent(35),
            progressBar.fill(),
            volumeBar.length(12)
        )
    ).rounded().borderColor(BORDER_DEFAULT)
}

