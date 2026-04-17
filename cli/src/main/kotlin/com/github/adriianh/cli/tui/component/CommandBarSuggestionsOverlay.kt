package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.graphics.ClearGraphicsWidget
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Rect
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.Size
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

class CommandBarSuggestionsOverlay(
    private val stateProvider: () -> MeloState
) : Element {
    private val clearGraphics = ClearGraphicsWidget()
    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val state = stateProvider()
        val commandBarState = state.commandBar
        if (!commandBarState.isVisible || commandBarState.suggestions.isEmpty()) {
            return
        }
        val items = commandBarState.suggestions.mapIndexed { index, suggestion ->
            val isSelected = index == commandBarState.selectedSuggestionIndex
            row(
                text(if (isSelected) " > " else "   ").fg(MeloTheme.TEXT_PRIMARY),
                text(suggestion).fg(if (isSelected) MeloTheme.TEXT_PRIMARY else MeloTheme.TEXT_SECONDARY)
                    .fill()
            ).length(1)
        }
        val overlayW = area.width()
        val overlayH = items.size + 2
        val overlayX = area.x()
        // Determine Y based on height of suggestions, put it just above the command bar
        val overlayY = area.y() + area.height() - (overlayH + 1)
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)
        frame.renderWidget(clearGraphics, overlayArea)
        frame.buffer().clear(overlayArea)
        val hint = "[Tab] Complete - [Enter] Execute - [Esc] Cancel"
        val subtitle = "Commands"
        panel(
            column(*items.toTypedArray()),
            spacer(),
            text(hint).fg(MeloTheme.TEXT_DIM).centered()
        )
            .title(subtitle)
            .rounded()
            .borderColor(MeloTheme.BORDER_FOCUSED)
            .render(frame, overlayArea, context)
    }

    override fun preferredSize(
        availableWidth: Int, availableHeight: Int, context: RenderContext
    ): Size = Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()
    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult {
        return EventResult.UNHANDLED
    }
}
