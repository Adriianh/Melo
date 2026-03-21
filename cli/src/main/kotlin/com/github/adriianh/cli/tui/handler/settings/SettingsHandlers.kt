package com.github.adriianh.cli.tui.handler.settings

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.component.*
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch
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

    if (settingsViewState.isListeningForKey) {
        return handleListeningForKey(event)
    }

    if (settingsViewState.isKeybindingMode) {
        return handleKeybindingKey(event)
    }

    if (settingsViewState.isPickingDirectory) {
        return handleDirectoryPicker(event)
    }

    if (settingsViewState.isEditingText) {
        return handlePathEditing(event)
    }

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
        }

        else -> {}
    }
    return EventResult.HANDLED
}