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
import androidx.compose.material.icons.filled.DeleteSweep
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
            AppearanceSettingsSection(
                settings =
                    Settings(
                        themeMode = ThemeMode.DARK,
                        dynamicColor = true,
                        theme = ThemePreset.CATPPUCCIN_MOCHA,
                    ),
                actions = previewSettingsActions(),
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
            AppearanceSettingsSection(
                settings =
                    Settings(
                        themeMode = ThemeMode.DARK,
                        dynamicColor = false,
                        theme = ThemePreset.CATPPUCCIN_MOCHA,
                    ),
                actions = previewSettingsActions(),
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
            AccountSettingsSection(
                settings = Settings(syncHistoryToYouTube = true),
                isLoggedIn = false,
                profiles = emptyList(),
                activePageId = null,
                isLoadingProfiles = false,
                isSwitchingProfile = false,
                profilesError = null,
                actions = previewSettingsActions(),
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
                currentVersion = "2.2.1-nightly.20261110.1a2b3c4d5e6f-linux-x86_64",
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
            LocalFoldersSection(
                settings = Settings(localLibraryPaths = emptyList()),
                actions = previewSettingsActions(),
            )
            LocalFoldersSection(
                settings = Settings(localLibraryPaths = samplePaths),
                actions = previewSettingsActions(),
            )
            LocalFoldersSection(
                settings = Settings(localLibraryPaths = samplePaths.take(1)),
                actions = previewSettingsActions(),
                initialManualExpanded = true,
            )
        }
    }
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
            title = "Limpiar caché de audio",
            message =
                "¿Estás seguro de que deseas eliminar las pistas almacenadas temporalmente en caché? " +
                    "Tus canciones descargadas y archivos locales no se verán afectados.",
            confirmText = "Limpiar caché",
            icon = Icons.Default.DeleteSweep,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
