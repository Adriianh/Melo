package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.graphics.ClearGraphicsElement
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_CLOCK
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.util.TextMessagesUtil.buildGreeting
import dev.tamboui.layout.Constraint
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun renderHomeScreen(
    state: MeloState,
    recentList: ListElement<*>,
    favoritesList: ListElement<*>,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val recentPanel = if (state.home.recentTracks.isEmpty()) {
        panel(
            column(
                spacer(),
                text("  No recently played tracks yet").fg(TEXT_SECONDARY).centered(),
                text("  Start listening from Search").fg(TEXT_DIM).centered(),
                spacer(),
            )
        )
    } else {
        val items = state.home.recentTracks.take(10).map { entry ->
            val track = entry.track
            val isPlaying = track.id == state.player.nowPlaying?.id
            row(
                text(if (isPlaying) "$ICON_NOTE " else "  ").fg(PRIMARY_COLOR).length(2),
                text(track.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
                text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(30),
            )
        }
        recentList.elements(*items.toTypedArray())
        recentList.selected(state.home.homeRecentCursor)
        panel(recentList.fill())
    }
        .title("$ICON_CLOCK Recently Played")
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("home-recent-panel")
        .onKeyEvent(onKeyEvent)

    val favoritesPanel = if (state.library.favorites.isEmpty()) {
        panel(
            column(
                spacer(),
                text("  Save favorites to see them here").fg(TEXT_DIM).centered(),
                spacer(),
            )
        )
    } else {
        val items = state.library.favorites.take(10).mapIndexed { index, track ->
            val isPlaying = track.id == state.player.nowPlaying?.id
            row(
                text(if (isPlaying) "$ICON_NOTE " else "  ").fg(PRIMARY_COLOR).length(2),
                text("${index + 1}").dim().length(3),
                text(track.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
                text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            )
        }
        favoritesList.elements(*items.toTypedArray())
        favoritesList.selected(state.home.homeFavoritesCursor)
        panel(favoritesList.fill())
    }
        .title("$ICON_HEART Favorites")
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("home-favorites-panel")
        .onKeyEvent(onKeyEvent)

    return stack(
        ClearGraphicsElement().fill(),
        panel(
            column(
                text(buildGreeting()).bold().fg(PRIMARY_COLOR),
                text(""),
                dock()
                    .left(recentPanel.fill(), Constraint.percentage(60))
                    .center(favoritesPanel.fill())
                    .fill()
            )
        ).title("Home")
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("home-panel")
            .fill()
    )
}