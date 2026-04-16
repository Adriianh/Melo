package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Rect
import dev.tamboui.style.Color
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.Size
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

class CommandBarOverlay(private val stateProvider: () -> MeloState) : Element {
    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val state = stateProvider()
        if (!state.commandBar.isVisible) return

        val overlayH = 1
        val overlayW = area.width()
        val overlayX = area.x()
        val overlayY = area.y() + area.height() - overlayH
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)
        frame.buffer().clear(overlayArea)
        val content = if (state.commandBar.errorMessage != null) {
            row(
                text(" ").bg(Color.RED).fg(Color.WHITE),
                text(state.commandBar.errorMessage).bg(Color.RED).fg(Color.WHITE),
                text(" ").bg(Color.RED).fg(Color.WHITE).fill()
            )
        } else {
            row(
                text(":").fg(PRIMARY_COLOR),
                text(state.commandBar.input).fg(TEXT_PRIMARY),
                text("▌").fg(PRIMARY_COLOR)
            )
        }

        content.render(frame, overlayArea, context)
    }

    override fun preferredSize(
        availableWidth: Int,
        availableHeight: Int,
        context: RenderContext
    ): Size =
        Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()
    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult =
        EventResult.UNHANDLED
}
