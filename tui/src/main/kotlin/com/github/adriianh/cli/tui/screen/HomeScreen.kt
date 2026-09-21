package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.HomeFeedFocus
import com.github.adriianh.cli.tui.HomeTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_BLUE
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.util.TextMessagesUtil.buildGreeting
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun renderHomeScreen(
    state: MeloState,
    feedSectionList: ListElement<*>,
    feedItemList: ListElement<*>,
    recentList: ListElement<*>,
    favoritesList: ListElement<*>,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val s = state.screen as? ScreenState.Home
        ?: return panel(text("Home not active").centered()).rounded()

    val feedTab = tabPill("1: Explore Feed", s.homeTab == HomeTab.FEED)
    val recentTab = tabPill("2: Recently Played", s.homeTab == HomeTab.RECENT)
    val favTab = tabPill("3: Favorites", s.homeTab == HomeTab.FAVORITES)

    val userBadge = if (state.youtubeAccountName != null) {
        text("✓ ${state.youtubeAccountName}  ").fg(ACCENT_BLUE).bold()
    } else {
        text("○ Guest  ").fg(TEXT_DIM)
    }

    val headerRow = row(
        text(buildGreeting()).bold().fg(PRIMARY_COLOR),
        spacer(),
        userBadge,
        feedTab,
        text("  ").length(2),
        recentTab,
        text("  ").length(2),
        favTab
    ).margin(Margin.horizontal(1)).length(1)

    val content: StyledElement<*> = when (s.homeTab) {
        HomeTab.FEED -> renderFeedTab(state, s, feedSectionList, feedItemList)
        HomeTab.RECENT -> renderRecentTab(state, s, recentList)
        HomeTab.FAVORITES -> renderFavoritesTab(state, s, favoritesList)
    }

    val footerText = when {
        state.selection.isNotEmpty -> "[Space/v] Toggle  [Ctrl+A] All  [m] Batch (${state.selection.count})  [Esc] Clear"
        s.homeTab == HomeTab.FEED -> when (s.feedFocus) {
            HomeFeedFocus.CHIPS -> "[Tab] Focus  [←→] Moods  [Enter] Apply filter  [↓] Sections  [R] Refresh"
            HomeFeedFocus.SECTIONS -> "[Tab] Focus  [↑↓] Sections  [→/Enter] Items  [[/]] Jump  [1..3] Tabs  [R] Refresh"
            HomeFeedFocus.ITEMS -> "[Tab] Focus  [↑↓] Move  [v] Select  [←/Esc] Sections  [[/]] Jump  [Enter] Play  [Q] Queue  [F] Fav  [M] Options"
        }

        else -> "[1..3] Tabs  [Tab] Switch Tab  [↑↓] Move  [v] Select  [Enter] Play  [Q] Queue  [F] Favorite  [R] Refresh"
    }

    val footer = row(
        spacer(),
        text(footerText)
            .fg(TEXT_DIM)
            .ellipsis(),
        spacer()
    ).length(1)

    return panel(
        column(
            headerRow,
            text("").length(1),
            content.fill(),
            text("").length(1),
            footer
        )
    ).title(" Home ")
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("home-panel")
        .onKeyEvent(onKeyEvent)
        .fill()
}

internal fun tabPill(label: String, active: Boolean): Element =
    if (active) {
        text(" $label ").bold().fg(TEXT_PRIMARY).bg(PRIMARY_COLOR)
    } else {
        text(" $label ").fg(TEXT_DIM)
    }