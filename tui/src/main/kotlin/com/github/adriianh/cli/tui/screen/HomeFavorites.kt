package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_BLUE
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.allLibraryFavorites
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.dock
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement

internal fun renderFavoritesTab(
    state: MeloState,
    s: ScreenState.Home,
    favoritesList: ListElement<*>
): StyledElement<*> {
    val allFavorites = state.allLibraryFavorites()
    if (allFavorites.isEmpty()) {
        return column(
            spacer(),
            text("$ICON_HEART  No favorites saved yet").fg(TEXT_SECONDARY).centered(),
            text("Press F on any track to add it to your favorites").fg(TEXT_DIM).centered(),
            spacer()
        )
    }

    val items = allFavorites.mapIndexed { index, item ->
        val track = item.track
        val isPlaying = track.id == state.player.nowPlaying?.id
        val isPlayable = state.isPlayable(track)
        val isSelected = index == s.homeFavoritesCursor
        val providerColor = if (item.isRemote) ACCENT_BLUE else PRIMARY_COLOR
        val isBatchSelected = state.selection.isSelected(track.id)
        val rowElements = mutableListOf<Element>()
        rowElements.add(text("${item.icon} ").fg(providerColor).length(2))
        rowElements.add(text(if (isPlaying) "$ICON_NOTE " else "  ").fg(PRIMARY_COLOR).length(2))
        if (state.selection.isNotEmpty) {
            rowElements.add(
                text(if (isBatchSelected) "[x] " else "[ ] ")
                    .fg(if (isBatchSelected) PRIMARY_COLOR else TEXT_DIM)
                    .length(4)
            )
        }
        rowElements.add(text("${index + 1}").dim().length(3))
        rowElements.add(
            text(track.title).fg(if (isSelected) PRIMARY_COLOR else if (isPlayable) TEXT_PRIMARY else TEXT_DIM)
                .apply { if (isPlaying || isSelected) bold() }.ellipsisMiddle().fill()
        )
        rowElements.add(text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25))
        rowElements.add(text(ICON_HEART).fg(PRIMARY_COLOR).length(2))
        rowElements.add(
            text(if (track.durationMs > 0L) formatDuration(track.durationMs) else "").fg(TEXT_DIM)
                .length(6)
        )
        row(*rowElements.toTypedArray())
    }
    favoritesList.elements(*items.toTypedArray())
    favoritesList.selected(s.homeFavoritesCursor)

    val selectedTrack = allFavorites.getOrNull(s.homeFavoritesCursor)?.track
    val previewPane = buildTrackPreview(selectedTrack, state, "Favorite Track")

    val headerElements = mutableListOf<Element>()
    headerElements.add(text("").length(2))
    headerElements.add(text("").length(2))
    if (state.selection.isNotEmpty) {
        headerElements.add(text("Sel ").dim().length(4))
    }
    headerElements.add(text("#").dim().length(3))
    headerElements.add(text("Title").dim().fill())
    headerElements.add(text("Artist").dim().percent(25))
    headerElements.add(text(ICON_HEART).dim().length(2))
    headerElements.add(text("Time").dim().length(6))

    val centerContent = column(
        row(*headerElements.toTypedArray()).margin(Margin.horizontal(1)),
        text("").length(1),
        favoritesList.fill()
    ).fill()

    return dock()
        .center(centerContent)
        .right(previewPane, Constraint.percentage(32))
        .fill()
}