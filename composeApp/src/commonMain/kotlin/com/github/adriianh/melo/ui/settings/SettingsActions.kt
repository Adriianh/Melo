package com.github.adriianh.melo.ui.settings

import com.github.adriianh.core.domain.model.AudioQuality
import com.github.adriianh.core.domain.model.DownloadFormat
import com.github.adriianh.core.domain.model.DownloadQuality
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.ThemePreset

data class SettingsActions(
    val onThemeModeSelected: (ThemeMode) -> Unit,
    val onThemePresetSelected: (ThemePreset) -> Unit,
    val onDynamicColorToggle: (Boolean) -> Unit,
    val onDataSaverToggle: (Boolean) -> Unit,
    val onAudioQualitySelected: (AudioQuality) -> Unit,
    val onDownloadFormatSelected: (DownloadFormat) -> Unit = {},
    val onDownloadQualitySelected: (DownloadQuality) -> Unit = {},
    val onAutoplayToggle: (Boolean) -> Unit = {},
    val onDiscordRpcToggle: (Boolean) -> Unit = {},
    val onAddLocalPath: (String) -> Unit = {},
    val onRemoveLocalPath: (String) -> Unit = {},
    val onOpenLogin: () -> Unit,
    val onLogout: () -> Unit,
)