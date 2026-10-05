package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.handler.CommandBarHandlers
import com.github.adriianh.cli.tui.handler.CommandCategory
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.MeloKey
import com.github.adriianh.core.domain.model.Settings
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Rect
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.Size
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

sealed interface HelpRow {
    data class Header(
        val title: String,
    ) : HelpRow

    data class Item(
        val trigger: String,
        val description: String,
        val extra: String? = null,
    ) : HelpRow

    data object Separator : HelpRow
}

fun buildHelpRows(settings: Settings): List<HelpRow> {
    val rows = mutableListOf<HelpRow>()

    val grouped = CommandBarHandlers.COMMANDS.groupBy { it.category }
    for (category in CommandCategory.entries) {
        val cmds = grouped[category] ?: continue

        rows.add(HelpRow.Header(category.title))
        for (cmd in cmds) {
            val mainName = cmd.names.first()
            val syntax =
                if (cmd.argumentDescription != null) {
                    ":$mainName ${cmd.argumentDescription}"
                } else {
                    ":$mainName"
                }
            val desc = cmd.description.orEmpty()
            val aliases =
                if (cmd.names.size > 1) {
                    "alias: ${cmd.names.drop(1).joinToString { ":$it" }}"
                } else {
                    null
                }

            rows.add(HelpRow.Item(syntax, desc, aliases))
        }
        rows.add(HelpRow.Separator)
    }

    rows.add(HelpRow.Header("Global Shortcuts"))
    val globalShortcuts =
        listOf(
            ":" to "Open Command Bar",
            "Tab / Shift+Tab" to "Cycle focus across panels",
            "/" to "Focus search bar",
            "1..4" to "Switch search tabs (Songs, Albums, Artists, Playlists)",
            "Esc" to "Close dialog / clear selection / back",
            "?" to "Open this help dialog",
        )
    for ((key, desc) in globalShortcuts) {
        rows.add(HelpRow.Item(key, desc))
    }
    rows.add(HelpRow.Separator)

    rows.add(HelpRow.Header("Configured Actions"))
    for (action in MeloAction.entries) {
        val binding = settings.keybindings[action]
        val keyStr = binding?.toDisplayString() ?: "None"
        rows.add(HelpRow.Item(keyStr, action.displayName))
    }

    return rows
}

internal fun computeHelpOverlayDimensions(
    availableWidth: Int,
    availableHeight: Int,
): Pair<Int, Int> {
    val overlayW =
        (availableWidth * 0.85).toInt().coerceIn(76, 120).coerceAtMost(maxOf(40, availableWidth - 2))
    val overlayH =
        (availableHeight * 0.80).toInt().coerceIn(16, 34).coerceAtMost(maxOf(10, availableHeight - 2))
    return overlayW to overlayH
}

private fun MeloKey.toDisplayString(): String? {
    val base =
        when {
            char != null -> if (char == ' ') "Space" else char.toString()
            code != null -> code
            else -> ""
        }
    return if (ctrl) "Ctrl+$base" else base
}

private fun renderHelpRow(row: HelpRow): Element =
    when (row) {
        is HelpRow.Header ->
            row(
                text("─── ${row.title} ───")
                    .fg(PRIMARY_COLOR)
                    .bold()
                    .centered()
                    .fill(),
            )
        is HelpRow.Item -> {
            val keyElement = text("  ${row.trigger}").fg(PRIMARY_COLOR).bold().length(24)
            val descElement = text(row.description).fg(TEXT_PRIMARY).ellipsis().fill()
            if (!row.extra.isNullOrBlank()) {
                val extraElement = text("  ${row.extra}").fg(TEXT_DIM).length(26)
                row(keyElement, descElement, extraElement)
            } else {
                row(keyElement, descElement)
            }
        }
        is HelpRow.Separator -> text("")
    }

class HelpOverlay(
    private val stateProvider: () -> MeloState,
    private val settingsProvider: () -> Settings,
    private val onKeyEvent: (KeyEvent) -> EventResult,
) : Element {
    override fun render(
        frame: Frame,
        area: Rect,
        context: RenderContext,
    ) {
        val state = stateProvider()
        if (!state.helpOverlay.isVisible) return

        val allRows = buildHelpRows(settingsProvider())
        val (overlayW, overlayH) = computeHelpOverlayDimensions(area.width(), area.height())
        val overlayX = area.x() + (area.width() - overlayW) / 2
        val overlayY = area.y() + (area.height() - overlayH) / 2
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)

        frame.buffer().clear(overlayArea)

        val visibleLines = (overlayH - 4).coerceAtLeast(1)
        val maxScroll = maxOf(0, allRows.size - visibleLines)
        val currentScroll = state.helpOverlay.scrollOffset.coerceIn(0, maxScroll)
        val visibleRows = allRows.subList(currentScroll, minOf(allRows.size, currentScroll + visibleLines))

        val tableHeader =
            row(
                text("  COMMAND / SHORTCUT").fg(TEXT_DIM).bold().length(24),
                text("DESCRIPTION").fg(TEXT_DIM).bold().fill(),
                text("  ALIASES / BINDING").fg(TEXT_DIM).bold().length(26),
            )
        val tableDivider =
            row(
                text("  ──────────────────").fg(TEXT_DIM).length(24),
                text("───────────").fg(TEXT_DIM).fill(),
                text("  ─────────────────").fg(TEXT_DIM).length(26),
            )

        val renderedRows = visibleRows.map(::renderHelpRow)

        val scrollPos = "${currentScroll + 1}-${minOf(allRows.size, currentScroll + visibleLines)}/${allRows.size}"
        val hint = "[↑/↓/j/k] Scroll  [PgUp/Dn]  [Esc/q] Close  [:] Command  [$scrollPos]"

        val content =
            column(
                tableHeader,
                tableDivider,
                *renderedRows.toTypedArray(),
            )

        panel(content)
            .title(" Help & Commands ")
            .bottomTitle(" $hint ")
            .rounded()
            .borderColor(BORDER_FOCUSED)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("help-overlay-panel")
            .onKeyEvent(onKeyEvent)
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
    ): EventResult {
        if (!stateProvider().helpOverlay.isVisible) return EventResult.UNHANDLED
        return onKeyEvent(event)
    }
}
