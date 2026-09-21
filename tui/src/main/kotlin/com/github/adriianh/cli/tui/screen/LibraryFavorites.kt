package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.FavoritesSubTab
import com.github.adriianh.cli.tui.LibraryFavoriteEntityItem
import com.github.adriianh.cli.tui.LibraryFavoriteItem
import com.github.adriianh.cli.tui.LibrarySourceFilter
import com.github.adriianh.cli.tui.LibraryTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_BLUE
import com.github.adriianh.cli.tui.MeloTheme.ICON_BULLET
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.SECONDARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.allFavoriteAlbums
import com.github.adriianh.cli.tui.allFavoriteArtists
import com.github.adriianh.cli.tui.allFavoritePlaylists
import com.github.adriianh.cli.tui.allLibraryFavorites
import com.github.adriianh.cli.tui.filteredAndSortedFavorites
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import dev.tamboui.layout.Margin
import dev.tamboui.style.Color
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement

internal fun buildFavoritesContent(
    state: MeloState,
    actualState: ScreenState.Library,
    favoritesList: ListElement<*>,
    terminalWidth: Int = 120,
): StyledElement<*> {
    val allFavorites = state.allLibraryFavorites()
    val albums = state.allFavoriteAlbums()
    val artists = state.allFavoriteArtists()
    val playlists = state.allFavoritePlaylists()

    val subTabs = FavoritesSubTab.entries.map { subTab ->
        val active = actualState.favoritesSubTab == subTab
        val count = when (subTab) {
            FavoritesSubTab.SONGS -> allFavorites.size
            FavoritesSubTab.ALBUMS -> albums.size
            FavoritesSubTab.ARTISTS -> artists.size
            FavoritesSubTab.PLAYLISTS -> playlists.size
        }
        val label = when {
            terminalWidth < 70 -> if (active) "[ ${subTab.label} ]" else subTab.label
            terminalWidth < 95 -> if (active) "[ ${subTab.label} ($count) ]" else subTab.label
            else -> if (active) "[ ${subTab.label} ($count) ]" else "${subTab.label} ($count)"
        }
        text(label).fg(if (active) PRIMARY_COLOR else TEXT_DIM).apply { if (active) bold() }
    }
    val subTabBar = row(
        *subTabs.flatMapIndexed { index, el ->
            if (index < subTabs.size - 1) listOf(el, text("  ")) else listOf(el)
        }.toTypedArray()
    ).margin(Margin.horizontal(1))

    val subTabContent = when (actualState.favoritesSubTab) {
        FavoritesSubTab.SONGS -> buildFavoriteSongsContent(
            state,
            actualState,
            allFavorites,
            favoritesList,
            terminalWidth
        )

        FavoritesSubTab.ALBUMS -> buildFavoriteEntitiesContent(
            title = "Album",
            icon = "◎",
            iconColor = ACCENT_BLUE,
            emptyText = "No favorite albums yet",
            emptyHint = "Press Shift+F on any album in search or details to add it here",
            entities = albums,
            favoritesList = favoritesList
        )

        FavoritesSubTab.ARTISTS -> buildFavoriteEntitiesContent(
            title = "Artist",
            icon = "◇",
            iconColor = PRIMARY_COLOR,
            emptyText = "No favorite artists yet",
            emptyHint = "Press Shift+F on any artist in search or details to add it here",
            entities = artists,
            favoritesList = favoritesList
        )

        FavoritesSubTab.PLAYLISTS -> buildFavoriteEntitiesContent(
            title = "Playlist",
            icon = "≡",
            iconColor = SECONDARY_COLOR,
            emptyText = "No favorite playlists yet",
            emptyHint = "Press Shift+F on any playlist in search or details to add it here",
            entities = playlists,
            favoritesList = favoritesList
        )
    }

    return column(
        subTabBar,
        text("").length(1),
        subTabContent.fill()
    )
}

