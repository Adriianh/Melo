package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_BLUE
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_RED
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_ERROR
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_LOADING
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.ICON_PLAY
import com.github.adriianh.cli.tui.MeloTheme.ICON_RADIO
import com.github.adriianh.cli.tui.MeloTheme.ICON_SHUFFLE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.SECONDARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.component.buildEntityDetailPanel
import com.github.adriianh.cli.tui.isFavoriteEntity
import com.github.adriianh.cli.tui.isFavoriteTrack
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.filterAndSortTracks
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.model.search.entityId
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.dock
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.elements.MarkupTextAreaElement
import dev.tamboui.toolkit.elements.Row
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.block.BorderType

internal fun renderTracksDetail(
    detail: ScreenState.EntityDetail,
    state: MeloState,
    entityTracksList: ListElement<*>,
    entityDescriptionArea: MarkupTextAreaElement,
    marqueeText: (String, Int, Int) -> String,
    onEntityDetailKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val isSaved = state.isFavoriteEntity(detail.entity.entityId)
    val favoriteBadge = if (isSaved) {
        text(" [♥ Liked] ").fg(ACCENT_RED).bold()
    } else {
        text(" [♡ Like (F)] ").fg(TEXT_DIM)
    }

    val totalDurationMs = detail.tracks.sumOf { it.durationMs }
    val totalDurationText =
        if (totalDurationMs > 0L) " • ${formatDuration(totalDurationMs)}" else ""
    val trackCountText = "${detail.tracks.size} track${if (detail.tracks.size != 1) "s" else ""}"

    val heroHeader = buildTracksHeroHeader(
        detail = detail,
        favoriteBadge = favoriteBadge,
        trackCountText = trackCountText,
        totalDurationText = totalDurationText,
    )

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

    val toolbar = buildTracksToolbar(detail, filteredTracks.size)

    val hasBio =
        !detail.description.isNullOrBlank() || (detail.entity as? SearchResult.Album)?.description?.isNotBlank() == true || (detail.entity as? SearchResult.Playlist)?.description?.isNotBlank() == true
    val bioHint = if (hasBio) "  [Tab] Details" else ""

    val footer = buildTracksFooter(detail, state, bioHint)

    if (filteredTracks.isEmpty()) {
        entityTracksList.elements()
        val emptyMessage = buildEmptyTracksMessage(detail)

        val emptyEntityPanel = panel(
            column(
                heroHeader,
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
        buildTrackListRow(
            track = track,
            index = index,
            selectedIndex = entityTracksList.selected(),
            state = state,
            marqueeText = marqueeText,
        )
    }
    entityTracksList.elements(*items.toTypedArray())

    val headerElements = buildTracksColumnHeader(state)

    val entityPanel = panel(
        column(
            heroHeader,
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

/**
 * Builds the hero header shown at the top of an album/playlist detail.
 */
private fun buildTracksHeroHeader(
    detail: ScreenState.EntityDetail,
    favoriteBadge: Element,
    trackCountText: String,
    totalDurationText: String,
): Element {
    val isAlbum = detail.entity is SearchResult.Album
    val typeBadge = if (isAlbum) "[ALBUM]" else "[PLAYLIST]"
    val typeColor = if (isAlbum) ACCENT_BLUE else SECONDARY_COLOR
    return panel(
    column(
        row(
            text(" $typeBadge ").fg(typeColor).bold(),
            text(" "),
            text(detail.title).bold().fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
            text(" "),
            favoriteBadge
        ),
        text("").length(1),
        row(
            text(detail.subtitle ?: (detail.entity as? SearchResult.Album)?.author ?: "").fg(
                TEXT_SECONDARY
            ).ellipsis(),
            text(if (detail.subtitle.isNullOrBlank()) "" else " • ").dim(),
            text("$trackCountText$totalDurationText").dim(),
            spacer(),
            text("[$ICON_PLAY Play (Space)] ").fg(PRIMARY_COLOR),
            text("[$ICON_SHUFFLE Shuffle (s)] ").fg(ACCENT_BLUE),
            text("[$ICON_RADIO Radio (r)] ").fg(TEXT_SECONDARY),
        )
    ).margin(Margin.symmetric(1, 1))
).borderType(BorderType.ROUNDED)
}

/**
 * Builds the search/sort toolbar for the tracks list.
 */
private fun buildTracksToolbar(detail: ScreenState.EntityDetail, filteredCount: Int): Element {
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

    return row(
        searchBadge,
        text("  "),
        sortBadge,
        text("  "),
        dirBadge,
        spacer(),
        text("$filteredCount / ${detail.tracks.size} tracks").dim()
    ).margin(Margin.horizontal(1))
}

/**
 * Builds the contextual footer for the tracks detail.
 */
private fun buildTracksFooter(
    detail: ScreenState.EntityDetail,
    state: MeloState,
    bioHint: String,
): Element = if (detail.isTyping) {
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
        text("[Enter] Play  [s] Shuffle  [r] Radio  [f] Like Track  [F] Like Entity  [m] Opt$bioHint  [Esc] Back")
            .fg(TEXT_DIM)
            .ellipsis(),
        spacer()
    ).length(1)
}

/**
 * Builds the empty-state message when there are no tracks to show.
 */
private fun buildEmptyTracksMessage(detail: ScreenState.EntityDetail) =
    if (detail.tracks.isEmpty()) {
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

/**
 * Builds a single track row for the entity tracks list.
 */
private fun buildTrackListRow(
    track: Track,
    index: Int,
    selectedIndex: Int,
    state: MeloState,
    marqueeText: (String, Int, Int) -> String,
): Row {
    val duration = if (track.durationMs > 0L) formatDuration(track.durationMs) else ""
    val nowPlayingIndicator =
        if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
    val isSelected = index == selectedIndex
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
    return row(*rowElements.toTypedArray())
}

/**
 * Builds the column header row of the tracks list.
 */
private fun buildTracksColumnHeader(state: MeloState): List<Element> {
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
    return headerElements
}