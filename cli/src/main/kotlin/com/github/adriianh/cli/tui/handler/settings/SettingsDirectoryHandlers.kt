package com.github.adriianh.cli.tui.handler.settings

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.component.*
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch
import java.io.File

internal fun MeloScreen.handleDirectoryPicker(event: KeyEvent): EventResult {
    val picker = settingsViewState.directoryPicker

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

            else -> {}
        }
        return EventResult.HANDLED
    }

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

    if (picker.errorMessage != null) {
        settingsViewState = settingsViewState.copy(
            directoryPicker = picker.clearError()
        )
        return EventResult.HANDLED
    }

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
                    val pickerState = settingsViewState.directoryPicker
                    val item = pickerState.targetItem

                    if (item == SettingsItem.LOCAL_FOLDERS) {
                        settingsViewState = settingsViewState.copy(
                            directoryPicker = pickerState.toggleMark()
                        )
                        state = state.copy()
                    } else {
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

internal fun MeloScreen.handlePathEditing(event: KeyEvent): EventResult {
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

                !File(path).exists() || !File(path).isDirectory || !File(path).canWrite() -> {
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