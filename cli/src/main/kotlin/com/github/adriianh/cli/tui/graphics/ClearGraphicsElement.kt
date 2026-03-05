package com.github.adriianh.cli.tui.graphics

import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Rect
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.Size
import dev.tamboui.toolkit.element.StyledElement

/**
 * A toolkit element that clears terminal graphics.
 */
class ClearGraphicsElement : StyledElement<ClearGraphicsElement>() {
    private val widget = ClearGraphicsWidget()

    override fun renderContent(frame: Frame, area: Rect, context: RenderContext) {
        frame.renderWidget(widget, area)
    }

    override fun constraint(): Constraint = Constraint.length(1)

    override fun preferredSize(availableWidth: Int, availableHeight: Int, context: RenderContext): Size =
        Size.of(1, 1)
}