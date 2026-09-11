package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.HomeTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_RED
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_ARROW
import com.github.adriianh.cli.tui.MeloTheme.ICON_CLOCK
import com.github.adriianh.cli.tui.MeloTheme.ICON_ERROR
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_LOADING
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.cli.tui.util.TextMessagesUtil.buildGreeting
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.*
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

    val headerRow = row(
        text(buildGreeting()).bold().fg(PRIMARY_COLOR),
        spacer(),
        feedTab,
        text("  ").length(2),
        recentTab,
        text("  ").length(2),
        favTab
    ).margin(Margin.horizontal(1)).length(1)

    val content: StyledElement<*> = when (s.homeTab) {
        HomeTab.FEED -> renderFeedTab(state, s, feedSectionList, feedItemList, onKeyEvent)
        HomeTab.RECENT -> renderRecentTab(state, s, recentList)
        HomeTab.FAVORITES -> renderFavoritesTab(state, s, favoritesList)
    }

    val footer = row(
        spacer(),
        text("[1..3] Tabs  [Tab] Switch Pane  [↑↓] Move  [Enter] Play/Open  [Q] Queue  [F] Favorite  [R] Refresh")
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

private fun tabPill(label: String, active: Boolean): Element =
    if (active) {
        text(" $label ").bold().fg(TEXT_PRIMARY).bg(PRIMARY_COLOR)
    } else {
        text(" $label ").fg(TEXT_DIM)
    }

private fun renderFeedTab(
    state: MeloState,
    s: ScreenState.Home,
    feedSectionList: ListElement<*>,
    feedItemList: ListElement<*>,
    onKeyEvent: (KeyEvent) -> EventResult
): StyledElement<*> {
    if (s.isLoadingFeed) {
        return column(
            spacer(),
            text("$ICON_LOADING  Loading music recommendations...").fg(PRIMARY_COLOR).centered(),
            spacer()
        )
    }

    if (s.feedError != null) {
        return column(
            spacer(),
            text("$ICON_ERROR  ${s.feedError}").fg(ACCENT_RED).centered(),
            text("Press R to retry").fg(TEXT_DIM).centered(),
            spacer()
        )
    }

    if (s.feedSections.isEmpty()) {
        return column(
            spacer(),
            text("No recommendations found").fg(TEXT_SECONDARY).centered(),
            text("Press R to reload feed or search music").fg(TEXT_DIM).centered(),
            spacer()
        )
    }

    // Sections List (Left Pane)
    val sectionItems = s.feedSections.mapIndexed { index, section ->
        val isSelected = index == s.selectedSectionIndex
        row(
            text(if (isSelected) "$ICON_ARROW " else "  ").fg(PRIMARY_COLOR).length(2),
            text(section.title).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                .apply { if (isSelected) bold() }.ellipsis().fill()
        )
    }
    feedSectionList.elements(*sectionItems.toTypedArray())
    feedSectionList.selected(s.selectedSectionIndex)

    val currentSection = s.feedSections.getOrNull(s.selectedSectionIndex)
    val sectionTitle = currentSection?.title ?: "Items"

    val rawItems = currentSection?.items.orEmpty()
    val itemElements = rawItems.mapIndexed { index, item ->
        val isSelected = index == s.selectedItemIndex
        when (item) {
            is SearchResult.Song -> {
                val track = item.track
                val isPlaying = track.id == state.player.nowPlaying?.id
                val indicator = if (isPlaying) "$ICON_NOTE " else "  "
                val isFav = state.collections.favorites.any { it.id == track.id }
                row(
                    text(indicator).fg(PRIMARY_COLOR).length(2),
                    text("[Song]").fg(PRIMARY_COLOR).length(7),
                    text(track.title).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                        .apply { if (isPlaying || isSelected) bold() }.ellipsisMiddle().fill(),
                    text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
                    text(if (isFav) ICON_HEART else " ").fg(PRIMARY_COLOR).length(2),
                    text(formatDuration(track.durationMs)).fg(TEXT_DIM).length(6),
                )
            }

            is SearchResult.Album -> {
                row(
                    text("  ").length(2),
                    text("[Album]").fg(MeloTheme.ACCENT_BLUE).length(8),
                    text(item.title).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                        .apply { if (isSelected) bold() }.ellipsisMiddle().fill(),
                    text(item.author).fg(TEXT_SECONDARY).ellipsis().percent(25),
                    text(item.year ?: "").fg(TEXT_DIM).length(6)
                )
            }

            is SearchResult.Playlist -> {
                row(
                    text("  ").length(2),
                    text("[List]").fg(MeloTheme.SECONDARY_COLOR).length(7),
                    text(item.title).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                        .apply { if (isSelected) bold() }.ellipsisMiddle().fill(),
                    text(item.author).fg(TEXT_SECONDARY).ellipsis().percent(25),
                    text("${item.trackCount ?: 0} tracks").fg(TEXT_DIM).length(10)
                )
            }

            is SearchResult.Artist -> {
                row(
                    text("  ").length(2),
                    text("[Artist]").fg(MeloTheme.SECONDARY_COLOR).length(9),
                    text(item.name).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                        .apply { if (isSelected) bold() }.ellipsis().fill()
                )
            }
        }
    }
    feedItemList.elements(*itemElements.toTypedArray())
    feedItemList.selected(s.selectedItemIndex)

    val leftPane = column(
        text("  SECTIONS").bold().fg(TEXT_DIM).length(1),
        feedSectionList.fill()
    )

    val rightPane = column(
        row(
            text("  $sectionTitle").bold().fg(PRIMARY_COLOR),
            spacer(),
            text("${rawItems.size} items  ").dim()
        ).length(1),
        feedItemList.fill()
    )

    return dock()
        .left(leftPane, Constraint.percentage(28))
        .center(rightPane)
        .fill()
}

private fun renderRecentTab(
    state: MeloState,
    s: ScreenState.Home,
    recentList: ListElement<*>
): StyledElement<*> {
    if (state.collections.recentTracks.isEmpty()) {
        return column(
            spacer(),
            text("$ICON_CLOCK  No recently played tracks yet").fg(TEXT_SECONDARY).centered(),
            text("Start listening from Explore Feed or Search").fg(TEXT_DIM).centered(),
            spacer()
        )
    }

    val items = state.collections.recentTracks.mapIndexed { index, entry ->
        val track = entry.track
        val isPlaying = track.id == state.player.nowPlaying?.id
        val isPlayable = state.isPlayable(track)
        val isSelected = index == s.homeRecentCursor
        val isFav = state.collections.favorites.any { it.id == track.id }
        row(
            text(if (isPlaying) "$ICON_NOTE " else "  ").fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(track.title).fg(if (isSelected) PRIMARY_COLOR else if (isPlayable) TEXT_PRIMARY else TEXT_DIM)
                .apply { if (isPlaying || isSelected) bold() }.ellipsisMiddle().fill(),
            text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            text(if (isFav) ICON_HEART else " ").fg(PRIMARY_COLOR).length(2),
            text(formatDuration(track.durationMs)).fg(TEXT_DIM).length(6)
        )
    }
    recentList.elements(*items.toTypedArray())
    recentList.selected(s.homeRecentCursor)

    return column(
        row(
            text("  #").dim().length(4),
            text("Title").dim().fill(),
            text("Artist").dim().percent(25),
            text(ICON_HEART).dim().length(2),
            text("Time").dim().length(6)
        ).margin(Margin.horizontal(1)),
        text("").length(1),
        recentList.fill()
    ).fill()
}

private fun renderFavoritesTab(
    state: MeloState,
    s: ScreenState.Home,
    favoritesList: ListElement<*>
): StyledElement<*> {
    if (state.collections.favorites.isEmpty()) {
        return column(
            spacer(),
            text("$ICON_HEART  No favorites saved yet").fg(TEXT_SECONDARY).centered(),
            text("Press F on any track to add it to your favorites").fg(TEXT_DIM).centered(),
            spacer()
        )
    }

    val items = state.collections.favorites.mapIndexed { index, track ->
        val isPlaying = track.id == state.player.nowPlaying?.id
        val isPlayable = state.isPlayable(track)
        val isSelected = index == s.homeFavoritesCursor
        row(
            text(if (isPlaying) "$ICON_NOTE " else "  ").fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(track.title).fg(if (isSelected) PRIMARY_COLOR else if (isPlayable) TEXT_PRIMARY else TEXT_DIM)
                .apply { if (isPlaying || isSelected) bold() }.ellipsisMiddle().fill(),
            text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            text(ICON_HEART).fg(PRIMARY_COLOR).length(2),
            text(formatDuration(track.durationMs)).fg(TEXT_DIM).length(6)
        )
    }
    favoritesList.elements(*items.toTypedArray())
    favoritesList.selected(s.homeFavoritesCursor)

    return column(
        row(
            text("  #").dim().length(4),
            text("Title").dim().fill(),
            text("Artist").dim().percent(25),
            text(ICON_HEART).dim().length(2),
            text("Time").dim().length(6)
        ).margin(Margin.horizontal(1)),
        text("").length(1),
        favoritesList.fill()
    ).fill()
}