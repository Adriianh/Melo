package com.github.adriianh.cli.tui.handler.settings

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.MeloKey
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch

internal fun MeloScreen.handleListeningForKey(event: KeyEvent): EventResult {
    val action = MeloAction.entries[settingsViewState.keybindingCursor]
    val newKey = when {
        event.code() == KeyCode.CHAR -> MeloKey(char = event.character())
        else -> MeloKey(code = event.code().name)
    }
    val conflictAction = settingsViewState.currentSettings.keybindings.entries
        .find { it.value == newKey && it.key != action }?.key

    val updatedMap = settingsViewState.currentSettings.keybindings.toMutableMap()
    updatedMap[action] = newKey
    if (conflictAction != null) updatedMap.remove(conflictAction)

    val newSettings = settingsViewState.currentSettings.copy(keybindings = updatedMap)
    settingsViewState = settingsViewState.copy(currentSettings = newSettings, isListeningForKey = false)
    scope.launch { updateSettings(newSettings) }
    return EventResult.HANDLED
}

internal fun MeloScreen.handleKeybindingKey(event: KeyEvent): EventResult {
    val actions = MeloAction.entries
    when (event.code()) {
        KeyCode.UP -> settingsViewState = settingsViewState.copy(
            keybindingCursor = (settingsViewState.keybindingCursor - 1 + actions.size) % actions.size
        )

        KeyCode.DOWN -> settingsViewState = settingsViewState.copy(
            keybindingCursor = (settingsViewState.keybindingCursor + 1) % actions.size
        )

        KeyCode.ENTER -> settingsViewState = settingsViewState.copy(isListeningForKey = true)
        else -> {}
    }
    return EventResult.HANDLED
}