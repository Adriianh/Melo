package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import dev.tamboui.style.Color
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun buildCommandBar(
    state: MeloState,
    onKeyEvent: (KeyEvent) -> EventResult
): Element {
    val content = if (state.commandBar.errorMessage != null) {
        row(
            text(" ").bg(Color.RED).fg(Color.WHITE),
            text(state.commandBar.errorMessage).bg(Color.RED).fg(Color.WHITE),
            text(" ").bg(Color.RED).fg(Color.WHITE).fill()
        )
    } else {
        val input = state.commandBar.input
        val cursor = state.commandBar.cursorPosition.coerceIn(0, input.length)
        val beforeCursor = input.substring(0, cursor)
        val afterCursor = input.substring(cursor)

        row(
            text(":").fg(PRIMARY_COLOR),
            text(beforeCursor).fg(TEXT_PRIMARY),
            text("▌").fg(PRIMARY_COLOR),
            text(afterCursor).fg(TEXT_PRIMARY),
            spacer()
        )
    }

    return content
        .focusable()
        .id("command-bar")
        .onKeyEvent(onKeyEvent)
}
