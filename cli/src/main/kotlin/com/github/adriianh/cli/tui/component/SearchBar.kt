package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.input.TextInputState

fun buildSearchBar(
    inputState: TextInputState,
    onSubmit: () -> Unit,
    onKeyEvent: (KeyEvent) -> dev.tamboui.toolkit.event.EventResult,
): Element = panel(
    row(
        text("♫ Melo").bold().fg(PRIMARY_COLOR).length(8),
        textInput(inputState)
            .placeholder("Search for songs, artists...")
            .onSubmit(onSubmit)
            .fill()
    )
).rounded()
    .borderColor(BORDER_DEFAULT)
    .focusedBorderColor(BORDER_FOCUSED)
    .id("search-bar")
    .onKeyEvent(onKeyEvent)

