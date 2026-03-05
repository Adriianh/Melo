package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.ICON_QUEUE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun buildQueuePanel(
    state: MeloState,
    queueList: ListElement<*>,
    onKeyEvent: (KeyEvent) -> EventResult = { EventResult.UNHANDLED },
): Element {
    if (state.queue.isEmpty()) {
        return panel(
            column(
                spacer(),
                text("  Queue is empty").fg(TEXT_DIM).centered(),
                text("  Press Q on any track to add it").fg(TEXT_DIM).centered(),
                spacer(),
            )
        ).title("$ICON_QUEUE Queue  [Q] add  [Del] remove  [C] clear")
            .rounded()
            .borderColor(PRIMARY_COLOR)
            .onKeyEvent(onKeyEvent)
    }

    val items = state.queue.mapIndexed { index, track ->
        val isPlaying = index == state.queueIndex
        val indicator = if (isPlaying) "$ICON_NOTE " else "${index + 1}. "
        val titleColor = if (isPlaying) PRIMARY_COLOR else TEXT_PRIMARY
        row(
            text(indicator).fg(PRIMARY_COLOR).length(4),
            text(track.title).fg(titleColor).apply { if (isPlaying) bold() }.ellipsisMiddle().fill(),
            text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            text(formatDuration(track.durationMs)).fg(TEXT_DIM).length(6),
        )
    }

    queueList.elements(*items.toTypedArray())
    if (state.queueIndex >= 0) queueList.selected(state.queueIndex)

    val remaining = state.queue.size - (state.queueIndex + 1).coerceAtLeast(0)
    val title = "$ICON_QUEUE Queue  ${state.queue.size} tracks  ($remaining remaining)  [Q] add  [Del] remove  [C] clear"

    return panel(
        queueList.fill()
    ).title(title)
        .rounded()
        .borderColor(PRIMARY_COLOR)
        .onKeyEvent(onKeyEvent)
}