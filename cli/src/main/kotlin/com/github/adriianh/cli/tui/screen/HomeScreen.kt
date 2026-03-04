package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.ClearGraphicsElement
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.util.TextMessagesUtil.buildGreeting
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.layout.Constraint
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun renderHomeScreen(
    state: MeloState,
    onSelectTrack: (Track) -> Unit,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val recentSection = if (state.recentTracks.isEmpty()) {
        column(
            spacer(),
            text("  No recently played tracks yet").fg(TEXT_SECONDARY).centered(),
            text("  Start listening from Search").fg(TEXT_DIM).centered(),
            spacer()
        )
    } else {
        val items = state.recentTracks.take(10).map { entry ->
            val track = entry.track
            row(
                text("▸ ").fg(PRIMARY_COLOR).length(2),
                text(track.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
                text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(30),
            )
        }
        column(*items.toTypedArray())
    }.id("recent-section")

    val quickPicksSection = if (state.recentTracks.isEmpty()) {
        column(
            spacer(),
            text("  Play something to get recommendations").fg(TEXT_DIM).centered(),
            spacer()
        )
    } else {
        val items = state.favorites.mapIndexed { index, track ->
            val nowPlayingIndicator = if (track.id == state.nowPlaying?.id) "♫ " else "  "
            row(
                text(nowPlayingIndicator).fg(PRIMARY_COLOR).length(2),
                text("${index + 1}").dim().length(3),
                text(track.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
                text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            )
        }.take(10)
        if (items.isEmpty()) {
            column(
                spacer(),
                text("  Save favorites to see them here").fg(TEXT_DIM).centered(),
                spacer()
            )
        } else {
            column(*items.toTypedArray())
        }
    }.id("favorites-section")

    val greeting = buildGreeting()

    return stack(
        ClearGraphicsElement().fill(),
        panel(
            column(
                text(greeting).bold().fg(PRIMARY_COLOR),
                text(""),
                dock()
                    .left(
                        panel(recentSection)
                            .title("🕘 Recently Played")
                            .rounded()
                            .borderColor(BORDER_DEFAULT)
                            .fill(),
                        Constraint.percentage(60)
                    )
                    .center(
                        panel(quickPicksSection)
                            .title("♥ Favorites")
                            .rounded()
                            .borderColor(BORDER_DEFAULT)
                            .fill()
                    )
                    .fill()
            )
        ).title("Home")
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("home-panel")
            .onKeyEvent(onKeyEvent)
            .fill()
    )
}