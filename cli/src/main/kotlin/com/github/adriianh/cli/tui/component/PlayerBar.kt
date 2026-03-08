package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.*

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_RED
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.ICON_ERROR
import com.github.adriianh.cli.tui.MeloTheme.ICON_LOADING
import com.github.adriianh.cli.tui.MeloTheme.ICON_NEXT
import com.github.adriianh.cli.tui.MeloTheme.ICON_PAUSE
import com.github.adriianh.cli.tui.MeloTheme.ICON_PLAY
import com.github.adriianh.cli.tui.MeloTheme.ICON_PREV
import com.github.adriianh.cli.tui.MeloTheme.ICON_QUEUE
import com.github.adriianh.cli.tui.MeloTheme.ICON_RADIO
import com.github.adriianh.cli.tui.MeloTheme.ICON_REPEAT
import com.github.adriianh.cli.tui.MeloTheme.ICON_REPEAT1
import com.github.adriianh.cli.tui.MeloTheme.ICON_SHUFFLE
import com.github.adriianh.cli.tui.MeloTheme.ICON_VOL_HIGH
import com.github.adriianh.cli.tui.MeloTheme.ICON_VOL_LOW
import com.github.adriianh.cli.tui.MeloTheme.ICON_VOL_MUTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.RepeatMode
import dev.tamboui.layout.Flex
import dev.tamboui.style.Style
import dev.tamboui.text.Line
import dev.tamboui.text.Span
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.tui.event.MouseEventKind

