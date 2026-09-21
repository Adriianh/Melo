package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_BLUE
import com.github.adriianh.cli.tui.MeloTheme.ICON_CLOCK
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.allRecentTracks
import com.github.adriianh.cli.tui.isFavoriteTrack
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

internal fun renderRecentTab(
    state: MeloState,
    s: ScreenState.Home,
    recentList: ListElement<*>,
): StyledElement<*> {
    val recentTracks = state.allRecentTracks()
    if (recentTracks.isEmpty()) {
        return column(
            spacer(),
            text("$ICON_CLOCK  No recently played tracks yet").fg(TEXT_SECONDARY).centered(),
            text("Start listening from Explore Feed or Search").fg(TEXT_DIM).centered(),
            spacer()
        )
    }

    val items = recentTracks.mapIndexed { index, entry ->
        val track = entry.track
        val isPlaying = track.id == state.player.nowPlaying?.id
        val isPlayable = state.isPlayable(track)
        val isSelected = index == s.homeRecentCursor
        val isFav = state.isFavoriteTrack(track)
        val isBatchSelected = state.selection.isSelected(track.id)
        val isRemote = state.collections.remoteRecentTracks.any { it.track.id == track.id } &&
                state.collections.recentTracks.none { it.track.id == track.id }
        val sourceIcon = if (isRemote) "☁" else "⌂"
        val sourceColor = if (isRemote) ACCENT_BLUE else PRIMARY_COLOR
        val rowElements = mutableListOf<Element>()
        rowElements.add(text("$sourceIcon ").fg(sourceColor).length(2))
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
        rowElements.add(text(if (isFav) ICON_HEART else " ").fg(PRIMARY_COLOR).length(2))
        rowElements.add(
            text(if (track.durationMs > 0L) formatDuration(track.durationMs) else "").fg(TEXT_DIM)
                .length(6)
        )
        row(*rowElements.toTypedArray())
    }
    recentList.elements(*items.toTypedArray())
    recentList.selected(s.homeRecentCursor)

    val selectedTrack = recentTracks.getOrNull(s.homeRecentCursor)?.track
    val previewPane = buildTrackPreview(selectedTrack, state, "Recent Track")

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
        recentList.fill()
    ).fill()

    return dock()
        .center(centerContent)
        .right(previewPane, Constraint.percentage(32))
        .fill()
}