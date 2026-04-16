package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SidebarSection
import com.github.adriianh.cli.tui.handler.CommandBarHandlers.handleCommandBarKey
import com.github.adriianh.cli.tui.handler.playback.handlePlayerBarKey
import com.github.adriianh.cli.tui.handler.playback.handleTrackOptionsKey
import com.github.adriianh.cli.tui.handler.settings.handleSettingsKey
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.Settings
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.bindings.Actions
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent

val NAV_SECTIONS = listOf(
    SidebarSection.HOME,
    SidebarSection.SEARCH,
    SidebarSection.LIBRARY,
    SidebarSection.NOW_PLAYING,
    SidebarSection.STATS,
    SidebarSection.OFFLINE,
    SidebarSection.SETTINGS
)

internal fun KeyEvent.matchesAction(action: MeloAction, settings: Settings): Boolean {
    val binding = settings.keybindings[action] ?: return false
    val codeStr = binding.code
    val charVal = binding.char
    return (codeStr != null && code() == KeyCode.valueOf(codeStr)) ||
            (charVal != null && character() == charVal)
}

internal fun MeloScreen.handleSidebarKey(event: KeyEvent): EventResult {
    when {
        event.matches(Actions.MOVE_DOWN) -> {
            val currentIndex = NAV_SECTIONS.indexOf(state.navigation.activeSection)
            val nextIndex = (currentIndex + 1) % NAV_SECTIONS.size
            applySidebarSelection(NAV_SECTIONS[nextIndex])
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            val currentIndex = NAV_SECTIONS.indexOf(state.navigation.activeSection)
            val prevIndex = if (currentIndex <= 0) NAV_SECTIONS.size - 1 else currentIndex - 1
            applySidebarSelection(NAV_SECTIONS[prevIndex])
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER -> {
            activateSidebarSelection(state.navigation.activeSection)
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.activateSidebarSelection(item: SidebarSection) {
    if (item == SidebarSection.SETTINGS) {
        state = state.copy(isSettingsVisible = true)
        appRunner()?.focusManager()?.setFocus("settings-panel")
        return
    }

    state = state.copy(
        needsGraphicsClear = true,
        navigation = state.navigation.copy(pendingSection = item)
    )

    when (item) {
        SidebarSection.HOME -> appRunner()?.focusManager()?.setFocus("home-panel")
        SidebarSection.SEARCH -> appRunner()?.focusManager()?.setFocus("search-bar")
        SidebarSection.LIBRARY -> appRunner()?.focusManager()?.setFocus("library-panel")
        SidebarSection.STATS -> appRunner()?.focusManager()?.setFocus("stats-panel")
        SidebarSection.OFFLINE -> appRunner()?.focusManager()?.setFocus("offline-panel")
        else -> {}
    }
}

internal fun MeloScreen.applySidebarSelection(item: SidebarSection) {
    state = state.copy(navigation = state.navigation.copy(activeSection = item))

    // Synchronize visual list selections
    val index = NAV_SECTIONS.indexOf(item)
    if (index < 4) {
        sidebarNavList.selected(index)
        sidebarUtilList.selected(-1)
        state = state.copy(navigation = state.navigation.copy(sidebarInUtil = false))
    } else {
        sidebarNavList.selected(-1)
        sidebarUtilList.selected(index - 4)
        state = state.copy(navigation = state.navigation.copy(sidebarInUtil = true))
    }
}

internal fun MeloScreen.isTyping(): Boolean {
    if (state.commandBar.isVisible) return true

    // Search bar is focused
    if (appRunner()?.focusManager()?.focusedId() == "search-bar") return true

    // Library/Offline "typing mode" (isTyping flag in screen state)
    val libraryState = state.screen as? ScreenState.Library
    if (libraryState?.isTyping == true) return true

    val offlineState = state.screen as? ScreenState.Offline

    return offlineState?.isTyping == true
}

internal fun MeloScreen.handleGlobalShortcuts(event: KeyEvent): EventResult {
    if (state.isSettingsVisible) return handleSettingsKey(event)
    if (state.trackOptions.isVisible) return handleTrackOptionsKey(event)
    if (state.commandBar.isVisible) return handleCommandBarKey(event)

    val isTyping = isTyping()
    val isCharacter = event.code() == KeyCode.CHAR

    // Skip playback keys if typing a character in an input
    if (!(isTyping && isCharacter)) {
        val handled = handlePlayerBarKey(event)
        if (handled == EventResult.HANDLED) return EventResult.HANDLED
    }

    // If we're typing, we don't want global navigation shortcuts to trigger
    if (isTyping && isCharacter) return EventResult.UNHANDLED

    when (event.code()) {
        KeyCode.CHAR -> {
            if (event.character() == ':') {
                val currentFocus = appRunner()?.focusManager()?.focusedId()
                state = state.copy(
                    commandBar = state.commandBar.copy(
                        isVisible = true,
                        input = "",
                        errorMessage = null,
                        cursorPosition = 0,
                        previousFocusId = currentFocus
                    )
                )
                appRunner()?.focusManager()?.setFocus("command-bar")
                return EventResult.HANDLED
            }
            if (event.character() == '/') {
                applySidebarSelection(SidebarSection.SEARCH)
                activateSidebarSelection(SidebarSection.SEARCH)
                return EventResult.HANDLED
            }
            if (event.character() == 'L') {
                applySidebarSelection(SidebarSection.LIBRARY)
                activateSidebarSelection(SidebarSection.LIBRARY)
                return EventResult.HANDLED
            }
        }
        else -> {}
    }

    return EventResult.UNHANDLED
}