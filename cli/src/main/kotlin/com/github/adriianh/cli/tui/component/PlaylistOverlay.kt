package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_LIBRARY
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.PlaylistInputMode
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Rect
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.Size
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

/**
 * Floating overlay for playlist create/rename — renders at a fixed centered
 * Rect on top of whatever is below it, exactly like FloatingPanelsArea does.
 */
class PlaylistInputOverlay(private val stateProvider: () -> MeloState) : Element {

    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val state = stateProvider()

        val overlayW = (area.width() * 0.4).toInt().coerceAtLeast(40)
        val overlayH = 7
        val overlayX = area.x() + (area.width() - overlayW) / 2
        val overlayY = area.y() + (area.height() - overlayH) / 2
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)

        frame.buffer().clear(overlayArea)

        val title = when (state.playlistInputMode) {
            PlaylistInputMode.CREATE -> "$ICON_LIBRARY New Playlist"
            PlaylistInputMode.RENAME -> "$ICON_LIBRARY Rename Playlist"
            else -> ""
        }
        val cursor = if ((System.currentTimeMillis() / 500) % 2 == 0L) "█" else " "
        val hint = "[Enter] confirm   [Esc] cancel"

        val content = column(
            spacer(),
            row(
                text("Name: ").fg(TEXT_SECONDARY).length(7),
                text(state.playlistInput + cursor).fg(TEXT_PRIMARY).fill(),
            ),
            spacer(),
            text(hint).fg(TEXT_DIM).centered(),
            spacer(),
        )

        panel(content)
            .title(title)
            .rounded()
            .borderColor(BORDER_FOCUSED)
            .focusedBorderColor(BORDER_FOCUSED)
            .render(frame, overlayArea, context)
    }

    override fun preferredSize(availableWidth: Int, availableHeight: Int, context: RenderContext): Size =
        Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()

    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult =
        EventResult.UNHANDLED
}

/**
 * Floating overlay for picking a playlist — same absolute-rect pattern.
 */
class PlaylistPickerOverlay(private val stateProvider: () -> MeloState) : Element {

    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val state = stateProvider()
        val playlists = state.playlists
        val track = state.playlistPickerTrack

        val overlayW = (area.width() * 0.5).toInt().coerceAtLeast(50)
        val overlayH = (playlists.size + 6).coerceIn(8, 20)
        val overlayX = area.x() + (area.width() - overlayW) / 2
        val overlayY = area.y() + (area.height() - overlayH) / 2
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)

        frame.buffer().clear(overlayArea)

        val hint = "[↑↓] navigate   [Enter] add   [Esc] cancel"
        val subtitle = track?.let { "${it.title} — ${it.artist}" } ?: ""

        val items = playlists.mapIndexed { index, playlist ->
            val isSelected = index == state.playlistPickerCursor
            val count = "${playlist.trackCount} track${if (playlist.trackCount != 1) "s" else ""}"
            row(
                text(if (isSelected) "▸ " else "  ").fg(PRIMARY_COLOR).length(2),
                text(playlist.name).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY).ellipsis().fill(),
                text(count).fg(TEXT_DIM).length(12),
            )
        }

        val content = column(
            text(subtitle).fg(TEXT_DIM).centered(),
            text("").length(1),
            *items.toTypedArray(),
            spacer(),
            text(hint).fg(TEXT_DIM).centered(),
        )

        panel(content)
            .title("$ICON_LIBRARY Add to Playlist")
            .rounded()
            .borderColor(BORDER_FOCUSED)
            .focusedBorderColor(BORDER_FOCUSED)
            .render(frame, overlayArea, context)
    }

    override fun preferredSize(availableWidth: Int, availableHeight: Int, context: RenderContext): Size =
        Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()

    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult =
        EventResult.UNHANDLED
}