fun buildPlayerBar(
    state: MeloState,
    formatDuration: (Long) -> String,
    onKeyEvent: (KeyEvent) -> EventResult = { EventResult.UNHANDLED },
    onPlayPause: () -> Unit = {},
    onVolumeChange: (Int) -> Unit = {},
    onSeekForward: () -> Unit = {},
    onSeekBackward: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onToggleQueue: () -> Unit = {},
): Element {
    val nowPlaying = state.player.nowPlaying

    val statusIcon = when {
        state.isRestoringSession -> ICON_LOADING
        state.player.isLoadingAudio -> ICON_LOADING
        state.player.audioError != null -> ICON_ERROR
        state.player.isPlaying -> ICON_PLAY
        else -> ICON_PAUSE
    }
    val statusColor = if (state.player.audioError != null) ACCENT_RED else PRIMARY_COLOR

    val panelTitle = if (state.isRestoringSession) {
        Line.from(Span.styled("Resuming session…", Style.EMPTY.fg(TEXT_DIM)))
    } else if (nowPlaying != null) {
        val titleStyle = if (state.player.isPlaying) Style.EMPTY.fg(PRIMARY_COLOR).bold()
            else Style.EMPTY.fg(TEXT_PRIMARY).bold()
        Line.from(Span.styled(nowPlaying.title, titleStyle))
    } else {
        Line.from(Span.styled("No track", Style.EMPTY.fg(TEXT_DIM)))
    }

    val leftTop = if (nowPlaying != null) {
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

    val centerTop = if (nowPlaying != null) {
        val currentMs = (state.player.progress * nowPlaying.durationMs).toLong()
        val elapsed = formatDuration(currentMs)
        val total = formatDuration(nowPlaying.durationMs)
        row(
            text(elapsed).fg(TEXT_DIM).length(6),
            lineGauge((state.player.progress * 100).toInt())
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
        state.player.volume == 0 -> ICON_VOL_MUTE
        state.player.volume < 50 -> ICON_VOL_LOW
        else -> ICON_VOL_HIGH
    }
    val rightTop = row(
        text(volumeIcon).length(2),
        text(" ").length(1),
        lineGauge(state.player.volume)
            .filledColor(TEXT_PRIMARY)
            .unfilledColor(TEXT_DIM)
            .fill()
            .onMouseEvent { event ->
                when (event.kind()) {
                    MouseEventKind.SCROLL_UP   -> { onVolumeChange(5);  EventResult.HANDLED }
                    MouseEventKind.SCROLL_DOWN -> { onVolumeChange(-5); EventResult.HANDLED }
                    else -> EventResult.UNHANDLED
                }
            },
        text(" ${state.player.volume}%").fg(TEXT_DIM).length(5),
    ).percent(20)

    val topRow = row(leftTop, centerTop, rightTop).length(1)

    val controlColor = if (nowPlaying != null && !state.player.isLoadingAudio) PRIMARY_COLOR else TEXT_DIM
    val playPauseIcon = if (state.player.isPlaying) ICON_PAUSE else ICON_PLAY

    val albumText = nowPlaying?.album?.takeIf { it.isNotBlank() } ?: ""
    val leftBottom = row(
        text(albumText).fg(TEXT_DIM).ellipsis().fill(),
    ).percent(20)

    val shuffleColor = if (state.player.shuffleEnabled) PRIMARY_COLOR else TEXT_DIM
    val repeatIcon = when (state.player.repeatMode) {
        RepeatMode.OFF -> ICON_REPEAT
        RepeatMode.ALL -> ICON_REPEAT
        RepeatMode.ONE -> ICON_REPEAT1
    }
    val repeatColor = if (state.player.repeatMode != RepeatMode.OFF) PRIMARY_COLOR else TEXT_DIM
    val queueColor = if (state.player.isQueueVisible) PRIMARY_COLOR else TEXT_DIM
    val queueCount = if (state.player.queue.isNotEmpty()) " ${state.player.queue.size}" else ""

    val centerBottom = row(
        text(ICON_SHUFFLE).fg(shuffleColor).length(2)
            .onMouseEvent { event ->
                if (event.kind() == MouseEventKind.PRESS) { onToggleShuffle(); EventResult.HANDLED }
                else EventResult.UNHANDLED
            },
        text(ICON_PREV).fg(controlColor).length(2)
            .onMouseEvent { event ->
                if (event.kind() == MouseEventKind.PRESS) { onSeekBackward(); EventResult.HANDLED }
                else EventResult.UNHANDLED
            },
        text(playPauseIcon).fg(controlColor).length(2)
            .onMouseEvent { event ->
                if (event.kind() == MouseEventKind.PRESS) { onPlayPause(); EventResult.HANDLED }
                else EventResult.UNHANDLED
            },
        text(ICON_NEXT).fg(controlColor).length(2)
            .onMouseEvent { event ->
                if (event.kind() == MouseEventKind.PRESS) { onSeekForward(); EventResult.HANDLED }
                else EventResult.UNHANDLED
            },
        text(repeatIcon).fg(repeatColor).length(2)
            .onMouseEvent { event ->
                if (event.kind() == MouseEventKind.PRESS) { onCycleRepeat(); EventResult.HANDLED }
                else EventResult.UNHANDLED
            },
    ).flex(Flex.CENTER).spacing(2).fill()

    val rightBottom = row(
        text(if (state.player.isRadioMode) ICON_RADIO else " ").fg(if (state.player.isRadioMode) PRIMARY_COLOR else TEXT_DIM).length(2),
        text(" ").length(1),
        text("$ICON_QUEUE$queueCount").fg(queueColor).length(4)
            .onMouseEvent { event ->
                if (event.kind() == MouseEventKind.PRESS) { onToggleQueue(); EventResult.HANDLED }
                else EventResult.UNHANDLED
            },
    ).percent(20)

    val bottomRow = row(leftBottom, centerBottom, rightBottom).length(1)

    val borderColor = if (state.player.isPlaying) PRIMARY_COLOR else BORDER_DEFAULT
    return panel(topRow, bottomRow)
        .rounded()
        .borderColor(borderColor)
        .focusedBorderColor(borderColor)
        .onKeyEvent(onKeyEvent)
        .title(panelTitle)
}