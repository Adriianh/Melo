package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.*

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
import com.github.adriianh.cli.tui.component.buildEntityDetailPanel
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
    lyricsArea: MarkupTextAreaElement,
    similarArea: ListElement<*>,
    marqueeText: (String, Int, Int) -> String,
    onResultsKeyEvent: (KeyEvent) -> EventResult,
    onDetailKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val actualState = state.screen as? ScreenState.Search ?: return panel(text("Search screen not active").centered()).rounded()
    
    return when {
        actualState.isLoading -> panel(
            column(
                spacer(),
                text("  Searching...").dim().centered(),
                spacer()
            )
        ).title("Results").rounded().borderColor(BORDER_DEFAULT)

        actualState.errorMessage != null -> panel(
            text(actualState.errorMessage).fg(MeloTheme.ACCENT_RED)
        ).title("Error").rounded().borderColor(MeloTheme.ACCENT_RED)

        actualState.results.isEmpty() && actualState.albumResults.isEmpty() && actualState.artistResults.isEmpty() && actualState.playlistResults.isEmpty() -> {
            val isOffline = state.isOfflineMode
            val query = actualState.query
            panel(
                column(
                    spacer(),
                    if (isOffline && query.isNotBlank()) {
                        text("  No offline tracks found matching \"$query\"").fg(TEXT_SECONDARY).centered()
                    } else {
                        row(
                            spacer(),
                            renderSearchTabs(actualState.tab),
                            spacer()
                        )
                    },
                    text("  Search for music to get started").fg(TEXT_SECONDARY).centered(),
                    text("  Press Tab to focus the search bar, Alt+Right/Left to change category").fg(TEXT_DIM).centered(),
                    spacer()
                )
            ).title(if (isOffline) "Offline Search" else "Melo").rounded().borderColor(BORDER_DEFAULT)
        }

        else -> renderResultsArea(
            state, actualState, resultList, lyricsArea, similarArea,
            marqueeText, onResultsKeyEvent, onDetailKeyEvent
        )
    }
}

private fun renderSearchTabs(activeTab: SearchTab): Element {
    val items = SearchTab.entries.joinToString("   |   ") { tab ->
        if (tab == activeTab) {
            "$ICON_NOTE ${tab.name}"
        } else {
            "  ${tab.name}"
        }
    }
    return text(items).fg(PRIMARY_COLOR)
}

private fun renderResultsArea(
    state: MeloState,
    actualState: ScreenState.Search,
    resultList: ListElement<*>,
    lyricsArea: MarkupTextAreaElement,
    similarArea: ListElement<*>,
    marqueeText: (String, Int, Int) -> String,
    onResultsKeyEvent: (KeyEvent) -> EventResult,
    onDetailKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val headerItems: Element
    val isPlayable: Boolean
    when (actualState.tab) {
        SearchTab.SONGS -> {
            isPlayable = true
            val items = actualState.results.mapIndexed { index, track ->
                val duration = formatDuration(track.durationMs)
                val nowPlayingIndicator = if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
                val isSelected = index == actualState.selectedIndex
                val titleText = if (isSelected) marqueeText(track.title, state.player.marqueeOffset, 40)
                                else track.title
                val isFav = state.collections.favorites.any { it.id == track.id }
                val isTrackPlayable = state.isPlayable(track)
                row(
                    text(nowPlayingIndicator).fg(PRIMARY_COLOR).length(2),
                    text("${index + 1}").dim().length(3),
                    text(titleText).fg(if (isTrackPlayable) TEXT_PRIMARY else TEXT_DIM).apply { if (!isSelected) ellipsisMiddle() }.fill(),
                    text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
                    text(if (isFav) ICON_HEART else " ").fg(PRIMARY_COLOR).length(2),
                    text(duration).fg(TEXT_DIM).length(6),
                )
            }
            resultList.elements(*items.toTypedArray())
            headerItems = row(
                text("").length(2),
                text("#").dim().length(3),
                text("Title").dim().fill(),
                text("Artist").dim().percent(25),
                text(ICON_HEART).dim().length(2),
                text("Time").dim().length(6),
            ).margin(Margin.horizontal(1))
        }
        SearchTab.ALBUMS -> {
            isPlayable = false
            val items = actualState.albumResults.mapIndexed { index, album ->
                val isSelected = index == actualState.selectedIndex
                row(
                    text("  ").length(2),
                    text("${index + 1}").dim().length(3),
                    text(album.title).fg(TEXT_PRIMARY).apply { if (!isSelected) ellipsisMiddle() }.fill(),
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
            val items = actualState.artistResults.mapIndexed { index, artist ->
                val isSelected = index == actualState.selectedIndex
                row(
                    text("  ").length(2),
                    text("${index + 1}").dim().length(3),
                    text(artist.name).fg(TEXT_PRIMARY).apply { if (!isSelected) ellipsisMiddle() }.fill()
                )
            }
            resultList.elements(*items.toTypedArray())
            headerItems = row(
                text("").length(2),
                text("#").dim().length(3),
                text("Artist").dim().fill()
            ).margin(Margin.horizontal(1))
        }
        SearchTab.PLAYLISTS -> {
            isPlayable = false
            val items = actualState.playlistResults.mapIndexed { index, pl ->
                val isSelected = index == actualState.selectedIndex
                row(
                    text("  ").length(2),
                    text("${index + 1}").dim().length(3),
                    text(pl.title).fg(TEXT_PRIMARY).apply { if (!isSelected) ellipsisMiddle() }.fill(),
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

    val resultsTitle = if (actualState.isLoadingMore) "Searching... ↓" else "Search Results"

    val resultsPanel = panel(
        column(
            row(spacer(), renderSearchTabs(actualState.tab), spacer()),
            text(""),
            headerItems,
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

    return if (isPlayable && state.detail.selectedTrack != null) {
        dock()
            .center(resultsPanel)
            .right(
                buildDetailPanel(state, lyricsArea, similarArea, marqueeText, onDetailKeyEvent),
                Constraint.percentage(35)
            )
    } else if (!isPlayable && state.detail.selectedEntity != null) {
        dock()
            .center(resultsPanel)
            .right(
                buildEntityDetailPanel(state),
                Constraint.percentage(35)
            )
    } else {
        dock()
            .center(resultsPanel)
            .bottom(com.github.adriianh.cli.tui.graphics.ClearGraphicsElement(), Constraint.length(1))
    }
}
