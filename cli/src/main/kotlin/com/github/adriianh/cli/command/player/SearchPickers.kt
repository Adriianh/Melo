package com.github.adriianh.cli.command.player

import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.white
import com.varabyte.kotter.runtime.render.RenderScope

object SearchPickers {
    fun <T> pickItem(
        items: List<T>,
        title: String,
        maxVisible: Int = 15,
        renderItem: RenderScope.(index: Int, item: T, isSelected: Boolean) -> Unit
    ): T? {
        if (items.isEmpty()) return null
        var selectedItem: T? = null
        session {
            var selectedIndex by liveVarOf(0)
            var accepted by liveVarOf(false)
            section {
                cyan { textLine(title) }
                textLine()
                val windowStart =
                    maxOf(0, minOf(selectedIndex - maxVisible / 2, items.size - maxVisible))
                val visibleItems = items.drop(windowStart).take(maxVisible)
                if (windowStart > 0) {
                    white { textLine("  ↑ ... ") }
                }
                visibleItems.forEachIndexed { index, item ->
                    val actualIndex = windowStart + index
                    val isSelected = actualIndex == selectedIndex
                    renderItem(actualIndex, item, isSelected)
                }
                if (windowStart + maxVisible < items.size) {
                    white { textLine("  ↓ ... ") }
                }
                textLine()
                textLine("Use UP/DOWN to scroll & pick, ENTER to select, ESC to exit")
            }.runUntilSignal {
                onKeyPressed {
                    when (key) {
                        Keys.UP -> selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        Keys.DOWN -> selectedIndex =
                            (selectedIndex + 1).coerceAtMost(items.size - 1)

                        Keys.ENTER -> {
                            accepted = true
                            signal()
                        }

                        Keys.ESC -> signal()
                    }
                }
            }
            if (accepted) {
                selectedItem = items[selectedIndex]
            }
        }
        return selectedItem
    }
}
