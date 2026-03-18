package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.*
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_OFFLINE
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.DownloadType
import dev.tamboui.toolkit.Toolkit.*
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

    val filteredDownloads = screen.downloads.filter { offlineTrack ->
        val matchesType = when (screen.filterType) {
            OfflineFilterType.ALL -> true
            OfflineFilterType.MANUAL -> offlineTrack.downloadType == DownloadType.MANUAL
            OfflineFilterType.CACHE -> offlineTrack.downloadType == DownloadType.PREFETCH
        }
        val matchesQuery = if (screen.searchQuery.isEmpty()) true else {
            val q = screen.searchQuery.lowercase()
            offlineTrack.track.title.lowercase().contains(q) ||
                    offlineTrack.track.artist.lowercase().contains(q)
        }
        matchesType && matchesQuery
    }

    val content = if (filteredDownloads.isEmpty()) {
        column(
            row(filterTabs),
            if (screen.searchQuery.isNotEmpty())
                text("  Filter: ${screen.searchQuery}").fg(PRIMARY_COLOR).bold()
            else text(""),
            spacer(),
            text("No tracks match the filter").fg(TEXT_SECONDARY).centered(),
            spacer()
        )
    } else {
        val items = filteredDownloads
            .map { offlineTrack ->
                val track = offlineTrack.track
                val isPlaying = track.id == state.player.nowPlaying?.id
                val typeLabel = if (offlineTrack.downloadType == DownloadType.MANUAL) " [M]" else " [C]"
                row(
                    text("$typeLabel ").fg(TEXT_DIM).length(4),
                    text(if (isPlaying) "$ICON_NOTE " else "  ").fg(PRIMARY_COLOR).length(2),
                    text(track.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
                    text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
                    text(formatDuration(track.durationMs)).fg(TEXT_DIM).length(6)
                )
            }
        offlineList.elements(*items.toTypedArray())
        offlineList.selected(screen.selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))

        column(
            row(filterTabs),
            if (screen.searchQuery.isNotEmpty())
                text("  Filter: ${screen.searchQuery} ").fg(PRIMARY_COLOR).bold()
            else text(""),
            text(""),
            offlineList.fill()
        )
    }

    val helpText = if (screen.isTyping) {
        "[Enter] finish search  [Esc] clear/cancel  [Backspace] delete"
    } else {
        "[Tab/f] filter type  [/] search  [Esc] clear search  [Enter] play  [m/o] options  [d] delete"
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
