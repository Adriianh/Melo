package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.SECONDARY_COLOR
import com.github.adriianh.cli.tui.ScreenState
import dev.tamboui.layout.Constraint
import dev.tamboui.toolkit.Toolkit.*
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

    if (state == null || !state.isShowingSuggestions || state.searchSuggestions.isEmpty()) {
        return searchBarPanel
    }

    val suggestionsPanel = panel(
        column(
            *state.searchSuggestions.mapIndexed { index, suggestion ->
                val isSelected = index == state.selectedSuggestionIndex
                row(
                    text(if (isSelected) " > " else "   ").fg(PRIMARY_COLOR),
                    text(suggestion).fg(if (isSelected) PRIMARY_COLOR else SECONDARY_COLOR).fill()
                )
            }.toTypedArray()
        )
    ).rounded().borderColor(BORDER_FOCUSED)

    return stack(
        searchBarPanel,
        dock().top(spacer(), Constraint.length(3)).top(suggestionsPanel, Constraint.fit())
    )
}