package com.github.adriianh.cli.tui.graphics

import dev.tamboui.buffer.Buffer
import dev.tamboui.layout.Rect
import dev.tamboui.widget.RawOutputCapable
import dev.tamboui.widget.Widget
import java.io.OutputStream
import java.nio.charset.StandardCharsets

/**
 * A widget that sends escape sequences to clear terminal graphics overlays
 * (Kitty, iTerm2). This is useful to prevent "ghosting" when an image
 * widget is removed from the render tree.
 */
class ClearGraphicsWidget : Widget, RawOutputCapable {
    companion object {
        private const val KITTY_CLEAR = "\u001b_Ga=d,d=A;\u001b\\"
        private const val ITERM_CLEAR = "\u001b]1337;ClearScrollback\u0007"
    }

    override fun render(area: Rect, buffer: Buffer) {
        if (!area.isEmpty()) {
            buffer.set(area.x(), area.y(), dev.tamboui.buffer.Cell.EMPTY)
        }
    }

    override fun render(area: Rect, buffer: Buffer, rawOutput: OutputStream?) {
        rawOutput?.let {
            it.write(KITTY_CLEAR.toByteArray(StandardCharsets.US_ASCII))
            it.flush()
        }
    }
}
