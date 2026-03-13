package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.graphics.ClearGraphicsWidget
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Settings
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Rect
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.Size
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

/**
 * Sections available in the settings panel.
 */
enum class SettingsSection(val label: String) {
    PERSONALIZATION("Personalization"),
    STORAGE("Storage"),
    DOWNLOADS("Downloads"),
    NETWORK("Network")
}

/**
 * Items available in the settings panel.
 */
enum class SettingsItem(val label: String) {
    THEME("Theme Preset"),
    VOLUME("Default Volume"),
    LANGUAGE("Search Language"),
    ARTWORK_RES("Artwork Resolution"),
    AUTO_DOWNLOAD("Auto-Download Next"),
    MAX_OFFLINE_SIZE("Max Offline Size"),
    MAX_OFFLINE_AGE("Max Offline Age"),
    OFFLINE_MODE("Offline Mode"),
    DOWNLOAD_FORMAT("Download Format"),
    DOWNLOAD_QUALITY("Download Quality"),
    DOWNLOAD_PATH("Download Path"),
    KEYBINDINGS("Custom Keybindings"),
}

enum class SettingsFocus { SECTION, ITEMS }

val sectionItems = mapOf(
    SettingsSection.PERSONALIZATION to listOf(
        SettingsItem.THEME,
        SettingsItem.VOLUME,
        SettingsItem.ARTWORK_RES,
        SettingsItem.KEYBINDINGS
    ),
    SettingsSection.STORAGE to listOf(
        SettingsItem.MAX_OFFLINE_SIZE,
        SettingsItem.MAX_OFFLINE_AGE,
        SettingsItem.OFFLINE_MODE
    ),
    SettingsSection.DOWNLOADS to listOf(
        SettingsItem.AUTO_DOWNLOAD,
        SettingsItem.DOWNLOAD_FORMAT,
        SettingsItem.DOWNLOAD_QUALITY,
        SettingsItem.DOWNLOAD_PATH
    ),
    SettingsSection.NETWORK to listOf(
        SettingsItem.LANGUAGE
    )
)

/**
 * Internal state for the settings view.
 */
data class SettingsViewState(
    val cursor: Int = 0,
    val sectionCursor: Int = 0,
    val focus: SettingsFocus = SettingsFocus.SECTION,
    val isEditing: Boolean = false,
    val isKeybindingMode: Boolean = false,
    val keybindingCursor: Int = 0,
    val isListeningForKey: Boolean = false,
    val currentSettings: Settings = Settings(),
    val isEditingText: Boolean = false,
    val textInput: String = ""
)

