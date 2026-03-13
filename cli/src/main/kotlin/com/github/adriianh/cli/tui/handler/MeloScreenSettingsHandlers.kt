package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.component.SettingsFocus
import com.github.adriianh.cli.tui.component.SettingsItem
import com.github.adriianh.cli.tui.component.SettingsSection
import com.github.adriianh.cli.tui.component.sectionItems
import com.github.adriianh.core.domain.model.DownloadFormat
import com.github.adriianh.core.domain.model.DownloadQuality
import com.github.adriianh.core.domain.model.MeloAction
import com.github.adriianh.core.domain.model.MeloKey
import com.github.adriianh.core.domain.model.ThemePreset
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.toolkit.event.EventResult
import kotlinx.coroutines.launch
import java.io.File

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
            settingsViewState.focus == SettingsFocus.ITEMS -> {
                settingsViewState = settingsViewState.copy(
                    focus = SettingsFocus.SECTION,
                    cursor = 0
                )
            }
            else -> {
                state = state.copy(isSettingsVisible = false)
                settingsViewState = settingsViewState.copy(
                    cursor = 0,
                    sectionCursor = 0,
                    focus = SettingsFocus.SECTION
                )
            }
        }
        return EventResult.HANDLED
    }

    // ── Listening for keybinding ─────────────────────────────────────────────
    if (settingsViewState.isListeningForKey) {
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

    // ── Keybinding mode ──────────────────────────────────────────────────────
    if (settingsViewState.isKeybindingMode) {
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

    if (settingsViewState.isEditingText) {
        when (event.code()) {
            KeyCode.ESCAPE -> {
                settingsViewState = settingsViewState.copy(isEditingText = false, textInput = "")
            }
            KeyCode.ENTER -> {
                val path = settingsViewState.textInput.trim()
                when {
                    path.isBlank() -> {
                        val newSettings = settingsViewState.currentSettings.copy(downloadPath = null)
                        settingsViewState = settingsViewState.copy(
                            currentSettings = newSettings,
                            isEditingText = false,
                            textInput = ""
                        )
                        scope.launch { updateSettings(newSettings) }
                    }
                    !File(path).exists() -> {
                        settingsViewState = settingsViewState.copy(
                            isEditingText = false,
                            textInput = ""
                        )
                    }
                    !File(path).isDirectory -> {
                        settingsViewState = settingsViewState.copy(
                            isEditingText = false,
                            textInput = ""
                        )
                    }
                    !File(path).canWrite() -> {
                        settingsViewState = settingsViewState.copy(
                            isEditingText = false,
                            textInput = ""
                        )
                    }
                    else -> {
                        val newSettings = settingsViewState.currentSettings.copy(downloadPath = path)
                        settingsViewState = settingsViewState.copy(
                            currentSettings = newSettings,
                            isEditingText = false,
                            textInput = ""
                        )
                        scope.launch { updateSettings(newSettings) }
                    }
                }
            }
            KeyCode.BACKSPACE -> {
                settingsViewState = settingsViewState.copy(
                    textInput = settingsViewState.textInput.dropLast(1)
                )
            }
            KeyCode.CHAR -> {
                settingsViewState = settingsViewState.copy(
                    textInput = settingsViewState.textInput + event.character()
                )
            }
            else -> {}
        }
        return EventResult.HANDLED
    }

    // ── Section focus ────────────────────────────────────────────────────────
    if (settingsViewState.focus == SettingsFocus.SECTION) {
        val sections = SettingsSection.entries
        when (event.code()) {
            KeyCode.UP -> settingsViewState = settingsViewState.copy(
                sectionCursor = (settingsViewState.sectionCursor - 1 + sections.size) % sections.size,
                cursor = 0
            )
            KeyCode.DOWN -> settingsViewState = settingsViewState.copy(
                sectionCursor = (settingsViewState.sectionCursor + 1) % sections.size,
                cursor = 0
            )
            KeyCode.RIGHT, KeyCode.ENTER -> settingsViewState = settingsViewState.copy(
                focus = SettingsFocus.ITEMS,
                cursor = 0
            )
            else -> {}
        }
        return EventResult.HANDLED
    }

    // ── Items focus ──────────────────────────────────────────────────────────
    if (settingsViewState.isEditing) {
        val currentSection = SettingsSection.entries[settingsViewState.sectionCursor]
        val items = sectionItems[currentSection] ?: emptyList()
        val item = items.getOrNull(settingsViewState.cursor) ?: return EventResult.HANDLED
        when (event.code()) {
            KeyCode.LEFT -> adjustSetting(item, -1)
            KeyCode.RIGHT -> adjustSetting(item, 1)
            else -> {}
        }
        return EventResult.HANDLED
    }

    val currentSection = SettingsSection.entries[settingsViewState.sectionCursor]
    val items = sectionItems[currentSection] ?: emptyList()

    when (event.code()) {
        KeyCode.UP -> settingsViewState = settingsViewState.copy(
            cursor = (settingsViewState.cursor - 1 + items.size) % items.size
        )
        KeyCode.DOWN -> settingsViewState = settingsViewState.copy(
            cursor = (settingsViewState.cursor + 1) % items.size
        )
        KeyCode.LEFT -> settingsViewState = settingsViewState.copy(
            focus = SettingsFocus.SECTION,
            cursor = 0
        )
        KeyCode.ENTER -> {
            val item = items.getOrNull(settingsViewState.cursor) ?: return EventResult.HANDLED
            settingsViewState = when (item) {
                SettingsItem.KEYBINDINGS ->
                    settingsViewState.copy(isKeybindingMode = true, keybindingCursor = 0)

                SettingsItem.DOWNLOAD_PATH ->
                    settingsViewState.copy(
                        isEditingText = true,
                        textInput = settingsViewState.currentSettings.downloadPath ?: ""
                    )

                else -> settingsViewState.copy(isEditing = true)
            }
        }        else -> {}
    }
    return EventResult.HANDLED
}

private fun MeloScreen.adjustSetting(item: SettingsItem, direction: Int) {
    val current = settingsViewState.currentSettings
    val newSettings = when (item) {
        SettingsItem.THEME -> {
            val presets = ThemePreset.entries
            val currentIndex = presets.indexOf(current.theme)
            val newIndex = (currentIndex + direction + presets.size) % presets.size
            val newTheme = presets[newIndex]
            MeloTheme.loadTheme(newTheme)
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
        SettingsItem.AUTO_DOWNLOAD -> current.copy(autoDownload = !current.autoDownload)
        SettingsItem.MAX_OFFLINE_SIZE -> {
            val steps = listOf(250, 500, 1000, 2000, 5000)
            val currentIndex = steps.indexOf(current.maxOfflineSizeMb).takeIf { it >= 0 } ?: 1
            val newIndex = (currentIndex + direction).coerceIn(0, steps.lastIndex)
            current.copy(maxOfflineSizeMb = steps[newIndex])
        }
        SettingsItem.MAX_OFFLINE_AGE -> {
            val steps = listOf(7, 15, 30, 60, 90)
            val currentIndex = steps.indexOf(current.maxOfflineAgeDays).takeIf { it >= 0 } ?: 2
            val newIndex = (currentIndex + direction).coerceIn(0, steps.lastIndex)
            current.copy(maxOfflineAgeDays = steps[newIndex])
        }
        SettingsItem.OFFLINE_MODE -> current.copy(offlineMode = !current.offlineMode)
        SettingsItem.KEYBINDINGS -> current
        SettingsItem.DOWNLOAD_FORMAT -> {
            val formats = DownloadFormat.entries
            val currentIndex = formats.indexOf(current.downloadFormat)
            val newIndex = (currentIndex + direction + formats.size) % formats.size
            current.copy(downloadFormat = formats[newIndex])
        }
        SettingsItem.DOWNLOAD_QUALITY -> {
            val qualities = DownloadQuality.entries
            val currentIndex = qualities.indexOf(current.downloadQuality)
            val newIndex = (currentIndex + direction + qualities.size) % qualities.size
            current.copy(downloadQuality = qualities[newIndex])
        }
        SettingsItem.DOWNLOAD_PATH -> current
    }

    settingsViewState = settingsViewState.copy(currentSettings = newSettings)
    scope.launch { updateSettings(newSettings) }
}