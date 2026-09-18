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
            (charVal != null && isChar(charVal))
}

internal fun MeloScreen.handleSidebarKey(event: KeyEvent): EventResult {
    when {
        event.matches(Actions.MOVE_DOWN) -> {
            val currentIndex = NAV_SECTIONS.indexOf(state.navigation.activeSection)
            val nextIndex = (currentIndex + 1) % NAV_SECTIONS.size
            val section = NAV_SECTIONS[nextIndex]
            applySidebarSelection(section)
            if (section != SidebarSection.SETTINGS) {
                switchScreenWithoutFocus(section)
            }
            return EventResult.HANDLED
        }

        event.matches(Actions.MOVE_UP) -> {
            val currentIndex = NAV_SECTIONS.indexOf(state.navigation.activeSection)
            val prevIndex = if (currentIndex <= 0) NAV_SECTIONS.size - 1 else currentIndex - 1
            val section = NAV_SECTIONS[prevIndex]
            applySidebarSelection(section)
            if (section != SidebarSection.SETTINGS) {
                switchScreenWithoutFocus(section)
            }
            return EventResult.HANDLED
        }

        event.code() == KeyCode.ENTER || event.matches(Actions.MOVE_RIGHT) -> {
            activateSidebarSelection(state.navigation.activeSection)
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}

internal fun MeloScreen.targetScreenFor(item: SidebarSection): ScreenState = when (item) {
    SidebarSection.HOME -> cachedHomeScreen
    SidebarSection.SEARCH -> ScreenState.Search()
    SidebarSection.LIBRARY -> ScreenState.Library()
    SidebarSection.NOW_PLAYING -> ScreenState.NowPlaying()
    SidebarSection.STATS -> cachedStatsScreen
    SidebarSection.OFFLINE -> ScreenState.Offline(downloads = state.collections.offlineTracks)
    SidebarSection.SETTINGS -> state.screen
}

internal fun MeloScreen.switchScreenWithoutFocus(item: SidebarSection) {
    if (item == SidebarSection.SETTINGS) return
    val targetScreen = targetScreenFor(item)
    state = state.copy(
        screen = targetScreen,
        navigation = state.navigation.copy(activeSection = item, pendingSection = null),
        detail = state.detail.copy(artworkData = if (item != SidebarSection.SEARCH) null else state.detail.artworkData),
        needsGraphicsClear = false
    )
    if (item == SidebarSection.HOME) {
        if (cachedHomeScreen.feedSections.isEmpty() && !cachedHomeScreen.isLoadingFeed && cachedHomeScreen.feedError == null) {
            loadHomeFeed()
        }
    } else if (item == SidebarSection.STATS) {
        if (cachedStatsScreen.statsListening == null && !cachedStatsScreen.statsLoading) {
            loadStats()
        }
    }
}

internal fun MeloScreen.activateSidebarSelection(item: SidebarSection) {
    if (item == SidebarSection.SETTINGS) {
        state = state.copy(isSettingsVisible = true)
        appRunner()?.focusManager()?.setFocus("settings-panel")
        return
    }

    applySidebarSelection(item)
    val targetScreen = targetScreenFor(item)
    state = state.copy(
        screen = targetScreen,
        navigation = state.navigation.copy(activeSection = item, pendingSection = null),
        detail = state.detail.copy(artworkData = if (item != SidebarSection.SEARCH) null else state.detail.artworkData),
        needsGraphicsClear = false
    )

    when (item) {
        SidebarSection.HOME -> {
            appRunner()?.focusManager()?.setFocus("home-panel")
            if (cachedHomeScreen.feedSections.isEmpty() && !cachedHomeScreen.isLoadingFeed && cachedHomeScreen.feedError == null) {
                loadHomeFeed()
            } else {
                enrichActiveSectionTracks()
            }
        }
        SidebarSection.SEARCH -> appRunner()?.focusManager()?.setFocus("search-bar")
        SidebarSection.LIBRARY -> appRunner()?.focusManager()?.setFocus("library-panel")
        SidebarSection.NOW_PLAYING -> appRunner()?.focusManager()?.setFocus("now-playing-panel")
        SidebarSection.STATS -> {
            appRunner()?.focusManager()?.setFocus("stats-panel")
            loadStats()
        }
        SidebarSection.OFFLINE -> appRunner()?.focusManager()?.setFocus("offline-panel")
    }
}

internal fun MeloScreen.applySidebarSelection(item: SidebarSection) {
    state = state.copy(navigation = state.navigation.copy(activeSection = item))

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

    if (appRunner()?.focusManager()?.focusedId() == "search-bar") return true

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

    if (!(isTyping && isCharacter)) {
        val handled = handlePlayerBarKey(event)
        if (handled == EventResult.HANDLED) return EventResult.HANDLED
    }

    if (isTyping && isCharacter) return EventResult.UNHANDLED

    when (event.code()) {
        KeyCode.CHAR -> {
            if (event.isChar(':')) {
                val currentFocus = appRunner()?.focusManager()?.focusedId()
                state = state.copy(
                    commandBar = state.commandBar.copy(
                        isVisible = true,
                        input = "",
                        errorMessage = null,
                        cursorPosition = 0,
                        previousFocusId = currentFocus,
                        suggestions = CommandBarHandlers.computeSuggestions(""),
                        selectedSuggestionIndex = null
                    )
                )
                appRunner()?.focusManager()?.setFocus("command-bar")
                return EventResult.HANDLED
            }
            if (event.isChar('/')) {
                activateSidebarSelection(SidebarSection.SEARCH)
                return EventResult.HANDLED
            }
            if (event.isChar('L')) {
                activateSidebarSelection(SidebarSection.LIBRARY)
                return EventResult.HANDLED
            }
        }
        else -> {}
    }

    return EventResult.UNHANDLED
}