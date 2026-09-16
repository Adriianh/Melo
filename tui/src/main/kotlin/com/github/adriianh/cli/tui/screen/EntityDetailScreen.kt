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

    val footer = row(
        spacer(),
        text("[Enter] Select/Play  [↑↓←→] Navigate  [Space] Play  [Q] Queue  [F] Favorite  [D] Bio  [Esc] Back")
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

    val tracks = detail.tracks
    val items = tracks.mapIndexed { index, track ->
        val duration = if (track.durationMs > 0L) formatDuration(track.durationMs) else ""
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

    val subtitleText = detail.subtitle?.let { " • $it" } ?: ""

    val footer = row(
        spacer(),
        text("[Enter] Play Track  [Space] Play All  [Q] Queue  [F] Favorite  [Esc] Back  [D] Bio")
            .fg(TEXT_DIM)
            .ellipsis(),
        spacer()
    ).length(1)

    val entityPanel = panel(
        column(
            row(
                text("  $typeBadge ").fg(typeColor).bold(),
                text(detail.title).bold().fg(TEXT_PRIMARY).ellipsis(),
                text(subtitleText).fg(TEXT_SECONDARY).ellipsis(),
                spacer(),
                text("${tracks.size} tracks total  ").dim()
            ).length(1),
            text(""),
            row(
                text("").length(2),
                text("#").dim().length(3),
                text("Title").dim().fill(),
                text("Artist").dim().percent(25),
                text(ICON_HEART).dim().length(2),
                text("Time").dim().length(6),
            ).margin(Margin.horizontal(1)),
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