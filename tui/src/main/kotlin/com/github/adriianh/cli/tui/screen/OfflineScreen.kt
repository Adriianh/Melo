package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.ICON_OFFLINE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.OfflineFilterType
import com.github.adriianh.core.domain.model.filterAndSortOfflineTracks
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun renderOfflineScreen(
    state: MeloState,
    offlineList: ListElement<*>,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val screen = state.screen as? ScreenState.Offline
        ?: return panel(text("Offline screen not active").centered()).rounded()

    val filterTabs = buildOfflineFilterTabs(screen.filterType)

    val filteredDownloads = filterAndSortOfflineTracks(
        downloads = screen.downloads,
        filterType = screen.filterType,
        sortOrder = screen.sortOrder,
        sortDirection = screen.sortDirection,
        query = screen.searchQuery
    )

    val searchBadge = if (screen.isTyping) {
        text(" [Search: ${screen.searchQuery}●] ").fg(PRIMARY_COLOR).bold()
    } else if (screen.searchQuery.isNotBlank()) {
        text(" [Search: \"${screen.searchQuery}\"] ").fg(PRIMARY_COLOR)
    } else {
        text(" [Ctrl+F Search] ").fg(TEXT_DIM)
    }

    val sortBadge = text(" [Sort: ${screen.sortOrder.label} (o)] ").fg(TEXT_SECONDARY)
    val dirBadge =
        text(" [${screen.sortDirection.symbol} ${screen.sortDirection.label} (O)] ").fg(
            TEXT_SECONDARY
        )

    val toolbar = row(
        filterTabs,
        text("  "),
        searchBadge,
        text("  "),
        sortBadge,
        text("  "),
        dirBadge,
        spacer(),
        text("${filteredDownloads.size} tracks").dim()
    ).margin(Margin.horizontal(1))

    val content = if (filteredDownloads.isEmpty()) {
        offlineList.elements()
        column(
            toolbar,
            text("").length(1),
            spacer(),
            text("No tracks match the filter").fg(TEXT_SECONDARY).centered(),
            text("Press Esc to reset search/filters").fg(TEXT_DIM).centered(),
            spacer()
        )
    } else {
        val items = filteredDownloads.mapIndexed { index, offlineTrack ->
            val track = offlineTrack.track
            val isPlaying = track.id == state.player.nowPlaying?.id
            val typeLabel = if (offlineTrack.downloadType == DownloadType.MANUAL) " [M]" else " [C]"
            row(
                text("$typeLabel ").fg(TEXT_DIM).length(4),
                text(if (isPlaying) "$ICON_NOTE " else "  ").fg(PRIMARY_COLOR).length(2),
                text("${index + 1}").dim().length(3),
                text(track.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
                text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
                text(if (track.durationMs > 0L) formatDuration(track.durationMs) else "").fg(
                    TEXT_DIM
                ).length(6)
            )
        }
        offlineList.elements(*items.toTypedArray())
        offlineList.selected(screen.selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))

        val header = row(
            text("").length(4),
            text("").length(2),
            text("#").dim().length(3),
            text("Title").dim().fill(),
            text("Artist").dim().percent(25),
            text("Time").dim().length(6)
        ).margin(Margin.horizontal(1))

        column(
            toolbar,
            text("").length(1),
            header,
            text("").length(1),
            offlineList.fill()
        )
    }

    val helpText = if (screen.isTyping) {
        "[Enter] finish search  [Esc] clear/cancel  [Backspace] delete"
    } else {
        "[Enter] play  [Space] pause  [m] options  [d] delete  [Tab/s] filter  [o] sort  [O] direction  [Ctrl+F] search"
    }
    val countTitle = if (filteredDownloads.size != screen.downloads.size)
        "(${filteredDownloads.size}/${screen.downloads.size})"
    else "(${screen.downloads.size})"

    return panel(content)
        .title("$ICON_OFFLINE Offline Tracks $countTitle")
        .bottomTitle(helpText)
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("offline-panel")
        .onKeyEvent(onKeyEvent)
        .fill()
}

private fun buildOfflineFilterTabs(current: OfflineFilterType): Element {
    val tabs = OfflineFilterType.entries.map { type ->
        val selected = type == current
        val label = "  ${type.label}  "
        if (selected) text(label).fg(PRIMARY_COLOR).bold()
        else text(label).fg(TEXT_DIM)
    }
    return row(*tabs.toTypedArray())
}