internal fun buildFavoriteEntitiesContent(
    title: String,
    icon: String,
    iconColor: Color,
    emptyText: String,
    emptyHint: String,
    entities: List<LibraryFavoriteEntityItem>,
    favoritesList: ListElement<*>
): StyledElement<*> {
    if (entities.isEmpty()) {
        favoritesList.elements()
        return column(
            spacer(),
            text("  $emptyText").fg(TEXT_SECONDARY).centered(),
            text("  $emptyHint").fg(TEXT_DIM).centered(),
            spacer(),
        )
    }

    val items = entities.mapIndexed { index, item ->
        val entity = item.entity
        val isSelected = index == favoritesList.selected()
        val indicator = if (isSelected) "$ICON_BULLET " else "  "
        val sourceColor = if (item.isRemote) ACCENT_BLUE else PRIMARY_COLOR
        val rowElements = mutableListOf<Element>()
        rowElements.add(text("${item.icon} ").fg(sourceColor).length(2))
        rowElements.add(text("$icon ").fg(iconColor).length(3))
        rowElements.add(text(indicator).fg(PRIMARY_COLOR).length(2))
        rowElements.add(text("${index + 1}").dim().length(3))
        rowElements.add(text(entity.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill())
        if (entity.subtitle != null) {
            rowElements.add(text(entity.subtitle).fg(TEXT_SECONDARY).percent(30).ellipsis())
        }
        if (entity.trackCount != null) {
            rowElements.add(text("${entity.trackCount} tracks").fg(TEXT_DIM).length(12))
        }
        row(*rowElements.toTypedArray())
    }
    favoritesList.elements(*items.toTypedArray())

    val headerElements = mutableListOf<Element>()
    headerElements.add(text("").length(2))
    headerElements.add(text("").length(3))
    headerElements.add(text("").length(2))
    headerElements.add(text("#").dim().length(3))
    headerElements.add(text("Name").dim().fill())
    if (entities.any { it.entity.subtitle != null }) {
        headerElements.add(
            text(if (title == "Artist") "Listeners / Subscribers" else "Author / Year").dim()
                .percent(30)
        )
    }
    if (entities.any { it.entity.trackCount != null }) {
        headerElements.add(text("Tracks").dim().length(12))
    }
    val header = row(*headerElements.toTypedArray()).margin(Margin.horizontal(1))

    return column(
        header,
        text("").length(1),
        favoritesList.fill(),
    )
}

internal fun buildFavoriteSongsContent(
    state: MeloState,
    actualState: ScreenState.Library,
    allFavorites: List<LibraryFavoriteItem>,
    favoritesList: ListElement<*>,
    terminalWidth: Int = 120,
): StyledElement<*> {
    if (allFavorites.isEmpty()) {
        favoritesList.elements()
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

    val isCompact = terminalWidth < 95
    val isTyping =
        actualState.isTyping && actualState.libraryTab == LibraryTab.FAVORITES && !actualState.isInPlaylistDetail
    val searchBadge = if (isTyping) {
        text(" [Search: ${actualState.favoritesSearchQuery}●] ").fg(PRIMARY_COLOR).bold()
    } else if (actualState.favoritesSearchQuery.isNotBlank()) {
        text(" [Search: \"${actualState.favoritesSearchQuery}\"] ").fg(PRIMARY_COLOR)
    } else {
        text(if (isCompact) " [Ctrl+F] " else " [Ctrl+F Search] ").fg(TEXT_DIM)
    }

    val sourceBadge = when (actualState.favoritesSourceFilter) {
        LibrarySourceFilter.ALL -> text(if (isCompact) " [All] " else " [All (s)] ").fg(TEXT_PRIMARY)
            .bold()

        LibrarySourceFilter.LOCAL -> text(if (isCompact) " [⌂] " else " [⌂ Local (s)] ").fg(
            PRIMARY_COLOR
        ).bold()

        LibrarySourceFilter.REMOTE -> text(if (isCompact) " [☁] " else " [☁ Cloud (s)] ").fg(
            ACCENT_BLUE
        ).bold()
    }

    val sortBadge =
        text(if (isCompact) " [${actualState.favoritesSortOrder.label}] " else " [Sort: ${actualState.favoritesSortOrder.label} (o)] ").fg(
            TEXT_SECONDARY
        )
    val dirBadge =
        text(if (isCompact) " [${actualState.favoritesSortDirection.symbol}] " else " [${actualState.favoritesSortDirection.symbol} ${actualState.favoritesSortDirection.label} (O)] ").fg(
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
        toolbarElements.add(text("${filtered.size} tracks").dim())
    }
    val toolbar = row(*toolbarElements.toTypedArray()).margin(Margin.horizontal(1))

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
        val isBatchSelected = state.selection.isSelected(track.id)
        val rowElements = mutableListOf<Element>()
        rowElements.add(text("${item.icon} ").fg(providerColor).length(2))
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
        rowElements.add(text(track.album.ifBlank { "—" }).fg(TEXT_DIM).ellipsis().percent(25))
        rowElements.add(
            text(if (track.durationMs > 0L) formatDuration(track.durationMs) else "").fg(
                TEXT_DIM
            ).length(6)
        )
        row(*rowElements.toTypedArray())
    }
    favoritesList.elements(*items.toTypedArray())

    val headerElements = mutableListOf<Element>()
    headerElements.add(text("").length(2))
    headerElements.add(text("").length(2))
    if (state.selection.isNotEmpty) {
        headerElements.add(text("Sel ").dim().length(4))
    }
    headerElements.add(text("#").dim().length(3))
    headerElements.add(text("Title").dim().fill())
    headerElements.add(text("Artist").dim().percent(25))
    headerElements.add(text("Album").dim().percent(25))
    headerElements.add(text("Time").dim().length(6))
    val header = row(*headerElements.toTypedArray()).margin(Margin.horizontal(1))

    return column(
        toolbar,
        text("").length(1),
        header,
        text("").length(1),
        favoritesList.fill(),
    )
}