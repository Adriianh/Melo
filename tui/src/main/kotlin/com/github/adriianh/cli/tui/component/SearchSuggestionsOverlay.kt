package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.ScreenState
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

        val hint = "[d] Delete - [Enter] Select - [Esc] Cancel"
        val subtitle = "Search suggestions"

        val items = searchState.searchSuggestions.mapIndexed { index, suggestion ->
            val isSelected = index == searchState.selectedSuggestionIndex
            row(
                text(if (isSelected) " > " else "   ").fg(MeloTheme.TEXT_PRIMARY),
                text(suggestion).fg(if (isSelected) MeloTheme.TEXT_PRIMARY else MeloTheme.TEXT_SECONDARY)
                    .fill()
            ).length(1)
        }

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