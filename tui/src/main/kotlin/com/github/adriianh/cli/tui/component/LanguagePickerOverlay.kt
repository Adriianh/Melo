package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
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

/**
 * Floating modal overlay for selecting the target lyrics translation language.
 */
class LanguagePickerOverlay(
    private val stateProvider: () -> MeloState,
    private val onKeyEvent: (KeyEvent) -> EventResult
) : Element {

    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val state = stateProvider()
        if (!state.languagePicker.isVisible) return

        val languages = state.languagePicker.languages
        val currentLang = state.languagePicker.currentLanguage.ifBlank { "es" }

        val overlayW = (area.width() * 0.45).toInt().coerceIn(38, 54)
        val overlayH = (languages.size + 7).coerceAtMost(area.height() - 4)
        val overlayX = area.x() + (area.width() - overlayW) / 2
        val overlayY = area.y() + (area.height() - overlayH) / 2
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)

        frame.buffer().clear(overlayArea)

        val hint = "[↑/↓] Navigate   [Enter] Select   [Esc] Cancel"
        val subtitle = "Choose target language for lyrics"
        val panelTitle = " Select Language "

        val safeSelectedIndex =
            state.languagePicker.selectedIndex.coerceIn(0, (languages.size - 1).coerceAtLeast(0))

        val rows = languages.mapIndexed { index, (code, name) ->
            val isSelected = index == safeSelectedIndex
            val isCurrent = code.equals(currentLang, ignoreCase = true)

            val prefix = if (isSelected) " ▸ " else "   "
            val label = "$name (${code.uppercase()})"
            val badge = if (isCurrent) " [Active] " else ""

            row(
                text(prefix).fg(PRIMARY_COLOR).length(3),
                text(label).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY).fill(),
                if (badge.isNotEmpty()) text(badge).fg(if (isSelected) PRIMARY_COLOR else TEXT_DIM)
                    .length(badge.length)
                else spacer()
            )
        }

        val content = column(
            text(subtitle).fg(TEXT_DIM).centered(),
            text("").length(1),
            *rows.toTypedArray(),
            spacer(),
            text(hint).fg(TEXT_DIM).centered(),
        )

        panel(content)
            .title(panelTitle)
            .rounded()
            .borderColor(BORDER_FOCUSED)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("language-picker-panel")
            .onKeyEvent(onKeyEvent)
            .render(frame, overlayArea, context)
    }

    override fun preferredSize(
        availableWidth: Int,
        availableHeight: Int,
        context: RenderContext
    ): Size =
        Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()

    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult {
        if (!stateProvider().languagePicker.isVisible) return EventResult.UNHANDLED
        return onKeyEvent(event)
    }
}