package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_BLUE
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_RED
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_BULLET
import com.github.adriianh.cli.tui.MeloTheme.ICON_ERROR
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_LOADING
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.SECONDARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.component.buildEntityDetailPanel
import com.github.adriianh.cli.tui.isFavoriteTrack
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.filterAndSortTracks
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.dock
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.elements.MarkupTextAreaElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.block.BorderType

fun renderEntityDetailScreen(
    state: MeloState,
    entityTracksList: ListElement<*>,
    artistDashboardList: ListElement<*>,
    entityDescriptionArea: MarkupTextAreaElement,
    marqueeText: (String, Int, Int) -> String,
    onEntityDetailKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val detail = state.screen as? ScreenState.EntityDetail
        ?: return panel(text("Entity details not active").centered()).rounded()

    return if (detail.entity is SearchResult.Artist) {
        renderArtistDetail(detail, state, artistDashboardList, onEntityDetailKeyEvent, marqueeText)
    } else {
        renderTracksDetail(
            detail,
            state,
            entityTracksList,
            entityDescriptionArea,
            marqueeText,
            onEntityDetailKeyEvent
        )
    }
}

private fun renderArtistDetail(
    detail: ScreenState.EntityDetail,
    state: MeloState,
    artistDashboardList: ListElement<*>,
    onEntityDetailKeyEvent: (KeyEvent) -> EventResult,
    marqueeText: (String, Int, Int) -> String,
): Element {
    if (detail.isLoading) {
        return panel(
            column(
                spacer(),
                text("$ICON_LOADING  Loading artist profile for \"${detail.title}\"...").fg(
                    PRIMARY_COLOR
                ).centered(),
                text("Fetching top tracks, albums and singles...").fg(TEXT_DIM).centered(),
                spacer()
            )
        ).title(" Artist: ${detail.title} ")
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("artist-dashboard-list")
            .onKeyEvent(onEntityDetailKeyEvent)
            .fill()
    }

    if (detail.errorMessage != null) {
        return panel(
            column(
                spacer(),
                text("$ICON_ERROR  ${detail.errorMessage}").fg(ACCENT_RED).centered(),
                text("Press Esc to go back").fg(TEXT_DIM).centered(),
                spacer()
            )
        ).title(" Error ")
            .rounded()
            .borderColor(ACCENT_RED)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("artist-dashboard-list")
            .onKeyEvent(onEntityDetailKeyEvent)
            .fill()
    }

    val artist = detail.entity as SearchResult.Artist

    val headerPanel = panel(
        column(
            *listOfNotNull(
                text(artist.name).bold().fg(PRIMARY_COLOR),
                if (artist.subscriberCountText.isNullOrBlank() && artist.monthlyListenerCount.isNullOrBlank()) null else text(
                    "${artist.subscriberCountText ?: ""} • ${artist.monthlyListenerCount ?: ""}"
                ).fg(TEXT_DIM),
                if (artist.description.isNullOrBlank()) null else text(
                    artist.description!!.replace("\n", " ")
                ).fg(TEXT_SECONDARY).ellipsis()
            ).toTypedArray()
        ).margin(Margin.symmetric(1, 1))
    ).borderType(BorderType.ROUNDED)

    val listItems = mutableListOf<StyledElement<*>>()

    detail.artistDashboardItems.forEachIndexed { index, item ->
        val isSelected = index == artistDashboardList.selected()
        val indicator = if (isSelected) "$ICON_BULLET " else "  "
        val isLeftSelected = isSelected && detail.artistDashboardX == 0
        val isRightSelected = isSelected && detail.artistDashboardX == 1

        val drawItemRow =
            { entity: Any?, sectionTitle: String, itemIndex: Int, isSectionSelected: Boolean ->
                val yForSection = detail.artistDashboardPositions[sectionTitle] ?: 0
                val isItemSelected = isSectionSelected && itemIndex == yForSection
                val itemIndicator = if (isItemSelected) "$ICON_BULLET " else "  "
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
                        val count = entity.trackCount ?: entity.songs?.size ?: 0
                        row(
                            text(itemIndicator).fg(PRIMARY_COLOR).length(2),
                            text(entity.title).fg(if (isItemSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                                .apply { if (!isItemSelected) ellipsisMiddle() }.fill(),
                            text(if (count > 0) "$count tracks" else "Playlist").dim().length(12)
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
                    val y = detail.artistDashboardPositions[entity.title] ?: 0
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
                val count = item.trackCount ?: item.songs?.size ?: 0
                listItems.add(
                    row(
                        text(indicator).fg(PRIMARY_COLOR).length(2),
                        text(item.title).fg(TEXT_PRIMARY)
                            .apply { if (!isSelected) ellipsisMiddle() }.fill(),
                        text(if (count > 0) "$count tracks" else "Playlist").dim().length(12)
                    )
                )
            }

            is SearchResult.Artist -> {
                listItems.add(
                    row(
                        text(indicator).fg(PRIMARY_COLOR).length(2),
                        text(item.name).fg(TEXT_PRIMARY).apply { if (!isSelected) ellipsisMiddle() }
                            .fill()
                    )
                )
            }

            else -> {
                listItems.add(text("Unknown item").dim())
            }
        }
    }

    artistDashboardList.elements(*listItems.toTypedArray())
    artistDashboardList.onKeyEvent(onEntityDetailKeyEvent)

    val hasBio = !artist.description.isNullOrBlank()
    val bioHint = if (hasBio) "  [Tab] Bio" else ""
    val footer = row(
        spacer(),
        text("[Enter] Select  [Space] Play  [m] Opt$bioHint  [Esc] Back")
            .fg(TEXT_DIM)
            .ellipsis(),
        spacer()
    ).length(1)

    return panel(
        column(
            headerPanel,
            text("").length(1),
            artistDashboardList.fill(),
            text("").length(1),
            footer
        ).fill()
    ).title("Artist Details: ${artist.name}")
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("artist-dashboard-list")
        .onKeyEvent(onEntityDetailKeyEvent)
        .fill()
}

private fun renderTracksDetail(
    detail: ScreenState.EntityDetail,
    state: MeloState,
    entityTracksList: ListElement<*>,
    entityDescriptionArea: MarkupTextAreaElement,
    marqueeText: (String, Int, Int) -> String,
    onEntityDetailKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val isAlbum = detail.entity is SearchResult.Album
    val typeBadge = if (isAlbum) "[Album]" else "[Playlist]"
    val typeColor = if (isAlbum) ACCENT_BLUE else SECONDARY_COLOR

    val rightPanel = buildEntityDetailPanel(state, entityDescriptionArea, onEntityDetailKeyEvent)

    if (detail.isLoading) {
        val loadingPanel = panel(
            column(
                spacer(),
                text("$ICON_LOADING  Loading tracks for \"${detail.title}\"...").fg(PRIMARY_COLOR)
                    .centered(),
                text("Fetching tracklist from source...").fg(TEXT_DIM).centered(),
                spacer()
            )
        ).title(" ${detail.title} ")
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("entity-tracks-list")
            .onKeyEvent(onEntityDetailKeyEvent)
            .fill()

        return dock().center(loadingPanel).right(rightPanel, Constraint.percentage(35)).fill()
    }

    if (detail.errorMessage != null) {
        val errorPanel = panel(
            column(
                spacer(),
                text("$ICON_ERROR  ${detail.errorMessage}").fg(ACCENT_RED).centered(),
                text("Press Esc to go back").fg(TEXT_DIM).centered(),
                spacer()
            )
        ).title(" Error ")
            .rounded()
            .borderColor(ACCENT_RED)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("entity-tracks-list")
            .onKeyEvent(onEntityDetailKeyEvent)
            .fill()

        return dock().center(errorPanel).right(rightPanel, Constraint.percentage(35)).fill()
    }

    val filteredTracks = filterAndSortTracks(
        tracks = detail.tracks,
        sortOrder = detail.sortOrder,
        sortDirection = detail.sortDirection,
        query = detail.searchQuery
    )

    val searchBadge = if (detail.isTyping) {
        text(" [Search: ${detail.searchQuery}●] ").fg(PRIMARY_COLOR).bold()
    } else if (detail.searchQuery.isNotBlank()) {
        text(" [Search: \"${detail.searchQuery}\"] ").fg(PRIMARY_COLOR)
    } else {
        text(" [Ctrl+F Search] ").fg(TEXT_DIM)
    }

    val sortBadge = text(" [Sort: ${detail.sortOrder.label} (o)] ").fg(TEXT_SECONDARY)
    val dirBadge = text(" [${detail.sortDirection.symbol} ${detail.sortDirection.label} (O)] ").fg(
        TEXT_SECONDARY
    )

    val toolbar = row(
        searchBadge,
        text("  "),
        sortBadge,
        text("  "),
        dirBadge,
        spacer(),
        text("${filteredTracks.size} / ${detail.tracks.size} tracks").dim()
    ).margin(Margin.horizontal(1))

    val subtitleText = detail.subtitle?.let { " • $it" } ?: ""

    val hasBio =
        !detail.description.isNullOrBlank() || (detail.entity as? SearchResult.Album)?.description?.isNotBlank() == true || (detail.entity as? SearchResult.Playlist)?.description?.isNotBlank() == true
    val bioHint = if (hasBio) "  [Tab] Details" else ""

    val footer = if (detail.isTyping) {
        row(
            spacer(),
            text("[Enter] Finish  [Esc] Clear").fg(TEXT_DIM).ellipsis(),
            spacer()
        ).length(1)
    } else if (state.selection.isNotEmpty) {
        row(
            spacer(),
            text("[Space/v] Toggle  [Ctrl+A] All  [m] Batch (${state.selection.count})  [Esc] Clear")
                .fg(TEXT_DIM)
                .ellipsis(),
            spacer()
        ).length(1)
    } else {
        row(
            spacer(),
            text("[Enter] Play  [v] Select  [m] Opt$bioHint  [Esc] Back")
                .fg(TEXT_DIM)
                .ellipsis(),
            spacer()
        ).length(1)
    }

    if (filteredTracks.isEmpty()) {
        entityTracksList.elements()
        val emptyMessage = if (detail.tracks.isEmpty()) {
            column(
                spacer(),
                text("  No tracks available").fg(TEXT_SECONDARY).centered(),
                spacer(),
            )
        } else {
            column(
                spacer(),
                text("  No tracks matching filter").fg(TEXT_SECONDARY).centered(),
                text("  Press Esc to reset search/filters").fg(TEXT_DIM).centered(),
                spacer(),
            )
        }

        val emptyEntityPanel = panel(
            column(
                row(
                    text("  $typeBadge ").fg(typeColor).bold(),
                    text(detail.title).bold().fg(TEXT_PRIMARY).ellipsis(),
                    text(subtitleText).fg(TEXT_SECONDARY).ellipsis(),
                    spacer(),
                    text("${detail.tracks.size} tracks total  ").dim()
                ).length(1),
                text("").length(1),
                toolbar,
                text("").length(1),
                emptyMessage.fill(),
                text("").length(1),
                footer
            )
        ).title(detail.title)
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("entity-tracks-list")
            .onKeyEvent(onEntityDetailKeyEvent)
            .fill()

        return dock()
            .center(emptyEntityPanel)
            .right(rightPanel, Constraint.percentage(35))
            .fill()
    }

    val items = filteredTracks.mapIndexed { index, track ->
        val duration = if (track.durationMs > 0L) formatDuration(track.durationMs) else ""
        val nowPlayingIndicator =
            if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
        val isSelected = index == entityTracksList.selected()
        val titleText = if (isSelected) marqueeText(track.title, state.player.marqueeOffset, 40)
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
    entityTracksList.elements(*items.toTypedArray())

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

    val entityPanel = panel(
        column(
            row(
                text("  $typeBadge ").fg(typeColor).bold(),
                text(detail.title).bold().fg(TEXT_PRIMARY).ellipsis(),
                text(subtitleText).fg(TEXT_SECONDARY).ellipsis(),
                spacer(),
                text("${filteredTracks.size} / ${detail.tracks.size} tracks  ").dim()
            ).length(1),
            text("").length(1),
            toolbar,
            text("").length(1),
            row(*headerElements.toTypedArray()).margin(Margin.horizontal(1)),
            text("").length(1),
            entityTracksList.fill(),
            text("").length(1),
            footer
        )
    ).title(detail.title)
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("entity-tracks-list")
        .onKeyEvent(onEntityDetailKeyEvent)
        .fill()

    return dock()
        .center(entityPanel)
        .right(rightPanel, Constraint.percentage(35))
        .fill()
}