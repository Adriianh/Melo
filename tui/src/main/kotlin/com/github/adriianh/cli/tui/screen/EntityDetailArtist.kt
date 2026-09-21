package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_RED
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_BULLET
import com.github.adriianh.cli.tui.MeloTheme.ICON_ERROR
import com.github.adriianh.cli.tui.MeloTheme.ICON_LOADING
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.ICON_RADIO
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.isFavoriteEntity
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
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
import dev.tamboui.widgets.block.BorderType

internal fun renderArtistDetail(
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
    val isArtistSaved = state.isFavoriteEntity(artist.id)
    val artistFavBadge = if (isArtistSaved) {
        text(" [♥ Following] ").fg(ACCENT_RED).bold()
    } else {
        text(" [♡ Follow (F)] ").fg(TEXT_DIM)
    }

    val headerPanel = buildArtistHeader(artist, artistFavBadge)

    val listItems = buildArtistDashboardItems(
        detail,
        state,
        marqueeText,
        artistDashboardList
    )
    artistDashboardList.elements(*listItems.toTypedArray())
    artistDashboardList.onKeyEvent(onEntityDetailKeyEvent)

    val hasBio = !artist.description.isNullOrBlank()
    val bioHint = if (hasBio) "  [Tab] Bio" else ""
    val footer = row(
        spacer(),
        text("[Enter] Select  [Space] Play  [r] Radio  [F] Follow  [m] Opt$bioHint  [Esc] Back")
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

/**
 * Builds the artist header panel with name, stats and bio.
 */
private fun buildArtistHeader(artist: SearchResult.Artist, artistFavBadge: Element): Element {
    val statsParts = listOfNotNull(
        artist.monthlyListenerCount?.takeIf { it.isNotBlank() }?.let { "$it listeners" },
        artist.subscriberCountText?.takeIf { it.isNotBlank() }?.let { "$it subscribers" }
    )
    val statsText =
        if (statsParts.isNotEmpty()) statsParts.joinToString(" • ") else "Artist Profile"

    val headerElements = mutableListOf<Element>()
    headerElements.add(
        row(
            text(" [ARTIST] ").fg(PRIMARY_COLOR).bold(),
            text(" "),
            text(artist.name).bold().fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
            text(" "),
            artistFavBadge
        )
    )
    headerElements.add(text("").length(1))
    headerElements.add(
        row(
            text(statsText).dim(),
            spacer(),
            text("[$ICON_RADIO Radio (r)] ").fg(TEXT_SECONDARY),
            text("[♥ Follow (F)]").fg(PRIMARY_COLOR),
        )
    )
    if (!artist.description.isNullOrBlank()) {
        headerElements.add(text("").length(1))
        headerElements.add(
            text(artist.description!!.replace("\n", " ")).fg(TEXT_SECONDARY).ellipsis()
        )
    }

    return panel(
        column(*headerElements.toTypedArray()).margin(Margin.symmetric(1, 1))
    ).borderType(BorderType.ROUNDED)
}

/**
 * Builds every row that appears inside a section of the artist dashboard.
 */
private fun artistDashboardRow(
    entity: Any?,
    sectionTitle: String,
    itemIndex: Int,
    isSectionSelected: Boolean,
    detail: ScreenState.EntityDetail,
    state: MeloState,
    marqueeText: (String, Int, Int) -> String,
): Element {
    val yForSection = detail.artistDashboardPositions[sectionTitle] ?: 0
    val isItemSelected = isSectionSelected && itemIndex == yForSection
    val itemIndicator = if (isItemSelected) "$ICON_BULLET " else "  "
    return when (entity) {
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

/**
 * Wraps a dashboard element (or null) inside a rounded panel for the two-column layout.
 */
private fun artistDashboardPanel(
    entity: Any?,
    selected: Boolean,
    detail: ScreenState.EntityDetail,
    state: MeloState,
    marqueeText: (String, Int, Int) -> String,
): Element = when (entity) {
    null -> {
        spacer()
    }

    is SearchResult.ArtistSection -> {
        val y = detail.artistDashboardPositions[entity.title] ?: 0
        val offset = maxOf(0, minOf(y - 2, entity.items.size - 5))
        val visibleItems = entity.items.drop(offset).take(5)
        val itemsGroup = column(*visibleItems.mapIndexed { i, it ->
            artistDashboardRow(
                it, entity.title, i + offset, selected, detail, state, marqueeText
            )
        }.toTypedArray())
        panel(itemsGroup).title(entity.title).borderType(BorderType.ROUNDED)
            .borderColor(if (selected) PRIMARY_COLOR else BORDER_DEFAULT).fill()
    }

    else -> {
        val element = artistDashboardRow(entity, "General", 0, selected, detail, state, marqueeText)
        panel(element).borderType(BorderType.ROUNDED)
            .borderColor(if (selected) PRIMARY_COLOR else BORDER_DEFAULT).fill()
    }
}

/**
 * Builds all the list rows rendered inside the artist dashboard list.
 */
private fun buildArtistDashboardItems(
    detail: ScreenState.EntityDetail,
    state: MeloState,
    marqueeText: (String, Int, Int) -> String,
    artistDashboardList: ListElement<*>,
): MutableList<StyledElement<*>> {
    val listItems = mutableListOf<StyledElement<*>>()

    detail.artistDashboardItems.forEachIndexed { index, item ->
        val isSelected = index == artistDashboardList.selected()
        val indicator = if (isSelected) "$ICON_BULLET " else "  "
        val isLeftSelected = isSelected && detail.artistDashboardX == 0
        val isRightSelected = isSelected && detail.artistDashboardX == 1

        when (item) {
            is String -> listItems.add(
                text("\n  $item").bold().fg(PRIMARY_COLOR)
            )

            is Pair<*, *> -> {
                listItems.add(
                    row(
                        artistDashboardPanel(item.first, isLeftSelected, detail, state, marqueeText),
                        artistDashboardPanel(item.second, isRightSelected, detail, state, marqueeText)
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

    return listItems
}