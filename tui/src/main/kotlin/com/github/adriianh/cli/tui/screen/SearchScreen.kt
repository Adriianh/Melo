package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.ICON_SEARCH
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SearchTab
import com.github.adriianh.cli.tui.isFavoriteTrack
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.elements.MarkupTextAreaElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun renderSearchScreen(
    state: MeloState,
    resultList: ListElement<*>,
    lyricsArea: MarkupTextAreaElement,
    similarArea: ListElement<*>,
    entityDescriptionArea: MarkupTextAreaElement,
    marqueeText: (String, Int, Int) -> String,
    onResultsKeyEvent: (KeyEvent) -> EventResult,
    onEntityDetailKeyEvent: (KeyEvent) -> EventResult,
    onDetailKeyEvent: (KeyEvent) -> EventResult,
    terminalHeight: Int = 30,
): Element {
    val actualState = state.screen as? ScreenState.Search
        ?: return panel(text("Search screen not active").centered()).rounded()

    return when {
        actualState.isLoading -> panel(
            column(
                spacer(), text("  Searching...").dim().centered(), spacer()
            )
        ).title(" Results ").rounded().borderColor(BORDER_DEFAULT)

        actualState.errorMessage != null -> panel(
            text(actualState.errorMessage).fg(MeloTheme.ACCENT_RED)
        ).title(" Error ").rounded().borderColor(MeloTheme.ACCENT_RED)

        actualState.results.isEmpty() && actualState.albumResults.isEmpty() && actualState.artistResults.isEmpty() && actualState.playlistResults.isEmpty() -> {
            val isOffline = state.isOfflineMode
            val query = actualState.query
            panel(
                column(
                    spacer(),
                    if (isOffline && query.isNotBlank()) {
                        text("  No offline tracks found matching \"$query\"").fg(TEXT_SECONDARY)
                            .centered()
                    } else {
                        row(
                            spacer(), renderSearchTabs(actualState.tab), spacer()
                        )
                    },
                    text("  Search for music to get started").fg(TEXT_SECONDARY).centered(),
                    text("  Press [Tab] or [/] to focus search bar, [1..4] to change tab").fg(
                        TEXT_DIM
                    ).centered(),
                    spacer()
                )
            ).title(if (isOffline) " Offline Search " else " Search ")
                .bottomTitle(" [/] Search  [1..4] Category tabs ")
                .rounded()
                .borderColor(BORDER_DEFAULT)
                .focusedBorderColor(BORDER_FOCUSED)
                .id("results-panel")
                .focusable()
                .onKeyEvent(onResultsKeyEvent)
        }

        else -> renderResultsArea(
            state,
            actualState,
            resultList,
            lyricsArea,
            similarArea,
            entityDescriptionArea,
            marqueeText,
            onResultsKeyEvent,
            onEntityDetailKeyEvent,
            onDetailKeyEvent,
            terminalHeight,
        )
    }
}

private fun renderSearchTabs(activeTab: SearchTab): Element {
    val tabs = listOf(
        SearchTab.SONGS to "1: Songs",
        SearchTab.ALBUMS to "2: Albums",
        SearchTab.ARTISTS to "3: Artists",
        SearchTab.PLAYLISTS to "4: Playlists",
    ).map { (tab, label) ->
        val active = tab == activeTab
        val textLabel = if (active) " [$label] " else "  $label  "
        if (active) text(textLabel).fg(PRIMARY_COLOR).bold()
        else text(textLabel).fg(TEXT_DIM)
    }
    return row(*tabs.toTypedArray())
}

