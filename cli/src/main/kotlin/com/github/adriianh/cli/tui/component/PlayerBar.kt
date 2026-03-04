package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_RED
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import dev.tamboui.style.Style
import dev.tamboui.text.Line
import dev.tamboui.text.Span
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.toolkit.event.EventResult

fun buildPlayerBar(
    state: MeloState,
    formatDuration: (Long) -> String,
    onKeyEvent: (KeyEvent) -> EventResult = { EventResult.UNHANDLED },
): Element {
    val nowPlaying = state.nowPlaying

    val statusIcon = when {
        state.isLoadingAudio -> "⏳"
        state.audioError != null -> "✗"
        state.isPlaying -> "▶"
        else -> "⏸"
    }
    val statusColor = if (state.audioError != null) ACCENT_RED else PRIMARY_COLOR

    val panelTitle = if (nowPlaying != null) {
        val titleStyle = if (state.isPlaying)
            Style.EMPTY.fg(PRIMARY_COLOR).bold()
        else
            Style.EMPTY.fg(TEXT_PRIMARY).bold()
        Line.from(Span.styled(nowPlaying.title, titleStyle))
    } else {
        Line.from(Span.styled("No track", Style.EMPTY.fg(TEXT_DIM)))
    }

    val leftSection = if (nowPlaying != null) {
        row(
            text(statusIcon).fg(statusColor).length(2),
            text(nowPlaying.artist).fg(TEXT_SECONDARY).ellipsis().fill(),
        ).percent(20)
    } else {
        row(
            text(statusIcon).fg(TEXT_DIM).length(2),
            text("Nothing playing").fg(TEXT_DIM).fill(),
        ).percent(20)
    }

    val centerSection = if (nowPlaying != null) {
        val currentMs = (state.progress * nowPlaying.durationMs).toLong()
        val elapsed = formatDuration(currentMs)
        val total = formatDuration(nowPlaying.durationMs)
        row(
            text(elapsed).fg(TEXT_DIM).length(6),
            lineGauge((state.progress * 100).toInt())
                .filledColor(PRIMARY_COLOR)
                .unfilledColor(TEXT_DIM)
                .fill(),
            text(" ").length(1),
            text(total).fg(TEXT_DIM).length(6),
        ).fill()
    } else {
        row(
            text("0:00").fg(TEXT_DIM).length(6),
            lineGauge(0).filledColor(TEXT_DIM).unfilledColor(TEXT_DIM).fill(),
            text(" ").length(1),
            text("0:00").fg(TEXT_DIM).length(6),
        ).fill()
    }

    val volumeIcon = when {
        state.volume == 0 -> "🔇"
        state.volume < 50 -> "🔉"
        else -> "🔊"
    }
    val rightSection = row(
        text(volumeIcon).length(2),
        text(" ").length(1),
        lineGauge(state.volume)
            .filledColor(TEXT_PRIMARY)
            .unfilledColor(TEXT_DIM)
            .fill(),
        text(" ${state.volume}%").fg(TEXT_DIM).length(5),
    ).percent(20)

    val borderColor = if (state.isPlaying) PRIMARY_COLOR else BORDER_DEFAULT

    return panel(leftSection, centerSection, rightSection)
        .horizontal()
        .rounded()
        .borderColor(borderColor)
        .focusedBorderColor(borderColor)
        .onKeyEvent(onKeyEvent)
        .title(panelTitle)
}