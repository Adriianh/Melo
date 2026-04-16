package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.SECONDARY_COLOR
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.graphics.ClearGraphicsWidget
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Rect
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.Size
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

class SearchSuggestionsOverlay(
    private val stateProvider: () -> MeloState
) : Element {
    private val clearGraphics = ClearGraphicsWidget()

    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val state = stateProvider()
        val searchState = state.screen as? ScreenState.Search
        if (searchState == null || !searchState.isShowingSuggestions || searchState.searchSuggestions.isEmpty()) {
            return
        }

        val overlayW = area.width()
        val overlayH = searchState.searchSuggestions.size + 2
        val overlayX = area.x()
        val overlayY = area.y() + 3
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)

        frame.renderWidget(clearGraphics, overlayArea)
        frame.buffer().clear(overlayArea)

        val items = searchState.searchSuggestions.mapIndexed { index, suggestion ->
            val isSelected = index == searchState.selectedSuggestionIndex
            row(
                text(if (isSelected) " > " else "   ").fg(PRIMARY_COLOR),
                text(suggestion).fg(if (isSelected) PRIMARY_COLOR else SECONDARY_COLOR).fill()
            ).length(1)
        }

        panel(column(*items.toTypedArray()))
            .rounded()
            .borderColor(BORDER_FOCUSED)
            .render(frame, overlayArea, context)
    }

    override fun preferredSize(availableWidth: Int, availableHeight: Int, context: RenderContext): Size =
        Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()

    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult {
        return EventResult.UNHANDLED
    }
}