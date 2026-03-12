package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.component.SettingsItem
import com.github.adriianh.core.domain.model.ThemePreset
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.toolkit.event.EventResult
import kotlinx.coroutines.launch

fun MeloScreen.handleSettingsKey(event: KeyEvent): EventResult {
    if (!state.isSettingsVisible) return EventResult.UNHANDLED

    if (event.code() == KeyCode.ESCAPE) {
        if (settingsViewState.isEditing) {
            settingsViewState = settingsViewState.copy(isEditing = false)
        } else {
            state = state.copy(isSettingsVisible = false)
            settingsViewState = settingsViewState.copy(cursor = 0)
        }
        return EventResult.HANDLED
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
                val newCursor = (settingsViewState.cursor - 1).coerceAtLeast(0)
                settingsViewState = settingsViewState.copy(cursor = newCursor)
                return EventResult.HANDLED
            }

            KeyCode.DOWN -> {
                val newCursor = (settingsViewState.cursor + 1).coerceAtMost(SettingsItem.entries.size - 1)
                settingsViewState = settingsViewState.copy(cursor = newCursor)
                return EventResult.HANDLED
            }

            KeyCode.ENTER -> {
                settingsViewState = settingsViewState.copy(isEditing = true)
                return EventResult.HANDLED
            }

            else -> return EventResult.UNHANDLED
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

        SettingsItem.CACHE_SIZE -> {
            val steps = listOf(100, 250, 500, 1000, 2048)
            val currentIndex = steps.indexOf(current.cacheSizeLimitMb).takeIf { it >= 0 } ?: 2
            val newIndex = (currentIndex + direction).coerceIn(0, steps.lastIndex)
            current.copy(cacheSizeLimitMb = steps[newIndex])
        }
    }

    settingsViewState = settingsViewState.copy(currentSettings = newSettings)

    scope.launch {
        updateSettings(newSettings)
    }
}
