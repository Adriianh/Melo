package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.LibrarySourceFilter
import com.github.adriianh.cli.tui.LibraryTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_BLUE
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_LIBRARY
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.allLibraryFavorites
import com.github.adriianh.cli.tui.allLibraryPlaylists
import com.github.adriianh.cli.tui.component.SettingsViewState
import com.github.adriianh.cli.tui.filteredAndSortedFavorites
import com.github.adriianh.cli.tui.filteredAndSortedPlaylists
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.filterAndSortTracks
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
import java.io.File

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

    val content: StyledElement<*> = when (actualState.libraryTab) {
        LibraryTab.FAVORITES -> buildFavoritesContent(state, actualState, favoritesList)
        LibraryTab.PLAYLISTS -> if (actualState.isInPlaylistDetail)
            buildPlaylistDetailContent(state, actualState, playlistTracksList)
        else
            buildPlaylistsContent(state, actualState, playlistsList)

        LibraryTab.LOCAL -> buildLocalContent(state, settingsViewState, actualState, localLibraryList)
    }

    val hints = if (actualState.isTyping) {
        "[Enter] Finish  [Esc] Clear"
    } else {
        when (actualState.libraryTab) {
            LibraryTab.FAVORITES -> "[Enter] Play  [m] Options  [f] Remove  [1..3] Tabs"
            LibraryTab.PLAYLISTS -> if (actualState.isInPlaylistDetail)
                "[Enter] Play  [m] Options  [d] Remove  [Esc] Back"
            else
                "[Enter] Open  [n] New  [r] Rename  [d] Delete  [1..3] Tabs"

            LibraryTab.LOCAL -> "[Enter] Play  [m] Options  [Tab] Folder  [r] Rescan  [1..3] Tabs"
        }
    }

    val footer = row(
        text(" $hints").fg(TEXT_DIM)
    )

    val body = column(
        tabBar,
        text("").length(1),
        content.fill(),
        text("").length(1),
        footer,
    )

    return panel(body)
        .title(" $ICON_LIBRARY Your Library ")
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("library-panel")
        .onKeyEvent(onKeyEvent)
        .fill()
}

private fun tabLabel(label: String, active: Boolean): Element =
    text(label).fg(if (active) PRIMARY_COLOR else TEXT_DIM).apply { if (active) bold() }

