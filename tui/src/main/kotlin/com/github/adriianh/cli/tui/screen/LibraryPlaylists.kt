package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.LibrarySourceFilter
import com.github.adriianh.cli.tui.LibraryTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_BLUE
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.allLibraryPlaylists
import com.github.adriianh.cli.tui.filteredAndSortedPlaylists
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.filterAndSortTracks
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement

internal fun buildPlaylistsContent(
    state: MeloState,
    actualState: ScreenState.Library,
    playlistsList: ListElement<*>,
    terminalWidth: Int = 120,
): StyledElement<*> {
    val allPlaylists = state.allLibraryPlaylists()
    if (allPlaylists.isEmpty()) {
        return column(
            spacer(),
            text("  No playlists yet").fg(TEXT_SECONDARY).centered(),
            text("  Press N to create a playlist").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }

    val filtered = state.filteredAndSortedPlaylists(
        sourceFilter = actualState.playlistsSourceFilter,
        sortOrder = actualState.playlistsSortOrder,
        sortDirection = actualState.playlistsSortDirection,
        query = actualState.playlistsSearchQuery
    )

    val isCompact = terminalWidth < 95
    val isTyping =
        actualState.isTyping && actualState.libraryTab == LibraryTab.PLAYLISTS && !actualState.isInPlaylistDetail
    val searchBadge = if (isTyping) {
        text(" [Search: ${actualState.playlistsSearchQuery}●] ").fg(PRIMARY_COLOR).bold()
    } else if (actualState.playlistsSearchQuery.isNotBlank()) {
        text(" [Search: \"${actualState.playlistsSearchQuery}\"] ").fg(PRIMARY_COLOR)
    } else {
        text(if (isCompact) " [Ctrl+F] " else " [Ctrl+F Search] ").fg(TEXT_DIM)
    }

    val sourceBadge = when (actualState.playlistsSourceFilter) {
        LibrarySourceFilter.ALL -> text(if (isCompact) " [All] " else " [All (s)] ").fg(TEXT_PRIMARY)
            .bold()

        LibrarySourceFilter.LOCAL -> text(if (isCompact) " [≡] " else " [≡ Local (s)] ").fg(
            PRIMARY_COLOR
        ).bold()

        LibrarySourceFilter.REMOTE -> text(if (isCompact) " [☁] " else " [☁ Cloud (s)] ").fg(
            ACCENT_BLUE
        ).bold()
    }

    val sortBadge =
        text(if (isCompact) " [${actualState.playlistsSortOrder.label}] " else " [Sort: ${actualState.playlistsSortOrder.label} (o)] ").fg(
            TEXT_SECONDARY
        )
    val dirBadge =
        text(if (isCompact) " [${actualState.playlistsSortDirection.symbol}] " else " [${actualState.playlistsSortDirection.symbol} ${actualState.playlistsSortDirection.label} (O)] ").fg(
            TEXT_SECONDARY
        )

    val toolbarElements = mutableListOf<Element>()
    toolbarElements.add(searchBadge)
    toolbarElements.add(text("  "))
    toolbarElements.add(sourceBadge)
    toolbarElements.add(text("  "))
    toolbarElements.add(sortBadge)
    toolbarElements.add(text("  "))
    toolbarElements.add(dirBadge)
    if (!isCompact) {
        toolbarElements.add(spacer())
        toolbarElements.add(text("${filtered.size} playlists").dim())
    }
    val toolbar = row(*toolbarElements.toTypedArray()).margin(Margin.horizontal(1))

    if (filtered.isEmpty()) {
        playlistsList.elements()
        return column(
            toolbar,
            text("").length(1),
            spacer(),
            text("  No playlists matching filter").fg(TEXT_SECONDARY).centered(),
            text("  Press Esc to reset search/filters").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }

    val items = filtered.map { item ->
        val iconColor = if (item.isRemote) ACCENT_BLUE else PRIMARY_COLOR
        val authorColor = if (item.isRemote) ACCENT_BLUE else TEXT_SECONDARY
        row(
            text("${item.icon} ").fg(iconColor).length(2),
            text(item.title).fg(TEXT_PRIMARY).ellipsis().fill(),
            text(item.author).fg(authorColor).ellipsis().percent(25),
            text(item.trackCountText).fg(TEXT_DIM).length(12),
        )
    }
    playlistsList.elements(*items.toTypedArray())

    val header = row(
        text("").length(2),
        text("Name").dim().fill(),
        text("Author").dim().percent(25),
        text("Tracks").dim().length(12),
    ).margin(Margin.horizontal(1))

    return column(
        toolbar,
        text("").length(1),
        header,
        text("").length(1),
        playlistsList.fill(),
    )
}

internal fun buildPlaylistDetailContent(
    state: MeloState,
    actualState: ScreenState.Library,
    tracksList: ListElement<*>
): StyledElement<*> {
    val playlist = actualState.selectedPlaylist
    val tracks   = actualState.playlistTracks

    val titleRow = row(
        text(playlist?.name ?: "Playlist").fg(PRIMARY_COLOR).bold(),
        text("  ${tracks.size} track${if (tracks.size != 1) "s" else ""}").fg(TEXT_DIM),
    )

    if (tracks.isEmpty()) {
        return column(
            titleRow,
            text("").length(1),
            spacer(),
            text("  This playlist is empty").fg(TEXT_SECONDARY).centered(),
            text("  Press A on any track to add it here").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }

    val filtered = filterAndSortTracks(
        tracks = tracks,
        sortOrder = actualState.playlistDetailSortOrder,
        sortDirection = actualState.playlistDetailSortDirection,
        query = actualState.playlistDetailSearchQuery
    )

    val isTyping = actualState.isTyping && actualState.isInPlaylistDetail
    val searchBadge = if (isTyping) {
        text(" [Search: ${actualState.playlistDetailSearchQuery}●] ").fg(PRIMARY_COLOR).bold()
    } else if (actualState.playlistDetailSearchQuery.isNotBlank()) {
        text(" [Search: \"${actualState.playlistDetailSearchQuery}\"] ").fg(PRIMARY_COLOR)
    } else {
        text(" [Ctrl+F Search] ").fg(TEXT_DIM)
    }

    val sortBadge =
        text(" [Sort: ${actualState.playlistDetailSortOrder.label} (o)] ").fg(TEXT_SECONDARY)
    val dirBadge =
        text(" [${actualState.playlistDetailSortDirection.symbol} ${actualState.playlistDetailSortDirection.label} (O)] ").fg(
            TEXT_SECONDARY
        )

    val toolbar = row(
        searchBadge,
        text("  "),
        sortBadge,
        text("  "),
        dirBadge,
        spacer(),
        text("${filtered.size} tracks").dim()
    ).margin(Margin.horizontal(1))

    if (filtered.isEmpty()) {
        tracksList.elements()
        return column(
            titleRow,
            text("").length(1),
            toolbar,
            text("").length(1),
            spacer(),
            text("  No tracks matching filter").fg(TEXT_SECONDARY).centered(),
            text("  Press Esc to reset search/filters").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }

    val items = filtered.mapIndexed { index, track ->
        val indicator = if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
        val isPlayable = state.isPlayable(track)
        val isBatchSelected = state.selection.isSelected(track.id)
        val rowElements = mutableListOf<Element>()
        rowElements.add(text(indicator).fg(PRIMARY_COLOR).length(2))
        if (state.selection.isNotEmpty) {
            rowElements.add(
                text(if (isBatchSelected) "[x] " else "[ ] ")
                    .fg(if (isBatchSelected) PRIMARY_COLOR else TEXT_DIM)
                    .length(4)
            )
        }
        rowElements.add(text("${index + 1}").dim().length(3))
        rowElements.add(
            text(track.title).fg(if (isPlayable) TEXT_PRIMARY else TEXT_DIM).ellipsisMiddle().fill()
        )
        rowElements.add(text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25))
        rowElements.add(
            text(if (track.durationMs > 0L) formatDuration(track.durationMs) else "").fg(
                TEXT_DIM
            ).length(6)
        )
        row(*rowElements.toTypedArray())
    }
    tracksList.elements(*items.toTypedArray())

    val headerElements = mutableListOf<Element>()
    headerElements.add(text("").length(2))
    if (state.selection.isNotEmpty) {
        headerElements.add(text("Sel ").dim().length(4))
    }
    headerElements.add(text("#").dim().length(3))
    headerElements.add(text("Title").dim().fill())
    headerElements.add(text("Artist").dim().percent(25))
    headerElements.add(text("Time").dim().length(6))
    val header = row(*headerElements.toTypedArray()).margin(Margin.horizontal(1))

    return column(
        titleRow,
        text("").length(1),
        toolbar,
        text("").length(1),
        header,
        text("").length(1),
        tracksList.fill(),
    )
}