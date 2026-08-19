package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.ThemePreset
import com.github.adriianh.melo.ui.components.SegmentedControl
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: Settings,
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onThemePresetSelected: (ThemePreset) -> Unit,
    onDynamicColorToggle: (Boolean) -> Unit,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Settings",
                style = MeloType.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MeloColors.textPrimary
            )

            SettingSection(
                title = "Theme mode",
                subtitle = "Follow the system or force light/dark mode."
            ) {
                SegmentedControl(
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    onSelect = onThemeModeSelected,
                    label = {
                        when (it) {
                            ThemeMode.SYSTEM -> "System"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        }
                    }
                )
            }

            SettingSection(
                title = "Dynamic colors",
                subtitle = "Use colors extracted from the current track's artwork."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = if (settings.dynamicColor) "Enabled" else "Disabled",
                        style = MeloType.body,
                        color = MeloColors.textSecondary
                    )
                    Switch(
                        checked = settings.dynamicColor,
                        onCheckedChange = onDynamicColorToggle
                    )
                }
            }

            SettingSection(
                title = "Color preset",
                subtitle = "Choose a fixed accent palette (overridden if dynamic colors are enabled)."
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ThemePreset.entries) { preset ->
                        val selected = preset == settings.theme
                        FilterChip(
                            selected = selected,
                            onClick = { onThemePresetSelected(preset) },
                            label = { Text(preset.displayName, style = MeloType.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MeloColors.surface1,
                                labelColor = MeloColors.textPrimary,
                                selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = MeloColors.borderStrong,
                                selectedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            SettingSection(
                title = "Session",
                subtitle = if (isLoggedIn) {
                    "Your account is connected."
                } else {
                    "Sign in to sync your library."
                }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isLoggedIn) {
                        OutlinedButton(onClick = onLogout) {
                            Text("Log out")
                        }
                    } else {
                        Button(onClick = onOpenLogin) {
                            Text("Open login")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MeloType.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MeloColors.textPrimary
            )
            Text(
                text = subtitle,
                style = MeloType.body,
                color = MeloColors.textMuted
            )
        }

        content()
    }
}