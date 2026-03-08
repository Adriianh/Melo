package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.component.buildDetailPanel
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.elements.MarkupTextAreaElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun renderSearchScreen(
    state: MeloState,
    resultList: ListElement<*>,
    lyricsArea: dev.tamboui.toolkit.elements.MarkupTextAreaElement,
    similarArea: ListElement<*>,
    marqueeText: (String, Int, Int) -> String,
    onResultsKeyEvent: (KeyEvent) -> EventResult,
    onDetailKeyEvent: (KeyEvent) -> EventResult,
): Element = when {
    state.search.isLoading -> panel(
        column(
            spacer(),
            text("  Searching...").dim().centered(),
            spacer()
        )
    ).title("Results").rounded().borderColor(BORDER_DEFAULT)

    state.search.errorMessage != null -> panel(
        text(state.search.errorMessage).fg(MeloTheme.ACCENT_RED)
    ).title("Error").rounded().borderColor(MeloTheme.ACCENT_RED)

    state.search.results.isEmpty() -> panel(
        column(
            spacer(),
            text("  Search for music to get started").fg(TEXT_SECONDARY).centered(),
            text("  Press Tab to focus the search bar").fg(TEXT_DIM).centered(),
            spacer()
        )
    ).title("Melo").rounded().borderColor(BORDER_DEFAULT)

    else -> renderResultsArea(
        state, resultList, lyricsArea, similarArea,
        marqueeText, onResultsKeyEvent, onDetailKeyEvent
    )
}

private fun renderResultsArea(
    state: MeloState,
    resultList: ListElement<*>,
    lyricsArea: MarkupTextAreaElement,
    similarArea: ListElement<*>,
    marqueeText: (String, Int, Int) -> String,
    onResultsKeyEvent: (KeyEvent) -> EventResult,
    onDetailKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val items = state.search.results.mapIndexed { index, track ->
        val duration = formatDuration(track.durationMs)
        val nowPlayingIndicator = if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
        val isSelected = index == state.search.selectedIndex
        val titleText = if (isSelected) marqueeText(track.title, state.marqueeOffset, 40)
                        else track.title
        val isFav = state.library.favorites.any { it.id == track.id }
        row(
            text(nowPlayingIndicator).fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(titleText).fg(TEXT_PRIMARY).apply { if (!isSelected) ellipsisMiddle() }.fill(),
            text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            text(if (isFav) ICON_HEART else " ").fg(PRIMARY_COLOR).length(2),
            text(duration).fg(TEXT_DIM).length(6),
        )
    }
    resultList.elements(*items.toTypedArray())

    val resultsTitle = if (state.search.isLoadingMore) "Searching... ↓" else "Search Results"

    val header = row(
        text("").length(2),
        text("#").dim().length(3),
        text("Title").dim().fill(),
        text("Artist").dim().percent(25),
        text(ICON_HEART).dim().length(2),
        text("Time").dim().length(6),
    ).margin(Margin.horizontal(1))

    val resultsPanel = panel(
        column(
            header,
            text("").length(1),
            resultList.fill()
        )
    ).title(resultsTitle)
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("results-panel")
        .onKeyEvent(onResultsKeyEvent)

    return if (state.detail.selectedTrack != null) {
        dock()
            .center(resultsPanel)
            .right(
                buildDetailPanel(state, lyricsArea, similarArea, marqueeText, onDetailKeyEvent),
                Constraint.percentage(35)
            )
    } else {
        dock().center(resultsPanel)
    }
}
