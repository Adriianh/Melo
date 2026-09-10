package com.github.adriianh.cli.tui.handler.settings

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.component.SettingsItem
import com.github.adriianh.core.domain.model.DownloadFormat
import com.github.adriianh.core.domain.model.DownloadQuality
import com.github.adriianh.core.domain.model.ThemePreset
import kotlinx.coroutines.launch

internal fun MeloScreen.adjustSetting(item: SettingsItem, direction: Int) {
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
        SettingsItem.DISCORD_RPC -> {
            val next = !current.discordRpcEnabled
            if (next) {
                discordRpcManager.connect()
                state.player.nowPlaying?.let {
                    val elapsedMs = (state.player.progress * it.durationMs).toLong()
                    discordRpcManager.updateActivity(it, state.player.isPlaying, elapsedMs)
                }
            } else {
                discordRpcManager.disconnect()
            }
            current.copy(discordRpcEnabled = next)
        }
    }

    settingsViewState = settingsViewState.copy(currentSettings = newSettings)
    state = state.copy(isOfflineMode = newSettings.offlineMode)
    scope.launch { updateSettings(newSettings) }
}