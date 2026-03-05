package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.ClearGraphicsElement
import com.github.adriianh.cli.tui.LibraryTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_LIBRARY
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import dev.tamboui.layout.Margin
import dev.tamboui.style.Style
import dev.tamboui.text.Line
import dev.tamboui.text.Span
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun renderLibraryScreen(
    state: MeloState,
    favoritesList: ListElement<*>,
    playlistsList: ListElement<*>,
    playlistTracksList: ListElement<*>,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val favTab = tabLabel("$ICON_HEART Favorites", state.libraryTab == LibraryTab.FAVORITES)
    val plTab  = tabLabel("$ICON_LIBRARY Playlists", state.libraryTab == LibraryTab.PLAYLISTS)
    val tabBar = row(favTab, text("  "), plTab, spacer())
        .margin(Margin.horizontal(1))

    val content = when (state.libraryTab) {
        LibraryTab.FAVORITES  -> buildFavoritesContent(state, favoritesList)
        LibraryTab.PLAYLISTS  -> if (state.isInPlaylistDetail)
            buildPlaylistDetailContent(state, playlistTracksList)
        else
            buildPlaylistsContent(state, playlistsList)
    }

    val hints = when (state.libraryTab) {
        LibraryTab.FAVORITES -> "[F] remove  [Q] queue  [1] favorites  [2] playlists"
        LibraryTab.PLAYLISTS -> if (state.isInPlaylistDetail)
            "[Enter] play  [Q] queue  [D] remove  [Esc] back"
        else
            "[Enter] open  [N] new  [R] rename  [D] delete  [P] play all  [1] favorites  [2] playlists"
    }

    val inputRow = if (state.playlistInputMode != PlaylistInputMode.NONE) {
        val label = when (state.playlistInputMode) {
            PlaylistInputMode.CREATE -> "New playlist name: "
            PlaylistInputMode.RENAME -> "Rename to: "
        }
        row(
            text(label).fg(TEXT_SECONDARY).length(label.length),
            text(state.playlistInput + "█").fg(TEXT_PRIMARY).fill(),
        ).margin(Margin.horizontal(1))
    } else null

    val body = if (inputRow != null) {
        column(
            tabBar,
            text("").length(1),
            content,
            inputRow,
        )
    } else {
        column(
            tabBar,
            text("").length(1),
            content,
        )
    }

    return stack(
        ClearGraphicsElement().fill(),
        panel(body)
            .title("$ICON_LIBRARY Your Library  $hints")
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("library-panel")
            .onKeyEvent(onKeyEvent)
            .fill()
    )
}

private fun tabLabel(label: String, active: Boolean): Element =
    text(label).fg(if (active) PRIMARY_COLOR else TEXT_DIM).apply { if (active) bold() }

private fun buildFavoritesContent(state: MeloState, favoritesList: ListElement<*>): Element {
    if (state.favorites.isEmpty()) {
        return column(
            spacer(),
            text("  No favorites yet").fg(TEXT_SECONDARY).centered(),
            text("  Press F on any track to add it").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }
    val items = state.favorites.mapIndexed { index, track ->
        val duration = formatDuration(track.durationMs)
        val indicator = if (track.id == state.nowPlaying?.id) "$ICON_NOTE " else "  "
        row(
            text(indicator).fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(track.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
            text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            text(duration).fg(TEXT_DIM).length(6),
        )
    }
    favoritesList.elements(*items.toTypedArray())

    val header = row(
        text("").length(2),
        text("#").dim().length(3),
        text("Title").dim().fill(),
        text("Artist").dim().percent(25),
        text("Time").dim().length(6),
    ).margin(Margin.horizontal(1))

    return column(
        header,
        text("").length(1),
        favoritesList.fill(),
    )
}

private fun buildPlaylistsContent(state: MeloState, playlistsList: ListElement<*>): Element {
    if (state.playlists.isEmpty()) {
        return column(
            spacer(),
            text("  No playlists yet").fg(TEXT_SECONDARY).centered(),
            text("  Press N to create your first playlist").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }
    val items = state.playlists.map { playlist ->
        val count = "${playlist.trackCount} track${if (playlist.trackCount != 1) "s" else ""}"
        row(
            text(playlist.name).fg(TEXT_PRIMARY).ellipsis().fill(),
            text(count).fg(TEXT_DIM).length(12),
        )
    }
    playlistsList.elements(*items.toTypedArray())

    val header = row(
        text("Name").dim().fill(),
        text("Tracks").dim().length(12),
    ).margin(Margin.horizontal(1))

    return column(
        header,
        text("").length(1),
        playlistsList.fill(),
    )
}

private fun buildPlaylistDetailContent(state: MeloState, tracksList: ListElement<*>): Element {
    val playlist = state.selectedPlaylist
    val tracks   = state.playlistTracks

    val titleLine = Line.from(
        Span.styled(playlist?.name ?: "Playlist", Style.EMPTY.fg(PRIMARY_COLOR).bold()),
        Span.styled("  ${tracks.size} tracks", Style.EMPTY.fg(TEXT_DIM)),
    )

    if (tracks.isEmpty()) {
        return column(
            text(titleLine).length(1),
            text("").length(1),
            spacer(),
            text("  This playlist is empty").fg(TEXT_SECONDARY).centered(),
            text("  Press A on any track to add it here").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }

    val items = tracks.mapIndexed { index, track ->
        val indicator = if (track.id == state.nowPlaying?.id) "$ICON_NOTE " else "  "
        row(
            text(indicator).fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(track.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
            text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            text(formatDuration(track.durationMs)).fg(TEXT_DIM).length(6),
        )
    }
    tracksList.elements(*items.toTypedArray())

    val header = row(
        text("").length(2),
        text("#").dim().length(3),
        text("Title").dim().fill(),
        text("Artist").dim().percent(25),
        text("Time").dim().length(6),
    ).margin(Margin.horizontal(1))

    return column(
        text(titleLine).length(1),
        text("").length(1),
        header,
        text("").length(1),
        tracksList.fill(),
    )
}