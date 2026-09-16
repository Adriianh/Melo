package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_RED
import com.github.adriianh.cli.tui.MeloTheme.BG_ELEVATED
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.ICON_ERROR
import com.github.adriianh.cli.tui.MeloTheme.ICON_LOADING
import com.github.adriianh.cli.tui.MeloTheme.ICON_NEXT
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.ICON_PAUSE
import com.github.adriianh.cli.tui.MeloTheme.ICON_PLAY
import com.github.adriianh.cli.tui.MeloTheme.ICON_PREV
import com.github.adriianh.cli.tui.MeloTheme.ICON_QUEUE
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
import dev.tamboui.toolkit.Toolkit.lineGauge
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
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

    val offlineBadge = if (state.isOfflineMode) {
        Span.styled(" [OFFLINE] ", Style.EMPTY.fg(ACCENT_RED).bold())
    } else {
        Span.styled("", Style.EMPTY)
    }
    val panelTitle = Line.from(
        Span.styled(" Melo ", Style.EMPTY.fg(PRIMARY_COLOR).bold()),
        offlineBadge
    )

    val playingIndicator = when {
        state.isRestoringSession -> ICON_LOADING
        state.player.isLoadingAudio -> ICON_LOADING
        state.player.audioError != null -> ICON_ERROR
        state.player.isPlaying -> MeloTheme.getEqualizerFrame(state.player.marqueeOffset.toLong())
        else -> "$ICON_PAUSE "
    }
    val statusColor = if (state.player.audioError != null) ACCENT_RED else PRIMARY_COLOR

    val leftTop = if (nowPlaying != null) {
        row(
            text(playingIndicator).fg(statusColor).length(4),
            text(nowPlaying.title).bold().fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
        ).percent(35)
    } else {
        row(
            text("$ICON_NOTE ").fg(TEXT_DIM).length(2),
            text("Nothing playing").fg(TEXT_DIM).fill(),
        ).percent(35)
    }

    val leftBottom = if (nowPlaying != null) {
        val albumPart = if (nowPlaying.album.isNotBlank()) " • ${nowPlaying.album}" else ""
        row(
            text("    ").length(4),
            text("${nowPlaying.artist}$albumPart").fg(TEXT_SECONDARY).ellipsis().fill(),
        ).percent(35)
    } else {
        row(
            text("  Press / to search music").fg(TEXT_DIM).fill(),
        ).percent(35)
    }

    val centerTop = if (nowPlaying != null) {
        val currentMs = state.player.nowPlayingPositionMs
        val elapsed = formatDuration(currentMs)
        val total =
            if (nowPlaying.durationMs > 0L) formatDuration(nowPlaying.durationMs) else "--:--"
        val gaugePercent = if (nowPlaying.durationMs > 0L) {
            (state.player.progress * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val timeLen = maxOf(5, elapsed.length, total.length)
        row(
            text(elapsed).fg(TEXT_DIM).length(timeLen),
            text(" ").length(1),
            lineGauge(gaugePercent)
                .filledColor(PRIMARY_COLOR)
                .unfilledColor(BG_ELEVATED)
                .fill(),
            text(" ").length(1),
            text(total).fg(TEXT_DIM).length(timeLen),
        ).percent(40)
    } else {
        row(
            text("0:00").fg(TEXT_DIM).length(5),
            text(" ").length(1),
            lineGauge(0).filledColor(TEXT_DIM).unfilledColor(BG_ELEVATED).fill(),
            text(" ").length(1),
            text("0:00").fg(TEXT_DIM).length(5),
        ).percent(40)
    }

    val controlColor =
        if (nowPlaying != null && !state.player.isLoadingAudio) TEXT_PRIMARY else TEXT_DIM
    val playPauseIcon = if (state.player.isPlaying) ICON_PAUSE else ICON_PLAY
    val shuffleColor = if (state.player.shuffleEnabled) PRIMARY_COLOR else TEXT_DIM
    val repeatIcon = when (state.player.repeatMode) {
        RepeatMode.OFF -> ICON_REPEAT
        RepeatMode.ALL -> ICON_REPEAT
        RepeatMode.ONE -> ICON_REPEAT1
    }
    val repeatColor = if (state.player.repeatMode != RepeatMode.OFF) PRIMARY_COLOR else TEXT_DIM

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
        text(playPauseIcon).fg(PRIMARY_COLOR).bold().length(2)
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
    ).flex(Flex.CENTER).spacing(2).percent(40)

    val volumeIcon = when {
        state.player.volume == 0 -> ICON_VOL_MUTE
        state.player.volume < 50 -> ICON_VOL_LOW
        else -> ICON_VOL_HIGH
    }
    val rightTop = row(
        spacer(),
        text(volumeIcon).length(2),
        text(" ").length(1),
        lineGauge(state.player.volume)
            .filledColor(TEXT_PRIMARY)
            .unfilledColor(BG_ELEVATED)
            .length(8)
            .onMouseEvent { event ->
                when (event.kind()) {
                    MouseEventKind.SCROLL_UP -> {
                        onVolumeChange(5); EventResult.HANDLED
                    }

                    MouseEventKind.SCROLL_DOWN -> {
                        onVolumeChange(-5); EventResult.HANDLED
                    }

                    else -> EventResult.UNHANDLED
                }
            },
        text(" ${state.player.volume}%").fg(TEXT_DIM).length(5),
    ).percent(25)

    val queueColor = if (state.player.isQueueVisible) PRIMARY_COLOR else TEXT_DIM
    val queueCount = if (state.player.queue.isNotEmpty()) " (${state.player.queue.size})" else ""

    val rightBottom = row(
        spacer(),
        if (state.player.isRadioMode) text("📻 Radio ").fg(PRIMARY_COLOR) else text(""),
        text("$ICON_QUEUE Queue$queueCount").fg(queueColor)
            .onMouseEvent { event ->
                if (event.kind() == MouseEventKind.PRESS) { onToggleQueue(); EventResult.HANDLED }
                else EventResult.UNHANDLED
            },
    ).percent(25)

    val topRow = row(leftTop, centerTop, rightTop).length(1)
    val bottomRow = row(leftBottom, centerBottom, rightBottom).length(1)

    val borderColor = if (state.player.isPlaying) PRIMARY_COLOR else BORDER_DEFAULT
    return panel(topRow, bottomRow)
        .rounded()
        .borderColor(borderColor)
        .focusedBorderColor(borderColor)
        .onKeyEvent(onKeyEvent)
        .title(panelTitle)
}