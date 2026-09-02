package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.AudioQuality
import com.github.adriianh.core.domain.model.DownloadFormat
import com.github.adriianh.core.domain.model.DownloadQuality
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.ThemePreset
import com.github.adriianh.melo.ui.components.LocalFolderPathsEditor
import com.github.adriianh.melo.ui.components.SegmentedControl
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType

@Composable
internal fun SettingsContent(
    settings: Settings,
    isLoggedIn: Boolean,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AppearanceSettingsSection(settings, actions)
        PlaybackSettingsSection(settings, actions)
        DownloadsSettingsSection(settings, actions)
        LocalFoldersSection(settings, actions)
        AccountSettingsSection(isLoggedIn, actions)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AppearanceSettingsSection(
    settings: Settings,
    actions: SettingsActions,
) {
    SettingsGroupCard(
        title = "Apariencia y tema",
        icon = Icons.Default.ColorLens
    ) {
        SettingRow(
            title = "Modo de tema",
            subtitle = "Sigue la configuración del sistema o fuerza claro/oscuro."
        ) {
            SegmentedControl(
                options = ThemeMode.entries,
                selected = settings.themeMode,
                onSelect = actions.onThemeModeSelected,
                label = {
                    when (it) {
                        ThemeMode.SYSTEM -> "Sistema"
                        ThemeMode.LIGHT -> "Claro"
                        ThemeMode.DARK -> "Oscuro"
                    }
                }
            )
        }

        SettingRow(
            title = "Color dinámico",
            subtitle = "Armoniza la interfaz con los colores de la carátula actual."
        ) {
            MeloSettingsSwitch(
                checked = settings.dynamicColor,
                onCheckedChange = actions.onDynamicColorToggle
            )
        }

        if (!settings.dynamicColor) {
            SettingRow(
                title = "Paleta de color",
                subtitle = "Selecciona una paleta de acento fija para la interfaz."
            ) {
                SettingOptionChips(
                    options = ThemePreset.entries,
                    isSelected = { it == settings.theme },
                    onSelect = actions.onThemePresetSelected,
                    label = { it.displayName }
                )
            }
        }
    }
}

@Composable
private fun PlaybackSettingsSection(
    settings: Settings,
    actions: SettingsActions,
) {
    SettingsGroupCard(
        title = "Reproducción y streaming",
        icon = Icons.Default.Headphones
    ) {
        SettingRow(
            title = "Ahorro de datos",
            subtitle = "Prioriza bitrates más bajos para minimizar el consumo móvil."
        ) {
            MeloSettingsSwitch(
                checked = settings.dataSaver,
                onCheckedChange = actions.onDataSaverToggle
            )
        }

        if (!settings.dataSaver) {
            SettingRow(
                title = "Calidad de streaming",
                subtitle = "Tasa de bits para la reproducción online."
            ) {
                SettingOptionChips(
                    options = AudioQuality.entries,
                    isSelected = { it == settings.audioQuality },
                    onSelect = actions.onAudioQualitySelected,
                    label = { it.displayName }
                )
            }
        }

        SettingRow(
            title = "Reproducción continua",
            subtitle = "Añade canciones similares al finalizar la cola."
        ) {
            MeloSettingsSwitch(
                checked = settings.autoplay,
                onCheckedChange = actions.onAutoplayToggle
            )
        }
    }
}

@Composable
private fun DownloadsSettingsSection(
    settings: Settings,
    actions: SettingsActions,
) {
    SettingsGroupCard(
        title = "Descargas y offline",
        icon = Icons.Default.Download
    ) {
        SettingRow(
            title = "Formato de descarga",
            subtitle = "Extensión y contenedor preferido para guardar pistas."
        ) {
            SettingOptionChips(
                options = DownloadFormat.entries,
                isSelected = { it == settings.downloadFormat },
                onSelect = actions.onDownloadFormatSelected,
                label = { it.name }
            )
        }

        SettingRow(
            title = "Calidad de descarga",
            subtitle = "Tasa de bits máxima al descargar."
        ) {
            SettingOptionChips(
                options = DownloadQuality.entries,
                isSelected = { it == settings.downloadQuality },
                onSelect = actions.onDownloadQualitySelected,
                label = { "${it.name} (${it.displayName})" }
            )
        }
    }
}

@Composable
private fun LocalFoldersSection(
    settings: Settings,
    actions: SettingsActions,
) {
    SettingsGroupCard(
        title = "Carpetas de música local",
        icon = Icons.Default.Folder
    ) {
        Text(
            text = "Configura carpetas de tu dispositivo para indexar canciones locales.",
            style = MeloType.labelSmall,
            color = MeloColors.textSecondary
        )

        LocalFolderPathsEditor(
            configuredPaths = settings.localLibraryPaths,
            onAddPath = actions.onAddLocalPath,
            onRemovePath = actions.onRemoveLocalPath
        )
    }
}

@Composable
private fun AccountSettingsSection(
    isLoggedIn: Boolean,
    actions: SettingsActions,
) {
    SettingsGroupCard(
        title = "Cuenta y sesión",
        icon = Icons.Default.Person
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLoggedIn) "Sesión iniciada con YouTube Music" else "Sin conexión de cuenta",
                    style = MeloType.body,
                    fontWeight = FontWeight.SemiBold,
                    color = MeloColors.textPrimary
                )
                Text(
                    text = if (isLoggedIn) "Tus playlists, likes y biblioteca están sincronizados." else "Inicia sesión para sincronizar tus playlists y favoritos.",
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isLoggedIn) {
                OutlinedButton(
                    onClick = actions.onLogout,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cerrar sesión")
                }
            } else {
                Button(
                    onClick = actions.onOpenLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Iniciar sesión")
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        color = MeloColors.surface2.copy(alpha = 0.45f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MeloColors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MeloType.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MeloColors.textPrimary
                )
            }

            content()
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MeloType.body,
                fontWeight = FontWeight.Medium,
                color = MeloColors.textPrimary
            )
            Text(
                text = subtitle,
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary
            )
        }
        content()
    }
}

@Composable
private fun <T> SettingOptionChips(
    options: List<T>,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    label: (T) -> String,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(options) { option ->
            val selected = isSelected(option)
            FilterChip(
                selected = selected,
                onClick = { onSelect(option) },
                label = { Text(label(option), style = MeloType.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MeloColors.surface2,
                    labelColor = MeloColors.textSecondary,
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(
                        alpha = 0.2f
                    ),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MeloColors.border,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderWidth = 0.5.dp,
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
}

@Composable
private fun MeloSettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.primary,
            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    )
}