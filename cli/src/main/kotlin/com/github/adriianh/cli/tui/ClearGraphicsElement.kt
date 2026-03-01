package com.github.adriianh.cli.tui

import dev.tamboui.layout.Constraint
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.element.Size

/**
 * A toolkit element that clears terminal graphics.
 */
class ClearGraphicsElement : StyledElement<ClearGraphicsElement>() {
    private val widget = ClearGraphicsWidget()

    override fun renderContent(frame: Frame, area: dev.tamboui.layout.Rect, context: RenderContext) {
        frame.renderWidget(widget, area)
    }

    override fun constraint(): Constraint {
        return Constraint.length(1)
    }

    override fun preferredSize(availableWidth: Int, availableHeight: Int, context: RenderContext): dev.tamboui.toolkit.element.Size {
        return Size.of(1, 1)
    }
}