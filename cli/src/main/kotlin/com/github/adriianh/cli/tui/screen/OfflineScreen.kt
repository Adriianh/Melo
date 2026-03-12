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
    val screen = state.screen as? ScreenState.Offline ?: return panel(text("Offline screen not active").centered()).rounded()

    val content = if (screen.downloads.isEmpty()) {
        column(
            spacer(),
            text("No downloaded tracks found").fg(TEXT_SECONDARY).centered(),
            text("Download tracks from the options menu to listen offline").fg(TEXT_DIM).centered(),
            spacer()
        )
    } else {
        val items = screen.downloads.map { offlineTrack ->
            val track = offlineTrack.track
            val isPlaying = track.id == state.player.nowPlaying?.id
            row(
                text(if (isPlaying) "$ICON_NOTE " else "  ").fg(PRIMARY_COLOR).length(2),
                text(track.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
                text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
                text(formatDuration(track.durationMs)).fg(TEXT_DIM).length(6)
            )
        }
        offlineList.elements(*items.toTypedArray())
        offlineList.selected(screen.selectedIndex)
        offlineList.fill()
    }

    val helpText = "[↑/↓] navigate  [Enter] play  [m/o] options  [d] delete"

    return panel(content)
        .title("$ICON_OFFLINE Downloaded Tracks")
        .bottomTitle(helpText)
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("offline-panel")
        .onKeyEvent(onKeyEvent)
        .fill()
}
