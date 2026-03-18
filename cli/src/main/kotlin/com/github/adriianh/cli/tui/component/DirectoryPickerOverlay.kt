package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.graphics.ClearGraphicsWidget
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Rect
import dev.tamboui.style.Color
import dev.tamboui.terminal.Frame
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.RenderContext
import dev.tamboui.toolkit.element.Size
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

/**
 * Overlay element that renders a directory browser for picking
 * a folder path in settings (e.g. DOWNLOAD_PATH, CACHE_PATH).
 */
class DirectoryPickerOverlay(
    private val settingsViewStateProvider: () -> SettingsViewState,
    private val onKeyEvent: (KeyEvent) -> EventResult = { EventResult.UNHANDLED },
) : Element {

    private val clearGraphics = ClearGraphicsWidget()

    override fun render(frame: Frame, area: Rect, context: RenderContext) {
        val viewState = settingsViewStateProvider()
        if (!viewState.isPickingDirectory) return

        val picker = viewState.directoryPicker

        // ── Overlay dimensions ──────────────────────────────────────
        val overlayW = (area.width() * 0.6).toInt().coerceAtLeast(50)
        val overlayH = (area.height() * 0.6).toInt().coerceIn(12, 30)
        val overlayX = area.x() + (area.width() - overlayW) / 2
        val overlayY = area.y() + (area.height() - overlayH) / 2
        val overlayArea = Rect(overlayX, overlayY, overlayW, overlayH)

        frame.renderWidget(clearGraphics, overlayArea)
        frame.buffer().clear(overlayArea)

        // ── Current path bar ────────────────────────────────────────
        val pathText = text("  ${MeloTheme.ICON_FOLDER_OPENED} ${picker.currentDirectory}")
            .fg(MeloTheme.TEXT_SECONDARY)
            .fill()

        // ── Bottom bar (help text, error, mkdir input, or delete confirm) ──
        val bottomElement = when {
            picker.errorMessage != null -> {
                text("  ⚠ ${picker.errorMessage}  [any key to dismiss]")
                    .fg(Color.RED).bold().fill()
            }

            picker.isCreatingDir -> {
                row(
                    text("  New folder: ").fg(MeloTheme.TEXT_SECONDARY),
                    text("${picker.newDirName}▌").fg(MeloTheme.PRIMARY_COLOR).bold().fill()
                ).length(1)
            }

            picker.isConfirmingDelete -> {
                val entryName = picker.entries.getOrNull(picker.cursor)?.name ?: ""
                text("  Delete \"$entryName\"? [y/n]")
                    .fg(Color.YELLOW).bold().fill()
            }

            else -> {
                text("[Enter] enter  [Space] mark  [Bksp] up  [n] mkdir  [d] delete  [Esc] save & exit")
                    .fg(MeloTheme.TEXT_SECONDARY).dim().fill()
            }
        }

        // ── Directory entries ───────────────────────────────────────
        val entries = picker.entries
        val offset = picker.scrollOffset
        val visibleCount = (overlayH - 6).coerceAtLeast(1)
        val actualVisible = visibleCount.coerceAtMost((entries.size - offset).coerceAtLeast(0))

        val entryElements = if (actualVisible == 0) {
            arrayOf(text("  Empty directory").fg(MeloTheme.TEXT_SECONDARY).dim().fill())
        } else {
            Array(actualVisible) { i ->
                val entryIndex = offset + i
                val entry = entries[entryIndex]
                val isSelected = entryIndex == picker.cursor

                val icon = if (entry.name == "..") MeloTheme.ICON_FOLDER else MeloTheme.ICON_FOLDER_OPENED
                val displayName = if (entry.name == "..") ".." else "${entry.name}/"

                val path = picker.currentDirectory.resolve(entry.name).toAbsolutePath().normalize().toString()
                val isMarked = if (entry.name == "..") false else picker.markedPaths.contains(path)
                val markPrefix = if (isMarked) "[*] " else ""
                val prefix = if (isSelected) " ${MeloTheme.ICON_ARROW} " else "   "
                val labelColor = if (isSelected) MeloTheme.PRIMARY_COLOR else MeloTheme.TEXT_PRIMARY

                row(
                    text("$prefix $markPrefix $icon $displayName")
                        .fg(labelColor)
                        .apply { if (isSelected) bold() }
                        .fill()
                ).length(1)
            }
        }

        val fileList = column(*entryElements).fill()

        // ── Title ───────────────────────────────────────────────────
        val title = when (picker.targetItem) {
            SettingsItem.DOWNLOAD_PATH -> "${MeloTheme.ICON_FOLDER_OPENED} Select Download Path"
            SettingsItem.CACHE_PATH -> "${MeloTheme.ICON_FOLDER_OPENED} Select Cache Path"
            else -> "${MeloTheme.ICON_FOLDER_OPENED} Select Directory"
        }

        // ── Compose overlay ─────────────────────────────────────────
        val content = dock()
            .top(pathText, Constraint.length(1))
            .bottom(bottomElement, Constraint.length(1))
            .center(fileList)
            .fill()

        panel(content)
            .title(title)
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("directory-picker-panel")
            .onKeyEvent(onKeyEvent)
            .render(frame, overlayArea, context)
    }

    override fun preferredSize(availableWidth: Int, availableHeight: Int, context: RenderContext): Size =
        Size.UNKNOWN

    override fun constraint(): Constraint = Constraint.fill()

    override fun handleKeyEvent(event: KeyEvent, focused: Boolean): EventResult {
        val viewState = settingsViewStateProvider()
        if (!viewState.isPickingDirectory) return EventResult.UNHANDLED
        return onKeyEvent(event)
    }
}