@file:Suppress("FunctionNaming", "TooManyFunctions", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.ThemePreset
import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.ReleaseAsset
import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.core.domain.model.update.UpdatePlatform
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.PlatformType

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
        currentVersion = "2.2.1-nightly.20261110.1a2b3c4d5e6f",
        release =
            AppRelease(
                version = "2.3.0-nightly.20261113.9f8e7d6c5b4a-universal",
                tagName = "v2.3.0-nightly",
                name = "Melo 2.3.0 Nightly",
                channel = UpdateChannel.NIGHTLY,
                htmlUrl = "https://github.com/adriianh/melo",
                releaseNotes = "Mejoras de diseño adaptativo y accesibilidad en Ajustes.",
                publishedAt = "2026-11-13",
                assets = emptyList(),
            ),
        targetAsset =
            ReleaseAsset(
                name = "melo-2.3.0-nightly-android-universal.apk",
                downloadUrl = "https://github.com/adriianh/melo",
                sizeBytes = 24_500_000,
                platform = UpdatePlatform.ANDROID,
            ),
    )

@Preview(widthDp = 320, fontScale = 1.3f)
@Composable
internal fun PreviewSettingsCategoryList() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MeloColors.surface1)
                    .padding(16.dp),
        ) {
            SettingsCategoryList(
                categories = SettingsCategory.visibleFor(PlatformType.ANDROID),
                settings =
                    Settings(
                        themeMode = ThemeMode.DARK,
                        dynamicColor = false,
                        theme = ThemePreset.CATPPUCCIN_MOCHA,
                    ),
                isLoggedIn = false,
                updateState = previewUpdateAvailableState(),
                onSelectCategory = {},
            )
        }
    }
}

@Composable
private fun PreviewCategoryPane(
    category: SettingsCategory,
    settings: Settings = Settings(),
    isLoggedIn: Boolean = false,
    updateState: UpdateState = UpdateState.Idle,
    currentVersion: String = "2.3.0-preview",
    initialManualFolderExpanded: Boolean = false,
) {
    val entries =
        rememberSettingEntries(
            settings = settings,
            isLoggedIn = isLoggedIn,
            profiles = emptyList(),
            activePageId = null,
            isLoadingProfiles = false,
            isSwitchingProfile = false,
            profilesError = null,
            actions = previewSettingsActions(),
            platformType = PlatformType.ANDROID,
            updateState = updateState,
            onCheckForUpdates = {},
            onUpdateChannelSelected = {},
            onStartUpdateDownload = { _, _ -> },
            onCancelUpdateDownload = {},
            onInstallUpdate = {},
            currentVersion = currentVersion,
            initialManualFolderExpanded = initialManualFolderExpanded,
        )
    SettingsCategoryEntriesPane(entries = entries, category = category)
}

@Preview(widthDp = 320, fontScale = 1.3f)
@Composable
internal fun PreviewAppearanceDynamicColorOn() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MeloColors.surface1)
                    .padding(16.dp),
        ) {
            PreviewCategoryPane(
                category = SettingsCategory.APPEARANCE,
                settings =
                    Settings(
                        themeMode = ThemeMode.DARK,
                        dynamicColor = true,
                        theme = ThemePreset.CATPPUCCIN_MOCHA,
                    ),
            )
        }
    }
}

@Preview(widthDp = 320, fontScale = 1.3f)
@Composable
internal fun PreviewAppearanceDynamicColorOff() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MeloColors.surface1)
                    .padding(16.dp),
        ) {
            PreviewCategoryPane(
                category = SettingsCategory.APPEARANCE,
                settings =
                    Settings(
                        themeMode = ThemeMode.DARK,
                        dynamicColor = false,
                        theme = ThemePreset.CATPPUCCIN_MOCHA,
                    ),
            )
        }
    }
}

@Preview(widthDp = 320, fontScale = 1.3f)
@Composable
internal fun PreviewAccountLoggedOut() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MeloColors.surface1)
                    .padding(16.dp),
        ) {
            PreviewCategoryPane(
                category = SettingsCategory.ACCOUNT,
                settings = Settings(syncHistoryToYouTube = true),
                isLoggedIn = false,
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
            PreviewCategoryPane(
                category = SettingsCategory.UPDATES,
                settings = Settings(autoCheckUpdates = true, updateChannel = UpdateChannel.NIGHTLY),
                updateState = previewUpdateAvailableState(),
                currentVersion = "2.2.1-nightly.20261110.1a2b3c4d5e6f-linux-x86_64",
            )
        }
    }
}

@Preview(widthDp = 320, fontScale = 1.3f)
@Composable
internal fun PreviewLocalFoldersStates() {
    val samplePaths =
        listOf(
            "/storage/emulated/0/Music/Colección FLAC sin pérdida",
            "/storage/emulated/0/Download/Melo Offline Tracks",
            "/mnt/media_rw/sdcard1/Audio/Álbumes Completos 2026",
        )
    MaterialTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MeloColors.surface1)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PreviewLocalFoldersEntry(paths = emptyList())
            PreviewLocalFoldersEntry(paths = samplePaths)
            PreviewLocalFoldersEntry(
                paths = samplePaths.take(1),
                initialManualExpanded = true,
            )
        }
    }
}

@Composable
private fun PreviewLocalFoldersEntry(
    paths: List<String>,
    initialManualExpanded: Boolean = false,
) {
    val entries =
        rememberSettingEntries(
            settings = Settings(localLibraryPaths = paths),
            isLoggedIn = false,
            profiles = emptyList(),
            activePageId = null,
            isLoadingProfiles = false,
            isSwitchingProfile = false,
            profilesError = null,
            actions = previewSettingsActions(),
            platformType = PlatformType.ANDROID,
            updateState = UpdateState.Idle,
            onCheckForUpdates = {},
            onUpdateChannelSelected = {},
            onStartUpdateDownload = { _, _ -> },
            onCancelUpdateDownload = {},
            onInstallUpdate = {},
            initialManualFolderExpanded = initialManualExpanded,
        ).filter { it.id == "library_local_folders" }
    SettingsEntriesSections(entries = entries)
}

@Preview(widthDp = 880, heightDp = 720)
@Composable
internal fun PreviewSettingsExpandedDialog() {
    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MeloColors.surface0)
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            SettingsDialogSurface(
                settings =
                    Settings(
                        themeMode = ThemeMode.DARK,
                        dynamicColor = false,
                        theme = ThemePreset.CATPPUCCIN_MOCHA,
                    ),
                isLoggedIn = true,
                profiles = emptyList(),
                activePageId = null,
                isLoadingProfiles = false,
                isSwitchingProfile = false,
                profilesError = null,
                actions = previewSettingsActions(),
                onDismiss = {},
                initialCategory = SettingsCategory.APPEARANCE,
                updateState = previewUpdateAvailableState(),
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
internal fun PreviewSettingsConfirmDialog() {
    MaterialTheme {
        SettingsConfirmDialog(
            title = "Cerrar sesión",
            message =
                "¿Estás seguro de que deseas cerrar tu sesión de YouTube Music? " +
                    "Dejarás de sincronizar tu biblioteca, listas y recomendaciones " +
                    "hasta que vuelvas a iniciar sesión.",
            confirmText = "Cerrar sesión",
            icon = Icons.AutoMirrored.Filled.Logout,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
