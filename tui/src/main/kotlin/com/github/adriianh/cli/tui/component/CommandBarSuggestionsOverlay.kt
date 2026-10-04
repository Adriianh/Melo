package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.handler.CommandBarHandlers
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

private const val MIN_OVERLAY_HEIGHT = 8
private const val MIN_VISIBLE_ITEMS = 3
private const val OVERLAY_OVERHEAD = 6
private const val HINT_TEXT = "[Tab] Complete  [Enter] Run  [Esc] Close"

private data class InspectorInfo(
    val syntax: String,
    val description: String,
)

class CommandBarSuggestionsOverlay(
    private val stateProvider: () -> MeloState,
) : Element {
    override fun render(
        frame: Frame,
        area: Rect,
        context: RenderContext,
    ) {
        val state = stateProvider()
        val commandBarState = state.commandBar
        if (!commandBarState.isVisible || commandBarState.suggestions.isEmpty()) {
            return
        }

        val total = commandBarState.suggestions.size
        val selectedIndex = commandBarState.selectedSuggestionIndex ?: 0

        val maxOverlayH = maxOf(MIN_OVERLAY_HEIGHT, area.height() - 3)
        val maxVisible = maxOf(MIN_VISIBLE_ITEMS, minOf(total, maxOverlayH - OVERLAY_OVERHEAD))
        val visibleCount = minOf(total, maxVisible)
        val scrollOffset = computeScrollOffset(total, visibleCount, selectedIndex)
        val visibleSuggestions = commandBarState.suggestions.subList(scrollOffset, scrollOffset + visibleCount)

        val items = buildSuggestionRows(visibleSuggestions, scrollOffset, commandBarState.selectedSuggestionIndex)
        val activeSuggestion =
            commandBarState
                .suggestions
                .getOrNull(selectedIndex) ?: commandBarState.suggestions.first()
        val inspector = buildInspector(activeSuggestion)

        val overlayW = computeOverlayWidth(commandBarState.suggestions, inspector.syntax, area.width())
        val overlayH = minOf(visibleCount + OVERLAY_OVERHEAD, area.height() - 2)
        val overlayArea =
            Rect(
                area.x() + 2,
                maxOf(0, area.y() + area.height() - (overlayH + 1)),
                overlayW,
                overlayH,
            )
        frame.buffer().clear(overlayArea)

        val title = if (total > visibleCount) "Commands (${selectedIndex + 1}/$total)" else "Commands"
        val divider = text("─".repeat(maxOf(0, overlayW - 4))).fg(MeloTheme.BORDER_DEFAULT).length(1)

        panel(
            column(
                *items.toTypedArray(),
                spacer(),
                divider,
                text(" ${inspector.syntax}").fg(MeloTheme.PRIMARY_COLOR).bold().length(1),
                text(" ${inspector.description}").fg(MeloTheme.TEXT_SECONDARY).length(1),
                text(HINT_TEXT).fg(MeloTheme.TEXT_DIM).centered().length(1),
            ),
        ).title(title)
            .rounded()
            .borderColor(MeloTheme.BORDER_FOCUSED)
            .render(frame, overlayArea, context)
    }

    override fun preferredSize(
        availableWidth: Int,
        availableHeight: Int,
        context: RenderContext,
    ): Size = Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()

    override fun handleKeyEvent(
        event: KeyEvent,
        focused: Boolean,
    ): EventResult = EventResult.UNHANDLED
}

private fun computeScrollOffset(
    total: Int,
    visibleCount: Int,
    selectedIndex: Int,
): Int =
    when {
        total <= visibleCount -> 0
        selectedIndex < visibleCount / 2 -> 0
        selectedIndex >= total - (visibleCount / 2) -> total - visibleCount
        else -> selectedIndex - (visibleCount / 2)
    }.coerceIn(0, maxOf(0, total - visibleCount))

private fun computeOverlayWidth(
    suggestions: List<String>,
    syntax: String,
    areaWidth: Int,
): Int {
    val longest = suggestions.maxOfOrNull { it.length } ?: 16
    val targetW = maxOf(48, minOf(64, maxOf(longest + 10, syntax.length + 8)))
    return minOf(targetW, areaWidth - 4)
}

private fun buildInspector(suggestion: String): InspectorInfo {
    val cmdName = suggestion.split(" ").first()
    val command = CommandBarHandlers.findCommand(cmdName)
    val syntax = ":" + cmdName + if (command?.argumentDescription != null) " ${command.argumentDescription}" else ""
    val description = command?.description ?: ""
    return InspectorInfo(syntax, description)
}

private fun buildSuggestionRows(
    visibleSuggestions: List<String>,
    scrollOffset: Int,
    selectedIndex: Int?,
): List<Element> {
    val maxCmdLen = visibleSuggestions.maxOfOrNull { it.split(" ").first().length } ?: 6
    val cmdColWidth = maxOf(6, minOf(14, maxCmdLen + 1))

    return visibleSuggestions.mapIndexed { index, suggestion ->
        val isSelected = (scrollOffset + index) == selectedIndex
        val parts = suggestion.split(" ", limit = 2)
        val cmdPart = parts[0]
        val argPart = parts.getOrNull(1)

        val pointer = if (isSelected) " ▶ " else "   "
        val cmdText =
            text(cmdPart)
                .fg(if (isSelected) MeloTheme.PRIMARY_COLOR else MeloTheme.TEXT_PRIMARY)
                .length(cmdColWidth)

        val rightElement =
            if (argPart != null) {
                val isPlaceholder = argPart.startsWith("<") && argPart.endsWith(">")
                val color =
                    when {
                        isSelected -> MeloTheme.TEXT_PRIMARY
                        isPlaceholder -> MeloTheme.TEXT_DIM
                        else -> MeloTheme.TEXT_SECONDARY
                    }
                text(argPart).fg(color).fill()
            } else {
                spacer()
            }

        row(
            text(pointer).fg(if (isSelected) MeloTheme.PRIMARY_COLOR else MeloTheme.TEXT_PRIMARY).length(3),
            cmdText,
            rightElement,
        ).length(1)
    }
}
