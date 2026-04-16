package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.ScreenState
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.Toolkit.textInput
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.input.TextInputState

fun buildSearchBar(
    inputState: TextInputState,
    state: ScreenState.Search?,
    onSubmit: () -> Unit,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val searchInput =
        textInput(inputState).placeholder("Search for songs, artists...").onSubmit { onSubmit() }
            .fill()

    val searchBarPanel = panel(
        row(
            text("$ICON_NOTE Melo").bold().fg(PRIMARY_COLOR).length(8), searchInput
        )
    ).rounded().borderColor(BORDER_DEFAULT).focusedBorderColor(BORDER_FOCUSED).focusable()
        .id("search-bar").onKeyEvent(onKeyEvent)

    return searchBarPanel
}