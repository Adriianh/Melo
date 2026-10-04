package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.SidebarSection
import com.github.adriianh.cli.tui.component.buildHelpRows
import com.github.adriianh.cli.tui.component.computeHelpOverlayDimensions
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

private fun MeloScreen.closeHelpOverlay() {
    state = state.copy(helpOverlay = state.helpOverlay.copy(isVisible = false, scrollOffset = 0))
    val targetFocus =
        when (state.navigation.activeSection) {
            SidebarSection.HOME -> "home-panel"
            SidebarSection.SEARCH -> "results-panel"
            SidebarSection.LIBRARY -> "library-panel"
            SidebarSection.NOW_PLAYING -> "now-playing-panel"
            SidebarSection.STATS -> "stats-panel"
            SidebarSection.OFFLINE -> "offline-panel"
            SidebarSection.SETTINGS -> "settings-panel"
        }
    appRunner()?.focusManager()?.setFocus(targetFocus)
}

private fun MeloScreen.updateHelpScrollOffset(newOffset: Int) {
    state = state.copy(helpOverlay = state.helpOverlay.copy(scrollOffset = newOffset))
}

private fun KeyEvent.isCloseHelpKey(): Boolean = code() == KeyCode.ESCAPE || isChar('q') || code() == KeyCode.ENTER

private fun KeyEvent.isScrollDown(): Boolean = matches(Actions.MOVE_DOWN) || isChar('j')

private fun KeyEvent.isScrollUp(): Boolean = matches(Actions.MOVE_UP) || isChar('k')

private fun KeyEvent.isScrollTop(): Boolean = code() == KeyCode.HOME || isChar('g')

private fun KeyEvent.isScrollBottom(): Boolean = code() == KeyCode.END || isChar('G')

private fun MeloScreen.handleHelpScroll(
    event: KeyEvent,
    maxScroll: Int,
    visibleLines: Int,
): Boolean {
    val currentOffset = state.helpOverlay.scrollOffset
    return when {
        event.isScrollDown() -> {
            updateHelpScrollOffset(minOf(maxScroll, currentOffset + 1))
            true
        }
        event.isScrollUp() -> {
            updateHelpScrollOffset(maxOf(0, currentOffset - 1))
            true
        }
        event.code() == KeyCode.PAGE_DOWN -> {
            updateHelpScrollOffset(minOf(maxScroll, currentOffset + visibleLines))
            true
        }
        event.code() == KeyCode.PAGE_UP -> {
            updateHelpScrollOffset(maxOf(0, currentOffset - visibleLines))
            true
        }
        event.isScrollTop() -> {
            updateHelpScrollOffset(0)
            true
        }
        event.isScrollBottom() -> {
            updateHelpScrollOffset(maxScroll)
            true
        }
        else -> false
    }
}

internal fun MeloScreen.handleHelpOverlayKey(event: KeyEvent): EventResult {
    val helpState = state.helpOverlay
    if (!helpState.isVisible) return EventResult.UNHANDLED

    val allRows = buildHelpRows(settingsViewState.currentSettings)
    val terminalSize =
        try {
            appRunner()
                ?.tuiRunner()
                ?.terminal()
                ?.size()
        } catch (_: Exception) {
            null
        }
    val terminalW = terminalSize?.width() ?: 100
    val terminalH = terminalSize?.height() ?: 30
    val (_, overlayH) = computeHelpOverlayDimensions(terminalW, terminalH)
    val visibleLines = (overlayH - 4).coerceAtLeast(1)
    val maxScroll = maxOf(0, allRows.size - visibleLines)

    val handled =
        when {
            event.isCloseHelpKey() -> {
                closeHelpOverlay()
                true
            }
            event.isChar(':') -> {
                closeHelpOverlay()
                openCommandBar()
                true
            }
            else -> handleHelpScroll(event, maxScroll, visibleLines)
        }

    return if (handled) EventResult.HANDLED else EventResult.UNHANDLED
}
