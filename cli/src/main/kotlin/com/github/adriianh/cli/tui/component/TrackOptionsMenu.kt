package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
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
 * Floating context menu for track-specific actions.
 */
class TrackOptionsOverlay(
    private val stateProvider: () -> MeloState,
    private val onKeyEvent: (KeyEvent) -> EventResult
) : Element {

    private fun getOptions(state: MeloState): List<String> {
        val track = state.trackOptions.track
        val offlineTrack = state.collections.offlineTracks.find { it.track.id == track?.id }
        val downloadLabel = when (offlineTrack?.downloadStatus) {
            DownloadStatus.COMPLETED if offlineTrack.downloadType == DownloadType.MANUAL -> "Remove from Offline"
            DownloadStatus.DOWNLOADING, DownloadStatus.PENDING -> "Downloading..."
            else -> "Download for Offline"
        }
        return listOf(
            "Play Now",
            "Start Radio",
            "Add to Queue",
            "Toggle Favorite",
            "Add to Playlist",
            downloadLabel,
            "View Similar Tracks"
        )
    }

    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val state = stateProvider()
        val track = state.trackOptions.track ?: return
        val options = getOptions(state)

        val overlayW = (area.width() * 0.4).toInt().coerceAtLeast(40)
        val overlayH = options.size + 6
        val overlayX = area.x() + (area.width() - overlayW) / 2
        val overlayY = area.y() + (area.height() - overlayH) / 2
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)

        frame.buffer().clear(overlayArea)

        val hint = "[↑↓] navigate   [Enter] select   [Esc] close"
        val subtitle = "${track.title} — ${track.artist}"

        val items = options.mapIndexed { index, option ->
            val isSelected = index == state.trackOptions.selectedIndex
            row(
                text(if (isSelected) " ${MeloTheme.ICON_ARROW} " else "   ").fg(PRIMARY_COLOR).length(3),
                text(option).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY).fill(),
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
            .title("Track Options")
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("track-options-panel")
            .onKeyEvent(onKeyEvent)
            .render(frame, overlayArea, context)
    }

    override fun preferredSize(availableWidth: Int, availableHeight: Int, context: RenderContext): Size =
        Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()

    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult {
        if (!stateProvider().trackOptions.isVisible) return EventResult.UNHANDLED
        return onKeyEvent(event)
    }
}