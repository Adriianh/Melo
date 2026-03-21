package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.*
import com.github.adriianh.cli.tui.component.SettingsViewState
import com.github.adriianh.cli.tui.graphics.ClearGraphicsElement
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
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import java.io.File
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun renderLibraryScreen(
    state: MeloState,
    settingsViewState: SettingsViewState,
    favoritesList: ListElement<*>,
    playlistsList: ListElement<*>,
    playlistTracksList: ListElement<*>,
    localLibraryList: ListElement<*>,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val actualState = state.screen as? ScreenState.Library ?: return panel(text("Library not active").centered()).rounded()
    
    val favTab = tabLabel("$ICON_HEART Favorites", actualState.libraryTab == LibraryTab.FAVORITES)
    val plTab  = tabLabel("$ICON_LIBRARY Playlists", actualState.libraryTab == LibraryTab.PLAYLISTS)
    val locTab = tabLabel("${MeloTheme.ICON_SEARCH} Local", actualState.libraryTab == LibraryTab.LOCAL)
    val tabBar = row(favTab, text("  "), plTab, text("  "), locTab, spacer())
        .margin(Margin.horizontal(1))

    val content = when (actualState.libraryTab) {
        LibraryTab.FAVORITES -> buildFavoritesContent(state, favoritesList)
        LibraryTab.PLAYLISTS -> if (actualState.isInPlaylistDetail)
            buildPlaylistDetailContent(state, actualState, playlistTracksList)
        else
            buildPlaylistsContent(state, playlistsList)

        LibraryTab.LOCAL -> buildLocalContent(state, settingsViewState, actualState, localLibraryList)
    }

    val hints = when (actualState.libraryTab) {
        LibraryTab.FAVORITES -> "[F] remove  [Q] queue  [A] add to playlist  [1..3] tabs"
        LibraryTab.PLAYLISTS -> if (actualState.isInPlaylistDetail)
            "[Enter] play  [Q] queue  [D] remove  [Esc] back"
        else
            "[Enter] open  [N] new  [R] rename  [D] delete  [P] play all  [1..3] tabs"
        LibraryTab.LOCAL -> if (actualState.isTyping) "[Enter] finish  [Esc] clear  [Bksp] del"
            else "[Tab/f/l] directory  [/] search  [Esc] clear  [Enter] play  [Q] queue  [1..3] tabs"
    }

    val body = column(
        tabBar,
        text("").length(1),
        content,
    )

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
    if (state.collections.favorites.isEmpty()) {
        return column(
            spacer(),
            text("  No favorites yet").fg(TEXT_SECONDARY).centered(),
            text("  Press F on any track to add it").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }
    val items = state.collections.favorites.mapIndexed { index, track ->
        val duration = formatDuration(track.durationMs)
        val indicator = if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
        val isPlayable = state.isPlayable(track)
        row(
            text(indicator).fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(track.title).fg(if (isPlayable) TEXT_PRIMARY else TEXT_DIM).ellipsisMiddle().fill(),
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
    if (state.collections.playlists.isEmpty()) {
        return column(
            spacer(),
            text("  No playlists yet").fg(TEXT_SECONDARY).centered(),
            text("  Press N to create your first playlist").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }
    val items = state.collections.playlists.map { playlist ->
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

private fun buildPlaylistDetailContent(state: MeloState, actualState: ScreenState.Library, tracksList: ListElement<*>): Element {
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

    val items = tracks.mapIndexed { index, track ->
        val indicator = if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
        val isPlayable = state.isPlayable(track)
        row(
            text(indicator).fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(track.title).fg(if (isPlayable) TEXT_PRIMARY else TEXT_DIM).ellipsisMiddle().fill(),
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
        titleRow,
        text("").length(1),
        header,
        text("").length(1),
        tracksList.fill(),
    )
}

private fun buildLocalContent(
    state: MeloState,
    settingsViewState: SettingsViewState,
    actualState: ScreenState.Library,
    localLibraryList: ListElement<*>
): Element {
    val allPaths = settingsViewState.currentSettings.localLibraryPaths
    val filterTabs = if (allPaths.size > 1) {
        val tabNames = listOf("All") + allPaths.map { File(it).name }
        val tabs = tabNames.mapIndexed { index, name ->
            val active = index == actualState.localFilterIndex
            val label = "  $name  "
            if (active) text(label).fg(PRIMARY_COLOR).bold()
            else text(label).fg(TEXT_DIM)
        }
        row(*tabs.toTypedArray())
    } else null

    val filtered = actualState.localTracks.filter { track ->
        val matchesTab = if (actualState.localFilterIndex == 0) true else {
            val selectedPath = allPaths.getOrNull(actualState.localFilterIndex - 1)
            selectedPath != null && track.id.startsWith("local:$selectedPath")
        }

        val matchesSearch = if (actualState.searchQuery.isEmpty()) true else {
            val q = actualState.searchQuery.lowercase()
            track.title.lowercase().contains(q) || track.artist.lowercase().contains(q)
        }
        matchesTab && matchesSearch
    }

    if (actualState.isLoading) {
        return column(
            filterTabs ?: text(""),
            text("").length(1),
            spacer(),
            text("Scanning local files...").dim().centered(),
            spacer()
        )
    }

    if (actualState.localTracks.isEmpty()) {
        val hasPaths = allPaths.isNotEmpty()
        return column(
            filterTabs ?: text(""),
            text("").length(1),
            spacer(),
            text(if (hasPaths) "  No audio files found in configured folders" else "  No local folders configured").fg(TEXT_SECONDARY).centered(),
            text("  Configure folders in Settings -> Storage").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }

    val items = filtered.mapIndexed { index, track ->
        val duration = formatDuration(track.durationMs)
        val indicator = if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
        val isPlayable = state.isPlayable(track)
        row(
            text(indicator).fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(track.title).fg(if (isPlayable) TEXT_PRIMARY else TEXT_DIM).ellipsisMiddle().fill(),
            text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            text(duration).fg(TEXT_DIM).length(6),
        )
    }
    localLibraryList.elements(*items.toTypedArray())

    val queryText = if (actualState.searchQuery.isNotEmpty()) " (Search: ${actualState.searchQuery})" else ""
    val header = row(
        text("").length(2),
        text("#").dim().length(3),
        text("Title$queryText").dim().fill(),
        text("Artist").dim().percent(25),
        text("Time").dim().length(6),
    ).margin(Margin.horizontal(1))

    return column(
        filterTabs ?: text(""),
        if (filterTabs != null) text("").length(1) else text(""),
        header,
        text("").length(1),
        localLibraryList.fill(),
    )
}