package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.adriianh.core.domain.model.AudioQuality
import com.github.adriianh.core.domain.model.DownloadFormat
import com.github.adriianh.core.domain.model.DownloadQuality
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.ThemePreset
import com.github.adriianh.core.platform.PlatformFileSystem
import com.github.adriianh.melo.ui.components.SegmentedControl
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.rememberDirectoryPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: Settings,
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onThemePresetSelected: (ThemePreset) -> Unit,
    onDynamicColorToggle: (Boolean) -> Unit,
    onDataSaverToggle: (Boolean) -> Unit,
    onAudioQualitySelected: (AudioQuality) -> Unit,
    onDownloadFormatSelected: (DownloadFormat) -> Unit = {},
    onDownloadQualitySelected: (DownloadQuality) -> Unit = {},
    onAutoplayToggle: (Boolean) -> Unit = {},
    onDiscordRpcToggle: (Boolean) -> Unit = {},
    onAddLocalPath: (String) -> Unit = {},
    onRemoveLocalPath: (String) -> Unit = {},
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MeloColors.surface1,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .size(36.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MeloColors.textMuted.copy(alpha = 0.4f))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        SettingsContent(
            settings = settings,
            isLoggedIn = isLoggedIn,
            onThemeModeSelected = onThemeModeSelected,
            onThemePresetSelected = onThemePresetSelected,
            onDynamicColorToggle = onDynamicColorToggle,
            onDataSaverToggle = onDataSaverToggle,
            onAudioQualitySelected = onAudioQualitySelected,
            onDownloadFormatSelected = onDownloadFormatSelected,
            onDownloadQualitySelected = onDownloadQualitySelected,
            onAutoplayToggle = onAutoplayToggle,
            onDiscordRpcToggle = onDiscordRpcToggle,
            onAddLocalPath = onAddLocalPath,
            onRemoveLocalPath = onRemoveLocalPath,
            onOpenLogin = onOpenLogin,
            onLogout = onLogout,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun SettingsDialog(
    settings: Settings,
    isLoggedIn: Boolean,
    onDismiss: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onThemePresetSelected: (ThemePreset) -> Unit,
    onDynamicColorToggle: (Boolean) -> Unit,
    onDataSaverToggle: (Boolean) -> Unit,
    onAudioQualitySelected: (AudioQuality) -> Unit,
    onDownloadFormatSelected: (DownloadFormat) -> Unit = {},
    onDownloadQualitySelected: (DownloadQuality) -> Unit = {},
    onAutoplayToggle: (Boolean) -> Unit = {},
    onDiscordRpcToggle: (Boolean) -> Unit = {},
    onAddLocalPath: (String) -> Unit = {},
    onRemoveLocalPath: (String) -> Unit = {},
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(min = 450.dp, max = 560.dp)
                .height(680.dp),
            shape = RoundedCornerShape(24.dp),
            color = MeloColors.surface1,
            border = BorderStroke(1.dp, MeloColors.border)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Ajustes",
                            style = MeloType.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MeloColors.textPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MeloColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsContent(
                    settings = settings,
                    isLoggedIn = isLoggedIn,
                    onThemeModeSelected = onThemeModeSelected,
                    onThemePresetSelected = onThemePresetSelected,
                    onDynamicColorToggle = onDynamicColorToggle,
                    onDataSaverToggle = onDataSaverToggle,
                    onAudioQualitySelected = onAudioQualitySelected,
                    onDownloadFormatSelected = onDownloadFormatSelected,
                    onDownloadQualitySelected = onDownloadQualitySelected,
                    onAutoplayToggle = onAutoplayToggle,
                    onDiscordRpcToggle = onDiscordRpcToggle,
                    onAddLocalPath = onAddLocalPath,
                    onRemoveLocalPath = onRemoveLocalPath,
                    onOpenLogin = onOpenLogin,
                    onLogout = onLogout,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: Settings,
    isLoggedIn: Boolean,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onThemePresetSelected: (ThemePreset) -> Unit,
    onDynamicColorToggle: (Boolean) -> Unit,
    onDataSaverToggle: (Boolean) -> Unit,
    onAudioQualitySelected: (AudioQuality) -> Unit,
    onDownloadFormatSelected: (DownloadFormat) -> Unit,
    onDownloadQualitySelected: (DownloadQuality) -> Unit,
    onAutoplayToggle: (Boolean) -> Unit,
    onDiscordRpcToggle: (Boolean) -> Unit,
    onAddLocalPath: (String) -> Unit,
    onRemoveLocalPath: (String) -> Unit,
    onOpenLogin: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    var newLocalPathInput by remember { mutableStateOf("") }
    val launchDirectoryPicker = rememberDirectoryPicker { selectedPath ->
        onAddLocalPath(selectedPath)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(20.dp)
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
                    onSelect = onThemeModeSelected,
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
                Switch(
                    checked = settings.dynamicColor,
                    onCheckedChange = onDynamicColorToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            }

            if (!settings.dynamicColor) {
                SettingRow(
                    title = "Paleta de color",
                    subtitle = "Selecciona una paleta de acento fija para la interfaz."
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(ThemePreset.entries) { preset ->
                            val selected = preset == settings.theme
                            FilterChip(
                                selected = selected,
                                onClick = { onThemePresetSelected(preset) },
                                label = { Text(preset.displayName, style = MeloType.labelMedium) },
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
            }
        }

        SettingsGroupCard(
            title = "Reproducción y streaming",
            icon = Icons.Default.Headphones
        ) {
            SettingRow(
                title = "Ahorro de datos",
                subtitle = "Prioriza bitrates más bajos para minimizar el consumo móvil."
            ) {
                Switch(
                    checked = settings.dataSaver,
                    onCheckedChange = onDataSaverToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            }

            if (!settings.dataSaver) {
                SettingRow(
                    title = "Calidad de streaming",
                    subtitle = "Tasa de bits para la reproducción online."
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(AudioQuality.entries) { quality ->
                            val selected = quality == settings.audioQuality
                            FilterChip(
                                selected = selected,
                                onClick = { onAudioQualitySelected(quality) },
                                label = { Text(quality.displayName, style = MeloType.labelMedium) },
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
            }

            SettingRow(
                title = "Reproducción continua",
                subtitle = "Añade canciones similares al finalizar la cola."
            ) {
                Switch(
                    checked = settings.autoplay,
                    onCheckedChange = onAutoplayToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            }
        }

        SettingsGroupCard(
            title = "Descargas y offline",
            icon = Icons.Default.Download
        ) {
            SettingRow(
                title = "Formato de descarga",
                subtitle = "Extensión y contenedor preferido para guardar pistas."
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(DownloadFormat.entries) { format ->
                        val selected = format == settings.downloadFormat
                        FilterChip(
                            selected = selected,
                            onClick = { onDownloadFormatSelected(format) },
                            label = { Text(format.name, style = MeloType.labelMedium) },
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

            SettingRow(
                title = "Calidad de descarga",
                subtitle = "Tasa de bits máxima al descargar."
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(DownloadQuality.entries) { quality ->
                        val selected = quality == settings.downloadQuality
                        FilterChip(
                            selected = selected,
                            onClick = { onDownloadQualitySelected(quality) },
                            label = {
                                Text(
                                    "${quality.name} (${quality.displayName})",
                                    style = MeloType.labelMedium
                                )
                            },
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
        }

        SettingsGroupCard(
            title = "Carpetas de música local",
            icon = Icons.Default.Folder
        ) {
            Text(
                text = "Configura carpetas de tu dispositivo para indexar canciones locales.",
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newLocalPathInput,
                    onValueChange = { newLocalPathInput = it },
                    placeholder = { Text("Ruta personalizada", style = MeloType.body) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MeloColors.borderStrong
                    )
                )

                IconButton(
                    onClick = {
                        if (newLocalPathInput.isNotBlank()) {
                            onAddLocalPath(newLocalPathInput.trim())
                            newLocalPathInput = ""
                        }
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Añadir",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = launchDirectoryPicker,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Elegir")
                }
            }

            if (settings.localLibraryPaths.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Carpetas añadidas:",
                        style = MeloType.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MeloColors.textPrimary
                    )
                    settings.localLibraryPaths.forEach { path ->
                        Surface(
                            color = MeloColors.surface2,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = path,
                                    style = MeloType.labelSmall,
                                    color = MeloColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onRemoveLocalPath(path) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Eliminar",
                                        tint = MeloColors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

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
                        onClick = onLogout,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar sesión")
                    }
                } else {
                    Button(
                        onClick = onOpenLogin,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Iniciar sesión")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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