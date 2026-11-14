@file:Suppress(
    "FunctionNaming",
    "LongMethod",
    "LongParameterList",
    "TooManyFunctions",
    "ktlint:standard:function-naming",
)

package com.github.adriianh.melo.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.AudioQuality
import com.github.adriianh.core.domain.model.CacheSizeLimit
import com.github.adriianh.core.domain.model.DownloadFormat
import com.github.adriianh.core.domain.model.DownloadQuality
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.ThemePreset
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.melo.ui.components.LocalFolderPathsEditor
import com.github.adriianh.melo.ui.components.SegmentedControl
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import com.github.adriianh.melo.util.getPlatform
import org.koin.compose.viewmodel.koinViewModel

private val EXPANDED_SETTINGS_BREAKPOINT = 540.dp

@Composable
internal fun SettingsContent(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: String?,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    initialCategory: SettingsCategory? = null,
    selectedCategoryName: String? = null,
    onSelectedCategoryChange: ((String?) -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    updateState: UpdateState? = null,
) {
    val platform = remember { getPlatform() }
    val categories = remember(platform.type) { SettingsCategory.visibleFor(platform.type) }
    var internalSelectedCategoryName by rememberSaveable { mutableStateOf(initialCategory?.name) }
    val activeCategoryName =
        if (onSelectedCategoryChange != null) {
            selectedCategoryName ?: initialCategory?.name
        } else {
            internalSelectedCategoryName
        }
    val setSelectedCategoryName: (String?) -> Unit = { nextName ->
        if (onSelectedCategoryChange != null) {
            onSelectedCategoryChange(nextName)
        } else {
            internalSelectedCategoryName = nextName
        }
    }
    val selectedCategory = categories.find { it.name == activeCategoryName }

    val resolvedUpdateState: UpdateState? =
        if (updateState != null) {
            updateState
        } else {
            val viewModel: UpdateViewModel = koinViewModel()
            val vmState by viewModel.uiState.collectAsState()
            vmState
        }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= EXPANDED_SETTINGS_BREAKPOINT

        if (isExpanded) {
            val activeCategory = selectedCategory ?: categories.first()
            Row(modifier = Modifier.fillMaxSize()) {
                SettingsSidebar(
                    categories = categories,
                    selectedCategory = activeCategory,
                    updateState = resolvedUpdateState,
                    onSelectCategory = { setSelectedCategoryName(it.name) },
                )

                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MeloColors.border),
                )

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    SettingsExpandedDetailHeader(
                        category = activeCategory,
                        onClose = onClose,
                    )

                    val detailScrollState = rememberScrollState()
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(
                                    state = detailScrollState,
                                    enabled = detailScrollState.maxValue > 0,
                                ).padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        SettingsCategoryDetailPane(
                            category = activeCategory,
                            settings = settings,
                            isLoggedIn = isLoggedIn,
                            profiles = profiles,
                            activePageId = activePageId,
                            isLoadingProfiles = isLoadingProfiles,
                            isSwitchingProfile = isSwitchingProfile,
                            profilesError = profilesError,
                            actions = actions,
                            platformType = platform.type,
                            previewUpdateState = updateState,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        } else {
            SettingsBackHandler(enabled = selectedCategory != null) {
                setSelectedCategoryName(null)
            }

            AnimatedContent(
                targetState = selectedCategory,
                label = "SettingsCategoryTransition",
                modifier = Modifier.fillMaxSize(),
            ) { currentCategory ->
                if (currentCategory == null) {
                    SettingsCategoryList(
                        categories = categories,
                        settings = settings,
                        isLoggedIn = isLoggedIn,
                        updateState = resolvedUpdateState,
                        onSelectCategory = { setSelectedCategoryName(it.name) },
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SettingsCompactDetailHeader(
                            category = currentCategory,
                            onBack = { setSelectedCategoryName(null) },
                        )

                        val detailScrollState = rememberScrollState()
                        Column(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(
                                        state = detailScrollState,
                                        enabled = detailScrollState.maxValue > 0,
                                    ),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            SettingsCategoryDetailPane(
                                category = currentCategory,
                                settings = settings,
                                isLoggedIn = isLoggedIn,
                                profiles = profiles,
                                activePageId = activePageId,
                                isLoadingProfiles = isLoadingProfiles,
                                isSwitchingProfile = isSwitchingProfile,
                                profilesError = profilesError,
                                actions = actions,
                                platformType = platform.type,
                                previewUpdateState = updateState,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsExpandedDetailHeader(
    category: SettingsCategory,
    onClose: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = category.title,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MeloColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onClose != null) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MeloColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun SettingsCompactDetailHeader(
    category: SettingsCategory,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver a categorías",
                tint = MeloColors.textPrimary,
            )
        }
        Text(
            text = category.title,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MeloColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SettingsCategoryDetailPane(
    category: SettingsCategory,
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: String?,
    actions: SettingsActions,
    platformType: PlatformType,
    previewUpdateState: UpdateState?,
) {
    when (category) {
        SettingsCategory.APPEARANCE -> AppearanceSettingsSection(settings, actions)
        SettingsCategory.PLAYBACK -> PlaybackSettingsSection(settings, actions)
        SettingsCategory.LIBRARY -> {
            DownloadsSettingsSection(settings, actions)
            LocalFoldersSection(settings, actions)
        }
        SettingsCategory.ACCOUNT -> {
            AccountSettingsSection(
                settings = settings,
                isLoggedIn = isLoggedIn,
                profiles = profiles,
                activePageId = activePageId,
                isLoadingProfiles = isLoadingProfiles,
                isSwitchingProfile = isSwitchingProfile,
                profilesError = profilesError,
                actions = actions,
            )
        }
        SettingsCategory.INTEGRATIONS -> {
            if (platformType != PlatformType.IOS) {
                DiscordRpcSettingsSection(settings, actions, platformType)
            }
        }
        SettingsCategory.UPDATES -> {
            if (previewUpdateState != null) {
                UpdateSettingsSectionContent(
                    state = previewUpdateState,
                    autoCheckUpdates = settings.autoCheckUpdates,
                    updateChannel = settings.updateChannel,
                    onToggleAutoCheckUpdates = actions.onToggleAutoCheckUpdates,
                    onUpdateChannelSelected = actions.onUpdateChannelSelected,
                    onCheckForUpdates = {},
                    onStartDownload = { _, _ -> },
                    onCancelDownload = {},
                    onInstallUpdate = {},
                )
            } else {
                UpdateSettingsSection(
                    autoCheckUpdates = settings.autoCheckUpdates,
                    onToggleAutoCheckUpdates = actions.onToggleAutoCheckUpdates,
                    updateChannel = settings.updateChannel,
                    onUpdateChannelSelected = actions.onUpdateChannelSelected,
                )
            }
        }
    }
}

@Composable
internal fun AppearanceSettingsSection(
    settings: Settings,
    actions: SettingsActions,
) {
    SettingsSection(caption = "Apariencia y tema") {
        SettingInlineChoiceRow(
            title = "Modo de tema",
            subtitle = "Sigue la configuración del sistema o fuerza claro/oscuro.",
        ) { isInline ->
            SegmentedControl(
                options = ThemeMode.entries,
                selected = settings.themeMode,
                onSelect = actions.onThemeModeSelected,
                label = { it.uiLabel() },
                fillMaxWidth = !isInline,
            )
        }

        SettingsRowDivider()

        SettingToggleRow(
            title = "Color dinámico",
            subtitle = "Armoniza la interfaz con los colores de la carátula actual.",
            checked = settings.dynamicColor,
            onCheckedChange = actions.onDynamicColorToggle,
        )

        SettingsRowDivider()

        val paletteEnabled = !settings.dynamicColor
        SettingChoiceRow(
            title = "Paleta de color",
            subtitle = "Selecciona una paleta de acento fija para la interfaz.",
            enabled = paletteEnabled,
            disabledNote = "Controlado por Color dinámico",
        ) {
            SettingOptionGrid(
                options = ThemePreset.entries,
                isSelected = { it == settings.theme },
                onSelect = actions.onThemePresetSelected,
                title = { it.displayName },
                enabled = paletteEnabled,
            )
        }
    }
}

@Composable
internal fun PlaybackSettingsSection(
    settings: Settings,
    actions: SettingsActions,
) {
    SettingsSection(caption = "Reproducción y streaming") {
        SettingToggleRow(
            title = "Ahorro de datos",
            subtitle = "Prioriza bitrates más bajos para minimizar el consumo móvil.",
            checked = settings.dataSaver,
            onCheckedChange = actions.onDataSaverToggle,
        )

        SettingsRowDivider()

        val streamingQualityEnabled = !settings.dataSaver
        SettingChoiceRow(
            title = "Calidad de streaming",
            subtitle = "Tasa de bits para la reproducción online.",
            enabled = streamingQualityEnabled,
            disabledNote = "Controlado por Ahorro de datos",
        ) {
            SettingOptionGrid(
                options = AudioQuality.entries,
                isSelected = { it == settings.audioQuality },
                onSelect = actions.onAudioQualitySelected,
                title = { it.uiTitle() },
                hint = { it.uiHint() },
                enabled = streamingQualityEnabled,
            )
        }

        SettingsRowDivider()

        SettingToggleRow(
            title = "Reproducción continua",
            subtitle = "Añade canciones similares al finalizar la cola.",
            checked = settings.autoplay,
            onCheckedChange = actions.onAutoplayToggle,
        )

        SettingsRowDivider()

        SettingToggleRow(
            title = "Reanudar la última sesión activa",
            subtitle = "Al arrancar, recupera la cola más reciente aunque pertenezca a otra instancia de Melo.",
            checked = settings.resumeLastActiveSession,
            onCheckedChange = actions.onResumeLastActiveSessionToggle,
        )
    }
}

@Composable
internal fun DownloadsSettingsSection(
    settings: Settings,
    actions: SettingsActions,
) {
    SettingsSection(caption = "Descargas y caché offline") {
        SettingToggleRow(
            title = "Modo sin conexión",
            subtitle = "Bloquea peticiones de red y prioriza únicamente contenido descargado y local.",
            checked = settings.offlineMode,
            onCheckedChange = actions.onOfflineModeToggle,
        )

        SettingsRowDivider()

        SettingChoiceRow(
            title = "Límite de tamaño de caché",
            subtitle = "Espacio máximo para canciones escuchadas recientemente (limpieza LRU automática).",
        ) {
            SettingOptionGrid(
                options = CacheSizeLimit.entries,
                isSelected = { it.sizeMb == settings.maxOfflineSizeMb },
                onSelect = actions.onCacheSizeLimitSelected,
                title = { it.displayName },
                hint = { it.uiHint() },
            )
        }

        SettingsRowDivider()

        SettingChoiceRow(
            title = "Formato de descarga",
            subtitle = "Extensión y contenedor preferido para guardar pistas.",
        ) {
            SettingOptionGrid(
                options = DownloadFormat.entries,
                isSelected = { it == settings.downloadFormat },
                onSelect = actions.onDownloadFormatSelected,
                title = { it.name },
                hint = { ".${it.displayName}" },
            )
        }

        SettingsRowDivider()

        SettingChoiceRow(
            title = "Calidad de descarga",
            subtitle = "Tasa de bits máxima al descargar.",
        ) {
            SettingOptionGrid(
                options = DownloadQuality.entries,
                isSelected = { it == settings.downloadQuality },
                onSelect = actions.onDownloadQualitySelected,
                title = { it.name },
                hint = { it.displayName },
            )
        }

        SettingsRowDivider()

        var showClearCacheConfirm by rememberSaveable { mutableStateOf(false) }

        if (showClearCacheConfirm) {
            SettingsConfirmDialog(
                title = "Limpiar caché de audio",
                message =
                    "¿Estás seguro de que deseas eliminar las pistas almacenadas temporalmente en caché? " +
                        "Tus canciones descargadas y archivos locales no se verán afectados.",
                confirmText = "Limpiar caché",
                icon = Icons.Default.DeleteSweep,
                onConfirm = {
                    showClearCacheConfirm = false
                    actions.onClearCache()
                },
                onDismiss = { showClearCacheConfirm = false },
            )
        }

        SettingActionRow(
            title = "Limpiar caché de audio",
            subtitle = "Libera espacio eliminando las pistas cacheadas temporalmente.",
        ) {
            SettingActionButton(
                text = "Limpiar caché",
                onClick = { showClearCacheConfirm = true },
                emphasis = SettingActionEmphasis.DESTRUCTIVE,
                icon = Icons.Default.DeleteSweep,
                modifier = Modifier.actionWidth(),
            )
        }
    }
}

@Composable
internal fun LocalFoldersSection(
    settings: Settings,
    actions: SettingsActions,
    initialManualExpanded: Boolean = false,
) {
    SettingsSection(caption = "Carpetas de música local") {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Configura carpetas de tu dispositivo para indexar canciones locales.",
                style = MeloType.labelSmall,
                color = MeloColors.textSecondary,
            )

            LocalFolderPathsEditor(
                configuredPaths = settings.localLibraryPaths,
                onAddPath = actions.onAddLocalPath,
                onRemovePath = actions.onRemoveLocalPath,
                initialManualExpanded = initialManualExpanded,
            )
        }
    }
}

@Composable
internal fun AccountSettingsSection(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: String?,
    actions: SettingsActions,
) {
    SettingsSection(caption = "Cuenta y sesión") {
        AccountSessionHeader(
            isLoggedIn = isLoggedIn,
            onLogout = actions.onLogout,
            onOpenLogin = actions.onOpenLogin,
        )

        if (isLoggedIn) {
            SettingsRowDivider()
            AccountProfilesList(
                profiles = profiles,
                activePageId = activePageId,
                isLoading = isLoadingProfiles,
                isSwitching = isSwitchingProfile,
                error = profilesError,
                onSelect = actions.onSelectAccountProfile,
                onRefresh = actions.onRefreshAccountProfiles,
            )
        }

        SettingsRowDivider()

        SettingToggleRow(
            title = "Registrar reproducciones en YouTube",
            subtitle = "Sincroniza automáticamente las pistas que escuchas con tu historial de YouTube Music.",
            checked = settings.syncHistoryToYouTube,
            onCheckedChange = actions.onSyncHistoryToYouTubeChanged,
            enabled = isLoggedIn,
            disabledNote = "Inicia sesión para activarlo",
        )
    }
}

@Composable
private fun AccountSessionHeader(
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
    onOpenLogin: () -> Unit,
) {
    var showLogoutConfirm by rememberSaveable { mutableStateOf(false) }

    if (showLogoutConfirm) {
        SettingsConfirmDialog(
            title = "Cerrar sesión",
            message =
                "¿Estás seguro de que deseas cerrar tu sesión de YouTube Music? " +
                    "Dejarás de sincronizar tu biblioteca, listas y recomendaciones " +
                    "hasta que vuelvas a iniciar sesión.",
            confirmText = "Cerrar sesión",
            icon = Icons.AutoMirrored.Filled.Logout,
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            },
            onDismiss = { showLogoutConfirm = false },
        )
    }

    SettingActionRow(
        title = if (isLoggedIn) "Sesión iniciada con YouTube Music" else "Sin conexión de cuenta",
        subtitle =
            if (isLoggedIn) {
                "Tus playlists, likes y biblioteca están sincronizados."
            } else {
                "Inicia sesión para sincronizar tus playlists y favoritos."
            },
    ) {
        if (isLoggedIn) {
            SettingActionButton(
                text = "Cerrar sesión",
                onClick = { showLogoutConfirm = true },
                emphasis = SettingActionEmphasis.DESTRUCTIVE,
                icon = Icons.AutoMirrored.Filled.Logout,
                modifier = Modifier.actionWidth(),
            )
        } else {
            SettingActionButton(
                text = "Iniciar sesión",
                onClick = onOpenLogin,
                emphasis = SettingActionEmphasis.PRIMARY,
                icon = Icons.AutoMirrored.Filled.Login,
                modifier = Modifier.actionWidth(),
            )
        }
    }
}