private fun renderResultsArea(
    state: MeloState,
    actualState: ScreenState.Search,
    resultList: ListElement<*>,
    lyricsArea: MarkupTextAreaElement,
    similarArea: ListElement<*>,
    entityDescriptionArea: MarkupTextAreaElement,
    marqueeText: (String, Int, Int) -> String,
    onResultsKeyEvent: (KeyEvent) -> EventResult,
    onEntityDetailKeyEvent: (KeyEvent) -> EventResult,
    onDetailKeyEvent: (KeyEvent) -> EventResult,
    terminalHeight: Int = 30,
): Element {
    val headerItems: Element
    val isPlayable: Boolean
    val currentCategoryCount: Int

    when (actualState.tab) {
        SearchTab.SONGS -> {
            isPlayable = true
            currentCategoryCount = actualState.results.size
            val items = actualState.results.mapIndexed { index, track ->
                val duration = if (track.durationMs > 0L) formatDuration(track.durationMs) else ""
                val nowPlayingIndicator =
                    if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
                val isSelected = index == actualState.selectedIndex
                val titleText =
                    if (isSelected) marqueeText(track.title, state.player.marqueeOffset, 40)
                    else track.title
                val isFav = state.isFavoriteTrack(track)
                val isTrackPlayable = state.isPlayable(track)
                val isBatchSelected = state.selection.isSelected(track.id)
                val rowElements = mutableListOf<Element>()
                rowElements.add(text(nowPlayingIndicator).fg(PRIMARY_COLOR).length(2))
                if (state.selection.isNotEmpty) {
                    rowElements.add(
                        text(if (isBatchSelected) "[x] " else "[ ] ")
                            .fg(if (isBatchSelected) PRIMARY_COLOR else TEXT_DIM)
                            .length(4)
                    )
                }
                rowElements.add(text("${index + 1}").dim().length(3))
                rowElements.add(
                    text(titleText).fg(if (isTrackPlayable) TEXT_PRIMARY else TEXT_DIM)
                        .apply { if (!isSelected) ellipsisMiddle() }.fill()
                )
                rowElements.add(text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25))
                rowElements.add(text(if (isFav) ICON_HEART else " ").fg(PRIMARY_COLOR).length(2))
                rowElements.add(text(duration).fg(TEXT_DIM).length(6))
                row(*rowElements.toTypedArray())
            }
            resultList.elements(*items.toTypedArray())
            val headerElements = mutableListOf<Element>()
            headerElements.add(text("").length(2))
            if (state.selection.isNotEmpty) {
                headerElements.add(text("Sel ").dim().length(4))
            }
            headerElements.add(text("#").dim().length(3))
            headerElements.add(text("Title").dim().fill())
            headerElements.add(text("Artist").dim().percent(25))
            headerElements.add(text(ICON_HEART).dim().length(2))
            headerElements.add(text("Time").dim().length(6))
            headerItems = row(*headerElements.toTypedArray()).margin(Margin.horizontal(1))
        }

        SearchTab.ALBUMS -> {
            isPlayable = false
            currentCategoryCount = actualState.albumResults.size
            val items = actualState.albumResults.mapIndexed { index, album ->
                val isSelected = index == actualState.selectedIndex
                row(
                    text("  ").length(2),
                    text("${index + 1}").dim().length(3),
                    text(album.title).fg(TEXT_PRIMARY).apply { if (!isSelected) ellipsisMiddle() }
                        .fill(),
                    text(album.author).fg(TEXT_SECONDARY).ellipsis().percent(25),
                    text(album.year ?: "").dim().length(6),
                )
            }
            resultList.elements(*items.toTypedArray())
            headerItems = row(
                text("").length(2),
                text("#").dim().length(3),
                text("Album").dim().fill(),
                text("Artist").dim().percent(25),
                text("Year").dim().length(6),
            ).margin(Margin.horizontal(1))
        }

        SearchTab.ARTISTS -> {
            isPlayable = false
            currentCategoryCount = actualState.artistResults.size
            val items = actualState.artistResults.mapIndexed { index, artist ->
                val isSelected = index == actualState.selectedIndex
                row(
                    text("  ").length(2),
                    text("${index + 1}").dim().length(3),
                    text(artist.name).fg(TEXT_PRIMARY).apply { if (!isSelected) ellipsisMiddle() }
                        .fill())
            }
            resultList.elements(*items.toTypedArray())
            headerItems = row(
                text("").length(2), text("#").dim().length(3), text("Artist").dim().fill()
            ).margin(Margin.horizontal(1))
        }

        SearchTab.PLAYLISTS -> {
            isPlayable = false
            currentCategoryCount = actualState.playlistResults.size
            val items = actualState.playlistResults.mapIndexed { index, pl ->
                val isSelected = index == actualState.selectedIndex
                row(
                    text("  ").length(2),
                    text("${index + 1}").dim().length(3),
                    text(pl.title).fg(TEXT_PRIMARY).apply { if (!isSelected) ellipsisMiddle() }
                        .fill(),
                    text(pl.author).fg(TEXT_SECONDARY).ellipsis().percent(25),
                    text("${pl.trackCount ?: 0} tracks").dim().length(10),
                )
            }
            resultList.elements(*items.toTypedArray())
            headerItems = row(
                text("").length(2),
                text("#").dim().length(3),
                text("Playlist").dim().fill(),
                text("Author").dim().percent(25),
                text("Tracks").dim().length(10),
            ).margin(Margin.horizontal(1))
        }
    }

    val countLabel = if (currentCategoryCount > 0) " ($currentCategoryCount)" else ""
    val loadingMoreLabel = if (actualState.isLoadingMore) " Searching... ↓" else ""
    val resultsTitle = " $ICON_SEARCH Results$countLabel$loadingMoreLabel "

    val hasDetail =
        (isPlayable && state.detail.selectedTrack != null) || (!isPlayable && state.detail.selectedEntity != null)
    val detailHint = if (hasDetail) "  [Tab] Detail" else ""

    val helpText = when (actualState.tab) {
        SearchTab.SONGS -> {
            if (state.selection.isNotEmpty) {
                "[Space/v] Toggle  [Ctrl+A] All  [m] Batch (${state.selection.count})  [Esc] Clear"
            } else {
                "[Enter] Play  [m] Options$detailHint  [v] Select  [1..4] Tabs  [/] Search"
            }
        }
        SearchTab.ALBUMS -> "[Enter] Open$detailHint  [1..4] Tabs  [/] Search"
        SearchTab.ARTISTS -> "[Enter] View Artist$detailHint  [1..4] Tabs  [/] Search"
        SearchTab.PLAYLISTS -> "[Enter] Open$detailHint  [1..4] Tabs  [/] Search"
    }

    val listContent = if (currentCategoryCount == 0) {
        column(
            spacer(),
            text("  No ${actualState.tab.name.lowercase()} found").fg(TEXT_DIM).centered(),
            text("  Press [1..4] to switch category or [/] to search").dim().centered(),
            spacer()
        ).fill()
    } else {
        resultList.fill()
    }

    val resultsPanel = panel(
        column(
            row(spacer(), renderSearchTabs(actualState.tab), spacer()),
            text(""),
            headerItems,
            text("").length(1),
            listContent
        )
    ).title(resultsTitle)
        .bottomTitle(helpText)
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("results-panel")
        .onKeyEvent(onResultsKeyEvent)

    return resultsPanel
}