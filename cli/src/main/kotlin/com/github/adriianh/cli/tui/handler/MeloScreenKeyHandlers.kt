package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SidebarSection
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
    val isFocused = appRunner()?.focusManager()?.focusedId() == "sidebar-panel"
    if (!isFocused) return EventResult.UNHANDLED

    when {
        event.matches(Actions.MOVE_DOWN) -> {
            val nextIndex = (NAV_SECTIONS.indexOf(state.navigation.activeSection) + 1) % NAV_SECTIONS.size
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
            val section = state.navigation.activeSection
            when (section) {
                SidebarSection.HOME -> appRunner()?.focusManager()?.setFocus("home-panel")
                SidebarSection.SEARCH -> appRunner()?.focusManager()?.setFocus("search-bar")
                SidebarSection.LIBRARY -> appRunner()?.focusManager()?.setFocus("library-panel")
                SidebarSection.STATS -> appRunner()?.focusManager()?.setFocus("stats-panel")
                SidebarSection.OFFLINE -> appRunner()?.focusManager()?.setFocus("offline-panel")
                SidebarSection.SETTINGS -> appRunner()?.focusManager()?.setFocus("settings-panel")
                else -> {}
            }
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.applySidebarSelection(item: SidebarSection) {
    state = state.copy(navigation = state.navigation.copy(activeSection = item))

    when (item) {
        SidebarSection.HOME -> updateScreen<ScreenState.Home> { it }
        SidebarSection.SEARCH -> updateScreen<ScreenState.Search> { it }
        SidebarSection.LIBRARY -> updateScreen<ScreenState.Library> { it }
        SidebarSection.STATS -> updateScreen<ScreenState.Stats> { it }
        SidebarSection.OFFLINE -> updateScreen<ScreenState.Offline> { it }
        SidebarSection.SETTINGS -> state = state.copy(isSettingsVisible = true)
        else -> {}
    }
}

internal fun MeloScreen.handleGlobalShortcuts(event: KeyEvent): EventResult {
    if (state.isSettingsVisible) return handleSettingsKey(event)
    if (state.trackOptions.isVisible) return handleTrackOptionsKey(event)

    val handled = handlePlayerBarKey(event)
    if (handled == EventResult.HANDLED) return EventResult.HANDLED

    when (KeyCode.CHAR) {
        event.code() if event.character() == '/' -> {
            applySidebarSelection(SidebarSection.SEARCH)
            appRunner()?.focusManager()?.setFocus("search-bar")
            return EventResult.HANDLED
        }

        event.code() if event.character() == 'L' -> {
            applySidebarSelection(SidebarSection.LIBRARY)
            appRunner()?.focusManager()?.setFocus("library-panel")
            return EventResult.HANDLED
        }

        else -> {}
    }

    return EventResult.UNHANDLED
}