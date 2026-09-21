package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.LibraryTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.component.SettingsViewState
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
import java.io.File

internal fun buildLocalContent(
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
        rowElements.add(text(track.album.ifBlank { "—" }).fg(TEXT_DIM).ellipsis().percent(25))
        rowElements.add(text(duration).fg(TEXT_DIM).length(6))
        row(*rowElements.toTypedArray())
    }
    localLibraryList.elements(*items.toTypedArray())

    val headerElements = mutableListOf<Element>()
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
        filterTabs ?: text(""),
        if (filterTabs != null) text("").length(1) else text(""),
        toolbar,
        text("").length(1),
        header,
        text("").length(1),
        localLibraryList.fill(),
    )
}