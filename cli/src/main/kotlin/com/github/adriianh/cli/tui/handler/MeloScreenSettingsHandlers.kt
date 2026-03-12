package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.component.SettingsItem
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.ThemePreset
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.toolkit.event.EventResult
import kotlinx.coroutines.launch

fun MeloScreen.handleSettingsKey(event: KeyEvent): EventResult {
    if (!state.isSettingsVisible) return EventResult.UNHANDLED

    if (event.code() == KeyCode.ESCAPE) {
        when {
            settingsViewState.isListeningForKey -> {
                settingsViewState = settingsViewState.copy(isListeningForKey = false)
            }

            settingsViewState.isKeybindingMode -> {
                settingsViewState = settingsViewState.copy(isKeybindingMode = false, keybindingCursor = 0)
            }

            settingsViewState.isEditing -> {
                settingsViewState = settingsViewState.copy(isEditing = false)
            }

            else -> {
                state = state.copy(isSettingsVisible = false)
                settingsViewState = settingsViewState.copy(cursor = 0)
            }
        }
        return EventResult.HANDLED
    }

    if (settingsViewState.isListeningForKey) {
        val action = MeloAction.entries[settingsViewState.keybindingCursor]
        val newKey = when {
            event.code() == KeyCode.CHAR -> com.github.adriianh.core.domain.model.MeloKey(char = event.character())
            else -> com.github.adriianh.core.domain.model.MeloKey(code = event.code().name)
        }

        // Conflict check
        val conflictAction = settingsViewState.currentSettings.keybindings.entries.find {
            it.value == newKey && it.key != action
        }?.key

        if (conflictAction != null) {
            val updatedMap = settingsViewState.currentSettings.keybindings.toMutableMap()
            updatedMap[action] = newKey
            updatedMap.entries.removeIf { it.key == conflictAction }

            val newSettings = settingsViewState.currentSettings.copy(keybindings = updatedMap)
            settingsViewState = settingsViewState.copy(currentSettings = newSettings, isListeningForKey = false)
            scope.launch { updateSettings(newSettings) }
        } else {
            val updatedMap = settingsViewState.currentSettings.keybindings.toMutableMap()
            updatedMap[action] = newKey
            val newSettings = settingsViewState.currentSettings.copy(keybindings = updatedMap)
            settingsViewState = settingsViewState.copy(currentSettings = newSettings, isListeningForKey = false)
            scope.launch { updateSettings(newSettings) }
        }
        return EventResult.HANDLED
    }

    if (settingsViewState.isKeybindingMode) {
        val actions = MeloAction.entries
        when (event.code()) {
            KeyCode.UP -> {
                settingsViewState =
                    settingsViewState.copy(keybindingCursor = (settingsViewState.keybindingCursor - 1 + actions.size) % actions.size)
                return EventResult.HANDLED
            }

            KeyCode.DOWN -> {
                settingsViewState =
                    settingsViewState.copy(keybindingCursor = (settingsViewState.keybindingCursor + 1) % actions.size)
                return EventResult.HANDLED
            }

            KeyCode.ENTER -> {
                settingsViewState = settingsViewState.copy(isListeningForKey = true)
                return EventResult.HANDLED
            }

            else -> return EventResult.HANDLED
        }
    }

    if (settingsViewState.isEditing) {
        val item = SettingsItem.entries[settingsViewState.cursor]
        when (event.code()) {
            KeyCode.LEFT -> adjustSetting(item, -1)
            KeyCode.RIGHT -> adjustSetting(item, 1)
            else -> {}
        }
        return EventResult.HANDLED
    } else {
        when (event.code()) {
            KeyCode.UP -> {
                val newCursor = (settingsViewState.cursor - 1 + SettingsItem.entries.size) % SettingsItem.entries.size
                settingsViewState = settingsViewState.copy(cursor = newCursor)
                return EventResult.HANDLED
            }

            KeyCode.DOWN -> {
                val newCursor = (settingsViewState.cursor + 1) % SettingsItem.entries.size
                settingsViewState = settingsViewState.copy(cursor = newCursor)
                return EventResult.HANDLED
            }

            KeyCode.ENTER -> {
                if (SettingsItem.entries[settingsViewState.cursor] == SettingsItem.KEYBINDINGS) {
                    settingsViewState = settingsViewState.copy(isKeybindingMode = true, keybindingCursor = 0)
                } else {
                    settingsViewState = settingsViewState.copy(isEditing = true)
                }
                return EventResult.HANDLED
            }

            else -> return EventResult.HANDLED
        }
    }
}

private fun MeloScreen.adjustSetting(item: SettingsItem, direction: Int) {
    val current = settingsViewState.currentSettings
    val newSettings = when (item) {
        SettingsItem.THEME -> {
            val presets = ThemePreset.entries
            val currentIndex = presets.indexOf(current.theme)
            val newIndex = (currentIndex + direction + presets.size) % presets.size
            val newTheme = presets[newIndex]
            MeloTheme.loadTheme(newTheme) // Live Preview
            current.copy(theme = newTheme)
        }

        SettingsItem.VOLUME -> {
            val newVol = (current.volume + (direction * 5)).coerceIn(0, 100)
            audioPlayer.setVolume(newVol)
            current.copy(volume = newVol)
        }

        SettingsItem.LANGUAGE -> {
            val newLang = if (current.searchLanguage == "en") "es" else "en"
            current.copy(searchLanguage = newLang)
        }

        SettingsItem.ARTWORK_RES -> {
            val steps = listOf(150, 300, 500, 800)
            val currentIndex = steps.indexOf(current.artworkResolution).takeIf { it >= 0 } ?: 1
            val newIndex = (currentIndex + direction).coerceIn(0, steps.lastIndex)
            current.copy(artworkResolution = steps[newIndex])
        }

        SettingsItem.AUTO_DOWNLOAD -> {
            current.copy(autoDownload = !current.autoDownload)
        }

        SettingsItem.MAX_OFFLINE_SIZE -> {
            val steps = listOf(250, 500, 1000, 2000, 5000)
            val currentIndex = steps.indexOf(current.maxOfflineSizeMb).takeIf { it >= 0 } ?: 1
            val newIndex = (currentIndex + direction).coerceIn(0, steps.lastIndex)
            current.copy(maxOfflineSizeMb = steps[newIndex])
        }

        SettingsItem.KEYBINDINGS -> current
    }

    settingsViewState = settingsViewState.copy(currentSettings = newSettings)

    scope.launch {
        updateSettings(newSettings)
    }
}
