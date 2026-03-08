package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.ICON_QUEUE
import com.github.adriianh.cli.tui.MeloTheme.ICON_RADIO
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.graphics.ClearGraphicsWidget
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Rect
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.Size
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

/**
 * Floating overlay for the playback queue — renders at a fixed centered
 * Rect on top of whatever is below it, exactly like PlaylistInputOverlay does.
 */
class QueueOverlay(
    private val stateProvider: () -> MeloState,
    private val queueList: ListElement<*>,
    private val onKeyEvent: (KeyEvent) -> EventResult = { EventResult.UNHANDLED },
) : Element {

    private val clearGraphics = ClearGraphicsWidget()

    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val state = stateProvider()

        val overlayW = (area.width() * 0.6).toInt().coerceAtLeast(60)
        val overlayH = (state.queue.size + 6).coerceIn(10, 20)
        val overlayX = area.x() + (area.width() - overlayW) / 2
        val overlayY = area.y() + (area.height() - overlayH) / 2
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)

        frame.renderWidget(clearGraphics, overlayArea)
        frame.buffer().clear(overlayArea)

        val content = if (state.queue.isEmpty()) {
            column(
                spacer(),
                text("Queue is empty").fg(TEXT_DIM).centered(),
                text("Press Q on any track to add it").fg(TEXT_DIM).centered(),
                spacer(),
            )
        } else {
            val items = state.queue.mapIndexed { index, track ->
                val isPlaying = index == state.queueIndex
                val indicator = if (isPlaying) "$ICON_NOTE " else "${index + 1}. "
                val titleColor = if (isPlaying) PRIMARY_COLOR else TEXT_PRIMARY
                row(
                    text(indicator).fg(PRIMARY_COLOR).length(4),
                    text(track.title).fg(titleColor).apply { if (isPlaying) bold() }.ellipsisMiddle().fill(),
                    text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
                    text(formatDuration(track.durationMs)).fg(TEXT_DIM).length(6),
                )
            }
            queueList.elements(*items.toTypedArray())
            queueList.selected(state.queueCursor)
            queueList.fill()
        }

        val remaining = state.queue.size - (state.queueIndex + 1).coerceAtLeast(0)
        val radioLabel = if (state.isRadioMode) "  $ICON_RADIO Radio" else ""
        val title = if (state.queue.isEmpty())
            "$ICON_QUEUE Queue  [Q] add  [Del] remove  [C] clear"
        else
            "$ICON_QUEUE Queue$radioLabel  ${state.queue.size} tracks  ($remaining remaining)  [Q] add  [Del] remove  [C] clear"

        panel(content)
            .title(title)
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("queue-panel")
            .onKeyEvent(onKeyEvent)
            .render(frame, overlayArea, context)
    }

    override fun preferredSize(availableWidth: Int, availableHeight: Int, context: RenderContext): Size =
        Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()

    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult =
        EventResult.UNHANDLED
}