class SettingsOverlay(
    private val stateProvider: () -> MeloState,
    private val settingsViewStateProvider: () -> SettingsViewState,
    private val sidebar: ListElement<*>,
    private val settingsList: ListElement<*>,
    private val onKeyEvent: (KeyEvent) -> EventResult = { EventResult.UNHANDLED },
) : Element {

    private val clearGraphics = ClearGraphicsWidget()

    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val state = stateProvider()
        if (!state.isSettingsVisible) return

        val viewState = settingsViewStateProvider()

        val overlayW = (area.width() * 0.7).toInt().coerceAtLeast(70)
        val overlayH = 16
        val overlayX = area.x() + (area.width() - overlayW) / 2
        val overlayY = area.y() + (area.height() - overlayH) / 2
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)

        frame.renderWidget(clearGraphics, overlayArea)
        frame.buffer().clear(overlayArea)

        val currentSection = SettingsSection.entries[viewState.sectionCursor]
        val items = sectionItems[currentSection] ?: emptyList()

        val sidebarItems = SettingsSection.entries.mapIndexed { index, section ->
            val isSelected = index == viewState.sectionCursor
            val isFocused = viewState.focus == SettingsFocus.SECTION
            row(
                text(
                    if (isSelected && isFocused) " ${MeloTheme.ICON_ARROW} ${section.label}"
                    else "   ${section.label}"
                ).fg(
                    if (isSelected) MeloTheme.PRIMARY_COLOR else MeloTheme.TEXT_PRIMARY
                ).apply { if (isSelected) bold() }.fill()
            )
        }

        sidebar.elements(*sidebarItems.toTypedArray())
        sidebar.selected(viewState.sectionCursor)
        sidebar.length(SettingsSection.entries.size)

        val sectionsPanel = panel(sidebar.fill())
            .rounded()
            .borderColor(
                if (viewState.focus == SettingsFocus.SECTION) BORDER_FOCUSED else BORDER_DEFAULT
            )
            .focusable()
            .id("settings-section-list")

        // ── Items panel ─────────────────────────────────────────────────────
        val content = if (viewState.isKeybindingMode) {
            val actions = MeloAction.entries
            val keybindingItems = actions.mapIndexed { index, action ->
                val isSelected = index == viewState.keybindingCursor
                val isListening = isSelected && viewState.isListeningForKey
                val binding = viewState.currentSettings.keybindings[action]
                val keyStr = when {
                    isListening -> "???"
                    binding?.char != null -> if (binding.char == ' ') "Space" else binding.char.toString()
                    binding?.code != null -> binding.code
                    else -> "None"
                }
                val labelColor = if (isSelected) MeloTheme.PRIMARY_COLOR else MeloTheme.TEXT_PRIMARY
                val valueColor = if (isListening) MeloTheme.ACCENT_RED else MeloTheme.TEXT_SECONDARY
                row(
                    text("  ${action.displayName}").fg(labelColor).apply { if (isSelected) bold() }.fill(),
                    text(keyStr).fg(valueColor).apply { if (isListening) bold() }
                )
            }
            settingsList.elements(*keybindingItems.toTypedArray())
            settingsList.selected(viewState.keybindingCursor)
            settingsList.length(keybindingItems.size)
            settingsList.fill()
        } else {
            val settingItems = items.mapIndexed { index, item ->
                val isSelected = index == viewState.cursor
                val isEditingThis = isSelected && viewState.isEditing
                val isFocused = viewState.focus == SettingsFocus.ITEMS

                val valueStr = when (item) {
                    SettingsItem.THEME -> viewState.currentSettings.theme.displayName
                    SettingsItem.VOLUME -> "${viewState.currentSettings.volume}%"
                    SettingsItem.LANGUAGE -> viewState.currentSettings.searchLanguage
                    SettingsItem.ARTWORK_RES -> "${viewState.currentSettings.artworkResolution}px"
                    SettingsItem.AUTO_DOWNLOAD -> if (viewState.currentSettings.autoDownload) "On" else "Off"
                    SettingsItem.MAX_OFFLINE_SIZE -> "${viewState.currentSettings.maxOfflineSizeMb} MB"
                    SettingsItem.MAX_OFFLINE_AGE -> "${viewState.currentSettings.maxOfflineAgeDays} days"
                    SettingsItem.OFFLINE_MODE -> if (viewState.currentSettings.offlineMode) "On" else "Off"
                    SettingsItem.KEYBINDINGS -> "→"
                    SettingsItem.DOWNLOAD_FORMAT -> viewState.currentSettings.downloadFormat.displayName
                    SettingsItem.DOWNLOAD_QUALITY -> viewState.currentSettings.downloadQuality.displayName
                    SettingsItem.DOWNLOAD_PATH -> when {
                        viewState.isEditingText &&
                                items.indexOf(SettingsItem.DOWNLOAD_PATH) == viewState.cursor ->
                            "${viewState.textInput}▌"
                        viewState.currentSettings.downloadPath != null ->
                            viewState.currentSettings.downloadPath
                        else -> "Default"
                    }
                }

                val labelColor = if (isSelected && isFocused) MeloTheme.PRIMARY_COLOR else MeloTheme.TEXT_PRIMARY
                val valueColor = if (isEditingThis) MeloTheme.ACCENT_RED else MeloTheme.TEXT_SECONDARY

                row(
                    text(
                        if (isSelected && isFocused) " ${MeloTheme.ICON_ARROW} ${item.label}"
                        else "   ${item.label}"
                    ).fg(labelColor).apply { if (isSelected && isFocused) bold() }.fill(),
                    text(valueStr).fg(valueColor).apply { if (isEditingThis) bold() }
                )
            }
            settingsList.elements(*settingItems.toTypedArray())
            settingsList.selected(viewState.cursor)
            settingsList.length(settingItems.size)
            settingsList.fill()
        }

        val itemsPanel = panel(content)
            .title(
                if (viewState.isKeybindingMode) "${MeloTheme.ICON_SETTINGS} Keybindings"
                else currentSection.label
            )
            .rounded()
            .borderColor(
                if (viewState.focus == SettingsFocus.ITEMS) BORDER_FOCUSED else BORDER_DEFAULT
            )
            .focusable()
            .id("settings-list")

        // ── Help text ────────────────────────────────────────────────────────
        val helpText = when {
            viewState.isListeningForKey -> "Press any key to bind... [Esc] cancel"
            viewState.isKeybindingMode -> "[↑↓] navigate  [Enter] change  [Esc] back"
            viewState.isEditing -> "[←/→] change  [Esc] apply"
            viewState.focus == SettingsFocus.SECTION -> "[↑↓] navigate  [→] enter section  [Esc] close"
            viewState.isEditingText -> "[Enter] confirm  [Esc] cancel  [Backspace] delete"
            else -> "[↑↓] navigate  [Enter] edit  [←] back  [Esc] close"
        }

        // ── Layout ───────────────────────────────────────────────────────────
        val layout = dock()
            .left(sectionsPanel, Constraint.length(24))
            .center(itemsPanel)
            .fill()

        panel(layout)
            .title("${MeloTheme.ICON_SETTINGS} Settings")
            .bottomTitle(helpText)
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

    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult {
        if (!stateProvider().isSettingsVisible) return EventResult.UNHANDLED
        return onKeyEvent(event)
    }
}