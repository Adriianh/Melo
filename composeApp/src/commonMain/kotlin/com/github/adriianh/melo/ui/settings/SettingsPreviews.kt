@file:Suppress("FunctionNaming", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.AudioQuality
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.ReleaseAsset
import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.core.domain.model.update.UpdatePlatform
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.melo.util.MeloColors

private fun previewSettingsActions() =
    SettingsActions(
        onThemeModeSelected = {},
        onThemePresetSelected = {},
        onDynamicColorToggle = {},
        onDataSaverToggle = {},
        onAudioQualitySelected = {},
        onOpenLogin = {},
        onLogout = {},
    )

private fun previewUpdateAvailableState() =
    UpdateState.UpdateAvailable(
        currentVersion = "2.2.1",
        release =
            AppRelease(
                version = "2.3.0-nightly.20261113.9f8e7d6c",
                tagName = "v2.3.0-nightly",
                name = "Melo 2.3.0 Nightly",
                channel = UpdateChannel.NIGHTLY,
                htmlUrl = "https://github.com/adriianh/melo",
                releaseNotes = "Mejoras de diseño adaptativo en Ajustes.",
                publishedAt = "2026-11-13",
                assets = emptyList(),
            ),
        targetAsset =
            ReleaseAsset(
                name = "melo-2.3.0-nightly-android.apk",
                downloadUrl = "https://github.com/adriianh/melo",
                sizeBytes = 24_500_000,
                platform = UpdatePlatform.ANDROID,
            ),
    )

@Preview(widthDp = 320, fontScale = 1.3f)
@Composable
internal fun PreviewSettingsCategoryListCompact() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MeloColors.surface1)
                    .padding(16.dp),
        ) {
            SettingsCategoryList(
                categories = SettingsCategory.entries,
                settings = Settings(themeMode = ThemeMode.DARK, dynamicColor = true),
                isLoggedIn = true,
                updateState = previewUpdateAvailableState(),
                onSelectCategory = {},
            )
        }
    }
}

@Preview(widthDp = 320, fontScale = 1.3f)
@Composable
internal fun PreviewSettingsUpdatesLongNightly() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MeloColors.surface1)
                    .padding(16.dp),
        ) {
            UpdateSettingsSectionContent(
                currentVersion = "2.3.0-nightly.20261113.a1b2c3d4e5f6g7h8-linux-x86_64",
                state = previewUpdateAvailableState(),
                autoCheckUpdates = true,
                updateChannel = UpdateChannel.NIGHTLY,
                onToggleAutoCheckUpdates = {},
                onUpdateChannelSelected = {},
                onCheckForUpdates = {},
                onStartDownload = { _, _ -> },
                onCancelDownload = {},
                onInstallUpdate = {},
            )
        }
    }
}

@Preview(widthDp = 320, fontScale = 1.3f)
@Composable
internal fun PreviewSettingsDownloadsAndOptionGrids() {
    val threeOptions = listOf("MP3", "OPUS", "FLAC")
    MaterialTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MeloColors.surface1)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingOptionGrid(
                options = threeOptions,
                isSelected = { it == "OPUS" },
                onSelect = {},
                title = { it },
                hint = { ".${it.lowercase()}" },
            )
            SettingOptionGrid(
                options = AudioQuality.entries,
                isSelected = { it == AudioQuality.HIGH },
                onSelect = {},
                title = { it.uiTitle() },
                hint = { it.uiHint() },
            )
            DownloadsSettingsSection(
                settings = Settings(),
                actions = previewSettingsActions(),
            )
        }
    }
}

@Preview(widthDp = 840, heightDp = 680)
@Composable
internal fun PreviewSettingsExpanded() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MeloColors.surface1)
                    .padding(24.dp),
        ) {
            SettingsContent(
                settings = Settings(themeMode = ThemeMode.DARK, dynamicColor = false),
                isLoggedIn = true,
                profiles = emptyList(),
                activePageId = null,
                isLoadingProfiles = false,
                isSwitchingProfile = false,
                profilesError = null,
                actions = previewSettingsActions(),
                initialCategory = SettingsCategory.APPEARANCE,
                updateState = previewUpdateAvailableState(),
            )
        }
    }
}
