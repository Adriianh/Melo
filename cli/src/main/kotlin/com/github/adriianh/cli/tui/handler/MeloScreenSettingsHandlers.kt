package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.component.*
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
import java.nio.file.Path

fun MeloScreen.handleSettingsKey(event: KeyEvent): EventResult {
    if (!state.isSettingsVisible) return EventResult.UNHANDLED

    if (event.code() == KeyCode.ESCAPE) {
        when {
            settingsViewState.isPickingDirectory -> {
                val picker = settingsViewState.directoryPicker
                when {
                    picker.isCreatingDir -> settingsViewState = settingsViewState.copy(
                        directoryPicker = picker.cancelMkdir()
                    )
                    picker.isConfirmingDelete -> settingsViewState = settingsViewState.copy(
                        directoryPicker = picker.cancelDelete()
                    )
                    picker.errorMessage != null -> settingsViewState = settingsViewState.copy(
                        directoryPicker = picker.clearError()
                    )
                    else -> {
                        if (picker.targetItem == SettingsItem.LOCAL_FOLDERS) {
                            val newPaths = picker.markedPaths.filter { it.startsWith("/") }.toList()
                            val newSettings = settingsViewState.currentSettings.copy(localLibraryPaths = newPaths)
                            settingsViewState = settingsViewState.copy(
                                isPickingDirectory = false,
                                currentSettings = newSettings
                            )
                            state = state.copy()
                            scope.launch { updateSettings(newSettings) }
                        } else {
                            settingsViewState = settingsViewState.copy(isPickingDirectory = false)
                            state = state.copy()
                        }
                    }
                }
            }
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

    // ── Directory picker mode ─────────────────────────────────────────────
    if (settingsViewState.isPickingDirectory) {
        val picker = settingsViewState.directoryPicker

        // Sub-state: creating a new directory (text input mode)
        if (picker.isCreatingDir) {
            when (event.code()) {
                KeyCode.ENTER -> settingsViewState = settingsViewState.copy(
                    directoryPicker = picker.confirmMkdir()
                )
                KeyCode.BACKSPACE -> settingsViewState = settingsViewState.copy(
                    directoryPicker = picker.backspaceNewDir()
                )
                KeyCode.CHAR -> settingsViewState = settingsViewState.copy(
                    directoryPicker = picker.appendToNewDir(event.character())
                )
                // ESC is handled by the top-level ESC block
                else -> {}
            }
            return EventResult.HANDLED
        }

        // Sub-state: confirming deletion
        if (picker.isConfirmingDelete) {
            when {
                event.isCharIgnoreCase('y') -> settingsViewState = settingsViewState.copy(
                    directoryPicker = picker.confirmDelete()
                )
                event.isCharIgnoreCase('n') || event.code() == KeyCode.ESCAPE -> settingsViewState = settingsViewState.copy(
                    directoryPicker = picker.cancelDelete()
                )
            }
            return EventResult.HANDLED
        }

        // Sub-state: error message displayed
        if (picker.errorMessage != null) {
            // Any key dismisses the error
            settingsViewState = settingsViewState.copy(
                directoryPicker = picker.clearError()
            )
            return EventResult.HANDLED
        }

        // Normal picker navigation
        when (event.code()) {
            KeyCode.UP -> {
                settingsViewState = settingsViewState.copy(
                    directoryPicker = picker.cursorUp()
                )
                state = state.copy()
            }
            KeyCode.DOWN -> {
                settingsViewState = settingsViewState.copy(
                    directoryPicker = picker.cursorDown()
                )
                state = state.copy()
            }
            KeyCode.CHAR -> {
                when (event.character()) {
                    ' ' -> {
                        // Toggle mark for multi-select, or select for single-select
                        val pickerState = settingsViewState.directoryPicker
                        val item = pickerState.targetItem
                        
                        if (item == SettingsItem.LOCAL_FOLDERS) {
                            settingsViewState = settingsViewState.copy(
                                directoryPicker = pickerState.toggleMark()
                            )
                            state = state.copy() // Force UI re-draw to show [*] indicator
                        } else {
                            // Single-select mode (DOWNLOAD_PATH, CACHE_PATH)
                            val path = pickerState.currentDirectory.toString()
                            val newSettings = when (item) {
                                SettingsItem.CACHE_PATH -> settingsViewState.currentSettings.copy(cachePath = path)
                                SettingsItem.DOWNLOAD_PATH -> settingsViewState.currentSettings.copy(downloadPath = path)
                                else -> settingsViewState.currentSettings
                            }
                            settingsViewState = settingsViewState.copy(
                                isPickingDirectory = false,
                                currentSettings = newSettings
                            )
                            scope.launch { updateSettings(newSettings) }
                        }
                    }
                    '<' -> settingsViewState = settingsViewState.copy(
                        directoryPicker = picker.navigateUp()
                    )
                    '>' -> settingsViewState = settingsViewState.copy(
                        directoryPicker = picker.enter()
                    )
                    'n', 'N' -> settingsViewState = settingsViewState.copy(
                        directoryPicker = picker.startMkdir()
                    )
                    'd', 'D' -> settingsViewState = settingsViewState.copy(
                        directoryPicker = picker.startDelete()
                    )
                }
            }
            KeyCode.ENTER -> {
                val pickerState = settingsViewState.directoryPicker
                settingsViewState = settingsViewState.copy(
                    directoryPicker = pickerState.enter()
                )
                state = state.copy()
            }
            KeyCode.BACKSPACE -> {
                settingsViewState = settingsViewState.copy(
                    directoryPicker = picker.navigateUp()
                )
                state = state.copy()
            }
            else -> {}
        }
        return EventResult.HANDLED
    }

    if (settingsViewState.isEditingText) {
        when (event.code()) {
            KeyCode.ESCAPE -> {
                settingsViewState = settingsViewState.copy(isEditingText = false, textInput = "", editingTextItem = null)
            }
            KeyCode.ENTER -> {
                val path = settingsViewState.textInput.trim()
                val item = settingsViewState.editingTextItem
                when {
                    path.isBlank() -> {
                        val newSettings = when (item) {
                            SettingsItem.CACHE_PATH -> settingsViewState.currentSettings.copy(cachePath = null)
                            SettingsItem.DOWNLOAD_PATH -> settingsViewState.currentSettings.copy(downloadPath = null)
                            else -> settingsViewState.currentSettings
                        }
                        settingsViewState = settingsViewState.copy(
                            currentSettings = newSettings,
                            isEditingText = false,
                            textInput = "",
                            editingTextItem = null
                        )
                        scope.launch { updateSettings(newSettings) }
                    }
                    !File(path).exists() -> {
                        settingsViewState = settingsViewState.copy(
                            isEditingText = false,
                            textInput = "",
                            editingTextItem = null
                        )
                    }
                    !File(path).isDirectory -> {
                        settingsViewState = settingsViewState.copy(
                            isEditingText = false,
                            textInput = "",
                            editingTextItem = null
                        )
                    }
                    !File(path).canWrite() -> {
                        settingsViewState = settingsViewState.copy(
                            isEditingText = false,
                            textInput = "",
                            editingTextItem = null
                        )
                    }
                    else -> {
                        val newSettings = when (item) {
                            SettingsItem.CACHE_PATH -> settingsViewState.currentSettings.copy(cachePath = path)
                            SettingsItem.DOWNLOAD_PATH -> settingsViewState.currentSettings.copy(downloadPath = path)
                            else -> settingsViewState.currentSettings
                        }
                        settingsViewState = settingsViewState.copy(
                            currentSettings = newSettings,
                            isEditingText = false,
                            textInput = "",
                            editingTextItem = null
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

        KeyCode.CHAR -> {
            val item = items.getOrNull(settingsViewState.cursor)
            if (item == SettingsItem.LOCAL_FOLDERS && (event.character() == 'd' || event.character() == 'D')) {
                val newSettings = settingsViewState.currentSettings.copy(localLibraryPaths = emptyList())
                settingsViewState = settingsViewState.copy(currentSettings = newSettings)
                scope.launch { updateSettings(newSettings) }
                return EventResult.HANDLED
            }
            return EventResult.UNHANDLED
        }

        KeyCode.LEFT -> settingsViewState = settingsViewState.copy(
            focus = SettingsFocus.SECTION,
            cursor = 0
        )
        KeyCode.ENTER -> {
            val item = items.getOrNull(settingsViewState.cursor) ?: return EventResult.HANDLED
            settingsViewState = when (item) {
                SettingsItem.KEYBINDINGS ->
                    settingsViewState.copy(isKeybindingMode = true, keybindingCursor = 0)

                SettingsItem.CACHE_PATH ->
                    settingsViewState.copy(
                        isPickingDirectory = true,
                        directoryPicker = DirectoryPickerState(
                            currentDirectory = Path.of(
                                settingsViewState.currentSettings.cachePath
                                    ?: System.getProperty("user.home")
                            ),
                            targetItem = SettingsItem.CACHE_PATH
                        ).refresh()
                    )
                            SettingsItem.DOWNLOAD_PATH ->
                                settingsViewState.copy(
                                    isPickingDirectory = true,
                                    directoryPicker = DirectoryPickerState(
                                        currentDirectory = Path.of(
                                            settingsViewState.currentSettings.downloadPath
                                                ?: System.getProperty("user.home")
                                        ),
                                        targetItem = SettingsItem.DOWNLOAD_PATH
                                    ).refresh()
                                )

                            SettingsItem.LOCAL_FOLDERS ->
                                settingsViewState.copy(
                                    isPickingDirectory = true,
                                    directoryPicker = DirectoryPickerState(
                                        currentDirectory = Path.of(System.getProperty("user.home")),
                                        targetItem = SettingsItem.LOCAL_FOLDERS,
                                        markedPaths = settingsViewState.currentSettings.localLibraryPaths
                                            .mapNotNull {
                                                try {
                                                    Path.of(it).toAbsolutePath().normalize().toString()
                                                } catch (_: Exception) {
                                                    null
                                                }
                                            }.toSet()
                                    ).refresh()
                                )

                else -> settingsViewState.copy(isEditing = true)
            }
        } else -> {}
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
        SettingsItem.CACHE_PATH -> current
        SettingsItem.LOCAL_FOLDERS -> current
    }

    settingsViewState = settingsViewState.copy(currentSettings = newSettings)
    state = state.copy() 
    scope.launch { updateSettings(newSettings) }
}