private fun buildFavoritesContent(
    state: MeloState,
    actualState: ScreenState.Library,
    favoritesList: ListElement<*>
): StyledElement<*> {
    val allFavorites = state.allLibraryFavorites()
    if (allFavorites.isEmpty()) {
        return column(
            spacer(),
            text("  No favorites yet").fg(TEXT_SECONDARY).centered(),
            text("  Press F on any track to add it here").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }

    val filtered = state.filteredAndSortedFavorites(
        sourceFilter = actualState.favoritesSourceFilter,
        sortOrder = actualState.favoritesSortOrder,
        sortDirection = actualState.favoritesSortDirection,
        query = actualState.favoritesSearchQuery
    )

    val isTyping =
        actualState.isTyping && actualState.libraryTab == LibraryTab.FAVORITES && !actualState.isInPlaylistDetail
    val searchBadge = if (isTyping) {
        text(" [Search: ${actualState.favoritesSearchQuery}●] ").fg(PRIMARY_COLOR).bold()
    } else if (actualState.favoritesSearchQuery.isNotBlank()) {
        text(" [Search: \"${actualState.favoritesSearchQuery}\"] ").fg(PRIMARY_COLOR)
    } else {
        text(" [Ctrl+F Search] ").fg(TEXT_DIM)
    }

    val sourceBadge = when (actualState.favoritesSourceFilter) {
        LibrarySourceFilter.ALL -> text(" [All (s)] ").fg(TEXT_PRIMARY).bold()
        LibrarySourceFilter.LOCAL -> text(" [⌂ Local (s)] ").fg(PRIMARY_COLOR).bold()
        LibrarySourceFilter.REMOTE -> text(" [☁ Cloud (s)] ").fg(ACCENT_BLUE).bold()
    }

    val sortBadge = text(" [Sort: ${actualState.favoritesSortOrder.label} (o)] ").fg(TEXT_SECONDARY)
    val dirBadge =
        text(" [${actualState.favoritesSortDirection.symbol} ${actualState.favoritesSortDirection.label} (O)] ").fg(
            TEXT_SECONDARY
        )

    val toolbar = row(
        searchBadge,
        text("  "),
        sourceBadge,
        text("  "),
        sortBadge,
        text("  "),
        dirBadge,
        spacer(),
        text("${filtered.size} tracks").dim()
    ).margin(Margin.horizontal(1))

    if (filtered.isEmpty()) {
        favoritesList.elements()
        return column(
            toolbar,
            text("").length(1),
            spacer(),
            text("  No favorites matching filter").fg(TEXT_SECONDARY).centered(),
            text("  Press Esc to reset search/filters").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }

    val items = filtered.mapIndexed { index, item ->
        val track = item.track
        val indicator = if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
        val isPlayable = state.isPlayable(track)
        val providerColor = if (item.isRemote) ACCENT_BLUE else PRIMARY_COLOR
        row(
            text("${item.icon} ").fg(providerColor).length(2),
            text(indicator).fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(track.title).fg(if (isPlayable) TEXT_PRIMARY else TEXT_DIM).ellipsisMiddle().fill(),
            text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            text(track.album.ifBlank { "—" }).fg(TEXT_DIM).ellipsis().percent(25),
            text(if (track.durationMs > 0L) formatDuration(track.durationMs) else "").fg(TEXT_DIM)
                .length(6),
        )
    }
    favoritesList.elements(*items.toTypedArray())

    val header = row(
        text("").length(2),
        text("").length(2),
        text("#").dim().length(3),
        text("Title").dim().fill(),
        text("Artist").dim().percent(25),
        text("Album").dim().percent(25),
        text("Time").dim().length(6),
    ).margin(Margin.horizontal(1))

    return column(
        toolbar,
        text("").length(1),
        header,
        text("").length(1),
        favoritesList.fill(),
    )
}

private fun buildPlaylistsContent(
    state: MeloState,
    actualState: ScreenState.Library,
    playlistsList: ListElement<*>
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

    val isTyping =
        actualState.isTyping && actualState.libraryTab == LibraryTab.PLAYLISTS && !actualState.isInPlaylistDetail
    val searchBadge = if (isTyping) {
        text(" [Search: ${actualState.playlistsSearchQuery}●] ").fg(PRIMARY_COLOR).bold()
    } else if (actualState.playlistsSearchQuery.isNotBlank()) {
        text(" [Search: \"${actualState.playlistsSearchQuery}\"] ").fg(PRIMARY_COLOR)
    } else {
        text(" [Ctrl+F Search] ").fg(TEXT_DIM)
    }

    val sourceBadge = when (actualState.playlistsSourceFilter) {
        LibrarySourceFilter.ALL -> text(" [All (s)] ").fg(TEXT_PRIMARY).bold()
        LibrarySourceFilter.LOCAL -> text(" [≡ Local (s)] ").fg(PRIMARY_COLOR).bold()
        LibrarySourceFilter.REMOTE -> text(" [☁ Cloud (s)] ").fg(ACCENT_BLUE).bold()
    }

    val sortBadge = text(" [Sort: ${actualState.playlistsSortOrder.label} (o)] ").fg(TEXT_SECONDARY)
    val dirBadge =
        text(" [${actualState.playlistsSortDirection.symbol} ${actualState.playlistsSortDirection.label} (O)] ").fg(
            TEXT_SECONDARY
        )

    val toolbar = row(
        searchBadge,
        text("  "),
        sourceBadge,
        text("  "),
        sortBadge,
        text("  "),
        dirBadge,
        spacer(),
        text("${filtered.size} playlists").dim()
    ).margin(Margin.horizontal(1))

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

private fun buildPlaylistDetailContent(
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
        row(
            text(indicator).fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(track.title).fg(if (isPlayable) TEXT_PRIMARY else TEXT_DIM).ellipsisMiddle().fill(),
            text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            text(if (track.durationMs > 0L) formatDuration(track.durationMs) else "").fg(TEXT_DIM)
                .length(6),
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
        toolbar,
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
): StyledElement<*> {
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

    val tabFiltered = actualState.localTracks.filter { track ->
        if (actualState.localFilterIndex == 0) true else {
            val selectedPath = allPaths.getOrNull(actualState.localFilterIndex - 1)
            if (selectedPath != null) {
                val clean = selectedPath.trimEnd('/')
                track.id.startsWith("local:$clean/") || track.id == "local:$clean"
            } else false
        }
    }

    val filtered = filterAndSortTracks(
        tracks = tabFiltered,
        sortOrder = actualState.localSortOrder,
        sortDirection = actualState.localSortDirection,
        query = actualState.localSearchQuery
    )

    val isTyping = actualState.isTyping && actualState.libraryTab == LibraryTab.LOCAL
    val searchBadge = if (isTyping) {
        text(" [Search: ${actualState.localSearchQuery}●] ").fg(PRIMARY_COLOR).bold()
    } else if (actualState.localSearchQuery.isNotBlank()) {
        text(" [Search: \"${actualState.localSearchQuery}\"] ").fg(PRIMARY_COLOR)
    } else {
        text(" [Ctrl+F Search] ").fg(TEXT_DIM)
    }

    val sortBadge = text(" [Sort: ${actualState.localSortOrder.label} (o)] ").fg(TEXT_SECONDARY)
    val dirBadge =
        text(" [${actualState.localSortDirection.symbol} ${actualState.localSortDirection.label} (O)] ").fg(
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

    if (actualState.isLoading) {
        return column(
            filterTabs ?: text(""),
            if (filterTabs != null) text("").length(1) else text(""),
            toolbar,
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
            if (filterTabs != null) text("").length(1) else text(""),
            toolbar,
            text("").length(1),
            spacer(),
            text(if (hasPaths) "  No audio files found in configured folders" else "  No local folders configured").fg(TEXT_SECONDARY).centered(),
            text("  Configure folders in Settings -> Storage").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }

    if (filtered.isEmpty()) {
        localLibraryList.elements()
        return column(
            filterTabs ?: text(""),
            if (filterTabs != null) text("").length(1) else text(""),
            toolbar,
            text("").length(1),
            spacer(),
            text("  No local tracks matching filter").fg(TEXT_SECONDARY).centered(),
            text("  Press Esc to reset search/filters").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }

    val items = filtered.mapIndexed { index, track ->
        val duration = if (track.durationMs > 0L) formatDuration(track.durationMs) else ""
        val indicator = if (track.id == state.player.nowPlaying?.id) "$ICON_NOTE " else "  "
        val isPlayable = state.isPlayable(track)
        row(
            text(indicator).fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(track.title).fg(if (isPlayable) TEXT_PRIMARY else TEXT_DIM).ellipsisMiddle().fill(),
            text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            text(track.album.ifBlank { "—" }).fg(TEXT_DIM).ellipsis().percent(25),
            text(duration).fg(TEXT_DIM).length(6),
        )
    }
    localLibraryList.elements(*items.toTypedArray())

    val header = row(
        text("").length(2),
        text("#").dim().length(3),
        text("Title").dim().fill(),
        text("Artist").dim().percent(25),
        text("Album").dim().percent(25),
        text("Time").dim().length(6),
    ).margin(Margin.horizontal(1))

    return column(
        filterTabs ?: text(""),
        if (filterTabs != null) text("").length(1) else text(""),
        toolbar,
        text("").length(1),
        header,
        text("").length(1),
        localLibraryList.fill(),
    )
}