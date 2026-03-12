package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.ICON_SETTINGS
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.graphics.ClearGraphicsWidget
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.ThemePreset
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Rect
import dev.tamboui.style.Style
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.Size
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

/**
 * Items available in the settings panel.
 */
enum class SettingsItem(val label: String) {
    THEME("Theme Preset"),
    VOLUME("Default Volume"),
    LANGUAGE("Search Language"),
    ARTWORK_RES("Artwork Resolution"),
    CACHE_SIZE("Cache Size Limit (MB)"),
}

/**
 * Internal state for the settings view.
 */
data class SettingsViewState(
    val cursor: Int = 0,
    val isEditing: Boolean = false,
    val currentSettings: Settings = Settings()
)

class SettingsOverlay(
    private val stateProvider: () -> MeloState,
    private val settingsViewStateProvider: () -> SettingsViewState,
    private val settingsList: ListElement<*>,
    private val onKeyEvent: (KeyEvent) -> EventResult = { EventResult.UNHANDLED },
) : Element {

    private val clearGraphics = ClearGraphicsWidget()

    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val state = stateProvider()

        if (!state.isSettingsVisible) return

        val viewState = settingsViewStateProvider()

        val overlayW = (area.width() * 0.5).toInt().coerceAtLeast(50)
        val overlayH = (SettingsItem.entries.size + 4).coerceIn(10, 20)
        val overlayX = area.x() + (area.width() - overlayW) / 2
        val overlayY = area.y() + (area.height() - overlayH) / 2
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)

        frame.renderWidget(clearGraphics, overlayArea)
        frame.buffer().clear(overlayArea)

        val items = SettingsItem.entries.mapIndexed { index, item ->
            val isSelected = index == viewState.cursor
            val isEditingThis = isSelected && viewState.isEditing

            val valueStr = when (item) {
                SettingsItem.THEME -> viewState.currentSettings.theme.displayName
                SettingsItem.VOLUME -> "${viewState.currentSettings.volume}%"
                SettingsItem.LANGUAGE -> viewState.currentSettings.searchLanguage
                SettingsItem.ARTWORK_RES -> "${viewState.currentSettings.artworkResolution}px"
                SettingsItem.CACHE_SIZE -> "${viewState.currentSettings.cacheSizeLimitMb} MB"
            }

            val labelColor = if (isSelected) MeloTheme.PRIMARY_COLOR else MeloTheme.TEXT_PRIMARY
            val valueColor = if (isEditingThis) MeloTheme.ACCENT_RED else MeloTheme.TEXT_SECONDARY
            val indicator = if (isEditingThis) ">" else if (isSelected) " " else " "

            row(
                text("$indicator ${item.label}").fg(labelColor).apply { if (isSelected) bold() }.fill(),
                text(valueStr).fg(valueColor).apply { if (isEditingThis) bold() }
            )
        }

        settingsList.elements(*items.toTypedArray())
        settingsList.selected(viewState.cursor)
        settingsList.fill()

        val helpText = if (viewState.isEditing)
            "[ESC] apply/cancel  [←/→] change value"
        else
            "[↑/↓] navigate  [Enter] edit  [ESC] close"

        panel(
            column(
                settingsList,
                spacer(),
                text(helpText).fg(MeloTheme.TEXT_DIM).centered()
            )
        )
            .title("${MeloTheme.ICON_SETTINGS} Settings")
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("settings-panel")
            .onKeyEvent(onKeyEvent)
            .render(frame, overlayArea, context)
    }

    override fun preferredSize(availableWidth: Int, availableHeight: Int, context: RenderContext): Size =
        Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()

    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult =
        EventResult.UNHANDLED
}
