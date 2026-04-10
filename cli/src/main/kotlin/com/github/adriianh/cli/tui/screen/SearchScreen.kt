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
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SearchTab
import com.github.adriianh.cli.tui.component.buildDetailPanel
import com.github.adriianh.cli.tui.component.buildEntityDetailPanel
import com.github.adriianh.cli.tui.graphics.ClearGraphicsElement
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.dock
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.stack
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.elements.MarkupTextAreaElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.block.BorderType

fun renderSearchScreen(
    state: MeloState,
    resultList: ListElement<*>,
    entityTracksList: ListElement<*>,
    artistDashboardList: ListElement<*>,
    lyricsArea: MarkupTextAreaElement,
    similarArea: ListElement<*>,
    marqueeText: (String, Int, Int) -> String,
    onResultsKeyEvent: (KeyEvent) -> EventResult,
    onEntityDetailKeyEvent: (KeyEvent) -> EventResult,
    onDetailKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val actualState = state.screen as? ScreenState.Search
        ?: return panel(text("Search screen not active").centered()).rounded()

    return when {
        actualState.isLoading -> panel(
            column(
                spacer(), text("  Searching...").dim().centered(), spacer()
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
                        text("  No offline tracks found matching \"$query\"").fg(TEXT_SECONDARY)
                            .centered()
                    } else {
                        row(
                            spacer(), renderSearchTabs(actualState.tab), spacer()
                        )
                    },
                    text("  Search for music to get started").fg(TEXT_SECONDARY).centered(),
                    text("  Press Tab to focus the search bar, Alt+Right/Left to change category").fg(
                        TEXT_DIM
                    ).centered(),
                    spacer()
                )
            ).title(if (isOffline) "Offline Search" else "Melo").rounded()
                .borderColor(BORDER_DEFAULT)
        }

        else -> renderResultsArea(
            state,
            actualState,
            resultList,
            entityTracksList,
            artistDashboardList,
            lyricsArea,
            similarArea,
            marqueeText,
            onResultsKeyEvent,
            onEntityDetailKeyEvent,
            onDetailKeyEvent
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
    entityTracksList: ListElement<*>,
    artistDashboardList: ListElement<*>,
    lyricsArea: MarkupTextAreaElement,
    similarArea: ListElement<*>,
    marqueeText: (String, Int, Int) -> String,
    onResultsKeyEvent: (KeyEvent) -> EventResult,
    onEntityDetailKeyEvent: (KeyEvent) -> EventResult,
    onDetailKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val headerItems: Element
    val isPlayable: Boolean
    when (actualState.tab) {
        SearchTab.SONGS -> {
            isPlayable = true
            val items = actualState.results.mapIndexed { index, track ->
                val duration = formatDuration(track.durationMs)
                val nowPlayingIndicator =
                    if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
                val isSelected = index == actualState.selectedIndex
                val titleText =
                    if (isSelected) marqueeText(track.title, state.player.marqueeOffset, 40)
                    else track.title
                val isFav = state.collections.favorites.any { it.id == track.id }
                val isTrackPlayable = state.isPlayable(track)
                row(
                    text(nowPlayingIndicator).fg(PRIMARY_COLOR).length(2),
                    text("${index + 1}").dim().length(3),
                    text(titleText).fg(if (isTrackPlayable) TEXT_PRIMARY else TEXT_DIM)
                        .apply { if (!isSelected) ellipsisMiddle() }.fill(),
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

    if (actualState.isInEntityDetail) {
        if (state.detail.selectedEntity is SearchResult.Artist) {
            return buildArtistDashboardPanel(
                state, actualState, artistDashboardList, onEntityDetailKeyEvent, marqueeText
            )
        }

        val tracks = actualState.entityTracks
        val items = tracks.mapIndexed { index, track ->
            val duration = formatDuration(track.durationMs)
            val nowPlayingIndicator =
                if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
            val isSelected = index == entityTracksList.selected()
            val titleText = if (isSelected) marqueeText(track.title, state.player.marqueeOffset, 40)
            else track.title
            val isFav = state.collections.favorites.any { it.id == track.id }
            val isTrackPlayable = state.isPlayable(track)
            row(
                text(nowPlayingIndicator).fg(PRIMARY_COLOR).length(2),
                text("${index + 1}").dim().length(3),
                text(titleText).fg(if (isTrackPlayable) TEXT_PRIMARY else TEXT_DIM)
                    .apply { if (!isSelected) ellipsisMiddle() }.fill(),
                text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
                text(if (isFav) ICON_HEART else " ").fg(PRIMARY_COLOR).length(2),
                text(duration).fg(TEXT_DIM).length(6),
            )
        }
        entityTracksList.elements(*items.toTypedArray())

        val entityPanel = panel(
            column(
                row(
                    text("  Tracks").fg(PRIMARY_COLOR).bold(),
                    spacer(),
                    text("${tracks.size} total  ").dim()
                ), text(""), row(
                    text("").length(2),
                    text("#").dim().length(3),
                    text("Title").dim().fill(),
                    text("Artist").dim().percent(25),
                    text(ICON_HEART).dim().length(2),
                    text("Time").dim().length(6),
                ).margin(Margin.horizontal(1)), text("").length(1), entityTracksList.fill()
            )
        ).title(actualState.entityTitle ?: "Entity Details").rounded().borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED).focusable().id("entity-tracks-list")
            .onKeyEvent(onEntityDetailKeyEvent)

        return dock().center(entityPanel).right(
            buildEntityDetailPanel(state), Constraint.percentage(35)
        )
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
    ).title(resultsTitle).rounded().borderColor(BORDER_DEFAULT).focusedBorderColor(BORDER_FOCUSED)
        .focusable().id("results-panel").onKeyEvent(onResultsKeyEvent)

    return if (isPlayable && state.detail.selectedTrack != null) {
        dock().center(resultsPanel).right(
            buildDetailPanel(state, lyricsArea, similarArea, marqueeText, onDetailKeyEvent),
            Constraint.percentage(35)
        )
    } else if (!isPlayable && state.detail.selectedEntity != null) {
        dock().center(resultsPanel).right(
            buildEntityDetailPanel(state), Constraint.percentage(35)
        )
    } else {
        dock().center(resultsPanel).bottom(ClearGraphicsElement(), Constraint.length(1))
    }
}

private fun buildArtistDashboardPanel(
    state: MeloState,
    actualState: ScreenState.Search,
    artistDashboardList: ListElement<*>,
    onEntityDetailKeyEvent: (KeyEvent) -> EventResult,
    marqueeText: (String, Int, Int) -> String
): Element {
    val artist = state.detail.selectedEntity as SearchResult.Artist

    val headerPanel = panel(
        column(
            text(artist.name).bold().fg(PRIMARY_COLOR),
            if (artist.subscriberCountText.isNullOrBlank() && artist.monthlyListenerCount.isNullOrBlank()) null else text(
                "${artist.subscriberCountText ?: ""} • ${artist.monthlyListenerCount ?: ""}"
            ).fg(
                TEXT_DIM
            ),
            if (artist.description.isNullOrBlank()) null else text(
                artist.description!!.replace(
                    "\n", " "
                )
            ).fg(TEXT_SECONDARY).ellipsis()
        ).margin(Margin.symmetric(1, 1))
    ).borderType(BorderType.ROUNDED)

    val listItems = mutableListOf<StyledElement<*>>()

    actualState.artistDashboardItems.forEachIndexed { index, item ->
        val isSelected = index == artistDashboardList.selected()
        val indicator = if (isSelected) "$ICON_NOTE " else "  "
        val isLeftSelected = isSelected && actualState.artistDashboardX == 0
        val isRightSelected = isSelected && actualState.artistDashboardX == 1

        val drawItemRow =
            { entity: Any?, sectionTitle: String, itemIndex: Int, isSectionSelected: Boolean ->
                val yForSection = actualState.artistDashboardPositions[sectionTitle] ?: 0
                val isItemSelected = isSectionSelected && itemIndex == yForSection
                val itemIndicator = if (isItemSelected) "$ICON_NOTE " else "  "
                when (entity) {
                    is SearchResult.Album -> {
                        row(
                            text(itemIndicator).fg(PRIMARY_COLOR).length(2),
                            text(entity.title).fg(if (isItemSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                                .apply { if (!isItemSelected) ellipsisMiddle() }.fill(),
                            text(entity.year ?: "").dim().length(6)
                        )
                    }

                    is SearchResult.Playlist -> {
                        row(
                            text(itemIndicator).fg(PRIMARY_COLOR).length(2),
                            text(entity.title).fg(if (isItemSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                                .apply { if (!isItemSelected) ellipsisMiddle() }.fill(),
                            text("${entity.trackCount ?: 0} tracks").dim().length(10)
                        )
                    }

                    is SearchResult.Artist -> {
                        row(
                            text(itemIndicator).fg(PRIMARY_COLOR).length(2),
                            text(entity.name).fg(if (isItemSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                                .apply { if (!isItemSelected) ellipsisMiddle() }.fill()
                        )
                    }

                    is SearchResult.Song -> {
                        val track = entity.track
                        val isTrackPlayable = state.isPlayable(track)
                        val durationText =
                            if (track.durationMs > 0L) formatDuration(track.durationMs) else ""
                        row(
                            text(itemIndicator).fg(PRIMARY_COLOR).length(2),
                            text(
                                if (isItemSelected) marqueeText(
                                    track.title, state.player.marqueeOffset, 40
                                ) else track.title
                            ).fg(if (isItemSelected) PRIMARY_COLOR else if (isTrackPlayable) TEXT_PRIMARY else TEXT_DIM)
                                .apply { if (!isItemSelected) ellipsisMiddle() }.fill(),
                            text(track.artist).fg(TEXT_SECONDARY).percent(30).ellipsis(),
                            text(durationText).fg(TEXT_DIM).length(6)
                        )
                    }

                    is Track -> {
                        val isTrackPlayable = state.isPlayable(entity)
                        val durationText =
                            if (entity.durationMs > 0L) formatDuration(entity.durationMs) else ""
                        row(
                            text(itemIndicator).fg(PRIMARY_COLOR).length(2),
                            text(
                                if (isItemSelected) marqueeText(
                                    entity.title, state.player.marqueeOffset, 40
                                ) else entity.title
                            ).fg(if (isItemSelected) PRIMARY_COLOR else if (isTrackPlayable) TEXT_PRIMARY else TEXT_DIM)
                                .apply { if (!isItemSelected) ellipsisMiddle() }.fill(),
                            text(entity.artist).fg(TEXT_SECONDARY).percent(30).ellipsis(),
                            text(durationText).fg(TEXT_DIM).length(6)
                        )
                    }

                    else -> text("Unknown item").dim()
                }
            }

        val drawEntityPanel = { entity: Any?, selected: Boolean ->
            when (entity) {
                null -> {
                    spacer()
                }

                is SearchResult.ArtistSection -> {
                    val y = actualState.artistDashboardPositions[entity.title] ?: 0
                    val offset = maxOf(0, minOf(y - 2, entity.items.size - 5))
                    val visibleItems = entity.items.drop(offset).take(5)
                    val itemsGroup = column(*visibleItems.mapIndexed { i, it ->
                        drawItemRow(
                            it, entity.title, i + offset, selected
                        )
                    }.toTypedArray())
                    panel(itemsGroup).title(entity.title).borderType(BorderType.ROUNDED)
                        .borderColor(if (selected) PRIMARY_COLOR else BORDER_DEFAULT).fill()
                }

                else -> {
                    val element = drawItemRow(entity, "General", 0, selected)
                    panel(element).borderType(BorderType.ROUNDED)
                        .borderColor(if (selected) PRIMARY_COLOR else BORDER_DEFAULT).fill()
                }
            }
        }

        when (item) {
            is String -> listItems.add(
                text("\n  $item").bold().fg(PRIMARY_COLOR)
            )

            is Pair<*, *> -> {
                listItems.add(
                    row(
                        drawEntityPanel(item.first, isLeftSelected),
                        drawEntityPanel(item.second, isRightSelected)
                    )
                )
            }

            is Track -> {
                val nowPlayingIndicator =
                    if (item.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
                val titleText = if (isSelected) marqueeText(
                    item.title, state.player.marqueeOffset, 40
                ) else item.title
                val durationText = if (item.durationMs > 0L) formatDuration(item.durationMs) else ""
                listItems.add(
                    row(
                        text(nowPlayingIndicator).fg(PRIMARY_COLOR).length(2),
                        text(titleText).fg(if (state.isPlayable(item)) TEXT_PRIMARY else TEXT_DIM)
                            .apply { if (!isSelected) ellipsisMiddle() }.fill(),
                        text(item.artist).fg(TEXT_SECONDARY).percent(30).ellipsis(),
                        text(durationText).fg(TEXT_DIM).length(6)
                    )
                )
            }

            is SearchResult.Song -> {
                val track = item.track
                val nowPlayingIndicator =
                    if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
                val titleText = if (isSelected) marqueeText(
                    track.title, state.player.marqueeOffset, 40
                ) else track.title
                val durationText =
                    if (track.durationMs > 0L) formatDuration(track.durationMs) else ""
                listItems.add(
                    row(
                        text(nowPlayingIndicator).fg(PRIMARY_COLOR).length(2),
                        text(titleText).fg(if (state.isPlayable(track)) TEXT_PRIMARY else TEXT_DIM)
                            .apply { if (!isSelected) ellipsisMiddle() }.fill(),
                        text(track.artist).fg(TEXT_SECONDARY).percent(30).ellipsis(),
                        text(durationText).fg(TEXT_DIM).length(6)
                    )
                )
            }

            is SearchResult.Album -> {
                listItems.add(
                    row(
                        text(indicator).fg(PRIMARY_COLOR).length(2),
                        text(item.title).fg(TEXT_PRIMARY)
                            .apply { if (!isSelected) ellipsisMiddle() }.fill(),
                        text(item.year ?: "").dim().length(6)
                    )
                )
            }

            is SearchResult.Playlist -> {
                listItems.add(
                    row(
                        text(indicator).fg(PRIMARY_COLOR).length(2),
                        text(item.title).fg(TEXT_PRIMARY)
                            .apply { if (!isSelected) ellipsisMiddle() }.fill(),
                        text("${item.trackCount ?: 0} tracks").dim().length(10)
                    )
                )
            }

            is SearchResult.Artist -> {
                listItems.add(
                    row(
                        text(indicator).fg(PRIMARY_COLOR).length(2),
                    text(item.name).fg(TEXT_PRIMARY).apply { if (!isSelected) ellipsisMiddle() }
                        .fill()))
            }

            else -> {
                listItems.add(text("Unknown item").dim())
            }
        }
    }

    artistDashboardList.elements(*listItems.toTypedArray())
    artistDashboardList.onKeyEvent(onEntityDetailKeyEvent)

    val dashboardPanel = panel(
        column(
            headerPanel, text(""), artistDashboardList.fill()
        ).fill()
    ).title("Artist Details: ${artist.name}").rounded().borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED).focusable().id("artist-dashboard-list")
        .onKeyEvent(onEntityDetailKeyEvent).fill()

    return stack(
        ClearGraphicsElement().fill(), dashboardPanel
    ).fill()
}