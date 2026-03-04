package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.ClearGraphicsElement
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun renderLibraryScreen(
    state: MeloState,
    favoritesList: ListElement<*>,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    if (state.favorites.isEmpty()) {
        return stack(
            ClearGraphicsElement().fill(),
            panel(
                column(
                    spacer(),
                    text("  Your library is empty").fg(TEXT_SECONDARY).centered(),
                    text("  Press F on any track to add it to your favorites").fg(TEXT_DIM).centered(),
                    spacer()
                )
            ).title("📚 Your Library")
                .rounded()
                .borderColor(BORDER_DEFAULT)
                .focusedBorderColor(BORDER_FOCUSED)
                .fill()
        )
    }

    val items = state.favorites.mapIndexed { index, track ->
        val duration = formatDuration(track.durationMs)
        val nowPlayingIndicator = if (track.id == state.nowPlaying?.id) "♫ " else "  "
        row(
            text(nowPlayingIndicator).fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(track.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
            text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            text(duration).fg(TEXT_DIM).length(6),
        )
    }
    favoritesList.elements(*items.toTypedArray())

    val header = row(
        text("").length(2),
        text("#").dim().length(3),
        text("Title").dim().fill(),
        text("Artist").dim().percent(25),
        text("Time").dim().length(6),
    ).margin(Margin.horizontal(1))

    return stack(
        ClearGraphicsElement().fill(),
        panel(
            column(
                header,
                text("").length(1),
                favoritesList.fill()
            )
        ).title("📚 Your Library  [F] remove")
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("library-panel")
            .onKeyEvent(onKeyEvent)
            .fill()
    )
}