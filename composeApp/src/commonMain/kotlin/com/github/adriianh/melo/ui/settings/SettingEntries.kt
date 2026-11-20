@file:Suppress(
    "FunctionNaming",
    "LongMethod",
    "LongParameterList",
    "TooManyFunctions",
    "ktlint:standard:function-naming",
)

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.AccountProfile
import com.github.adriianh.core.domain.model.AudioQuality
import com.github.adriianh.core.domain.model.CacheSizeLimit
import com.github.adriianh.core.domain.model.DownloadFormat
import com.github.adriianh.core.domain.model.DownloadQuality
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.ThemePreset
import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.ReleaseAsset
import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.core.util.MeloVersion
import com.github.adriianh.melo.ui.components.LocalFolderPathsEditor
import com.github.adriianh.melo.ui.components.SegmentedControl
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType

internal enum class SettingSearchDisplay {
    INLINE,
    LINK,
}

internal data class SettingEntry(
    val id: String,
    val category: SettingsCategory,
    val section: String?,
    val title: String,
    val description: String? = null,
    val keywords: List<String> = emptyList(),
    val visible: Boolean = true,
    val searchDisplay: SettingSearchDisplay = SettingSearchDisplay.INLINE,
    val content: @Composable () -> Unit,
)

@Composable
internal fun rememberSettingEntries(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: String?,
    actions: SettingsActions,
    platformType: PlatformType,
    updateState: UpdateState,
    onCheckForUpdates: (UpdateChannel) -> Unit,
    onUpdateChannelSelected: (UpdateChannel) -> Unit,
    onStartUpdateDownload: (AppRelease, ReleaseAsset) -> Unit,
    onCancelUpdateDownload: () -> Unit,
    onInstallUpdate: (String) -> Unit,
    currentVersion: String = MeloVersion.CURRENT,
    initialManualFolderExpanded: Boolean = false,
): List<SettingEntry> =
    remember(
        settings,
        isLoggedIn,
        profiles,
        activePageId,
        isLoadingProfiles,
        isSwitchingProfile,
        profilesError,
        actions,
        platformType,
        updateState,
        onCheckForUpdates,
        onUpdateChannelSelected,
        onStartUpdateDownload,
        onCancelUpdateDownload,
        onInstallUpdate,
        currentVersion,
        initialManualFolderExpanded,
    ) {
        buildList {
            addAppearanceEntries(settings = settings, actions = actions)
            addPlaybackEntries(
                settings = settings,
                actions = actions,
                platformType = platformType,
            )
            addLibraryEntries(
                settings = settings,
                actions = actions,
                initialManualFolderExpanded = initialManualFolderExpanded,
            )
            addAccountEntries(
                settings = settings,
                isLoggedIn = isLoggedIn,
                profiles = profiles,
                activePageId = activePageId,
                isLoadingProfiles = isLoadingProfiles,
                isSwitchingProfile = isSwitchingProfile,
                profilesError = profilesError,
                actions = actions,
            )
            addIntegrationsEntries(
                settings = settings,
                actions = actions,
                platformType = platformType,
            )
            addUpdateEntries(
                autoCheckUpdates = settings.autoCheckUpdates,
                updateChannel = settings.updateChannel,
                updateState = updateState,
                currentVersion = currentVersion,
                onToggleAutoCheckUpdates = actions.onToggleAutoCheckUpdates,
                onUpdateChannelSelected = onUpdateChannelSelected,
                onCheckForUpdates = onCheckForUpdates,
                onStartDownload = onStartUpdateDownload,
                onCancelDownload = onCancelUpdateDownload,
                onInstallUpdate = onInstallUpdate,
            )
        }
    }

@Composable
internal fun rememberUpdateSettingEntries(
    autoCheckUpdates: Boolean,
    updateChannel: UpdateChannel,
    updateState: UpdateState,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onUpdateChannelSelected: (UpdateChannel) -> Unit,
    onCheckForUpdates: (UpdateChannel) -> Unit,
    onStartDownload: (AppRelease, ReleaseAsset) -> Unit,
    onCancelDownload: () -> Unit,
    onInstallUpdate: (String) -> Unit,
    currentVersion: String = MeloVersion.CURRENT,
): List<SettingEntry> =
    remember(
        autoCheckUpdates,
        updateChannel,
        updateState,
        onToggleAutoCheckUpdates,
        onUpdateChannelSelected,
        onCheckForUpdates,
        onStartDownload,
        onCancelDownload,
        onInstallUpdate,
        currentVersion,
    ) {
        buildList {
            addUpdateEntries(
                autoCheckUpdates = autoCheckUpdates,
                updateChannel = updateChannel,
                updateState = updateState,
                currentVersion = currentVersion,
                onToggleAutoCheckUpdates = onToggleAutoCheckUpdates,
                onUpdateChannelSelected = onUpdateChannelSelected,
                onCheckForUpdates = onCheckForUpdates,
                onStartDownload = onStartDownload,
                onCancelDownload = onCancelDownload,
                onInstallUpdate = onInstallUpdate,
            )
        }
    }

@Composable
internal fun SettingsCategoryEntriesPane(
    entries: List<SettingEntry>,
    category: SettingsCategory,
    modifier: Modifier = Modifier,
) {
    val visibleEntries =
        remember(entries, category) {
            entries.filter { it.visible && it.category == category }
        }
    SettingsEntriesSections(entries = visibleEntries, modifier = modifier)
}

@Composable
internal fun SettingsEntriesSections(
    entries: List<SettingEntry>,
    modifier: Modifier = Modifier,
) {
    val sections = remember(entries) { entries.groupBy { it.section } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        sections.forEach { (sectionCaption, sectionEntries) ->
            if (sectionCaption != null) {
                SettingsSection(caption = sectionCaption) {
                    sectionEntries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            SettingsRowDivider()
                        }
                        entry.content()
                    }
                }
            } else {
                sectionEntries.forEach { entry ->
                    entry.content()
                }
            }
        }
    }
}

private fun MutableList<SettingEntry>.addAppearanceEntries(
    settings: Settings,
    actions: SettingsActions,
) {
    val section = "Apariencia y tema"
    add(
        SettingEntry(
            id = "appearance_theme_mode",
            category = SettingsCategory.APPEARANCE,
            section = section,
            title = "Modo de tema",
            description = "Sigue la configuración del sistema o fuerza claro/oscuro.",
            keywords =
                listOf(
                    "tema",
                    "theme",
                    "oscuro",
                    "dark",
                    "claro",
                    "light",
                    "sistema",
                    "system",
                    "modo",
                    "mode",
                ),
        ) {
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
        },
    )
    add(
        SettingEntry(
            id = "appearance_dynamic_color",
            category = SettingsCategory.APPEARANCE,
            section = section,
            title = "Color dinámico",
            description = "Armoniza la interfaz con los colores de la carátula actual.",
            keywords =
                listOf(
                    "color",
                    "dinámico",
                    "dynamic",
                    "carátula",
                    "cover",
                    "artwork",
                    "acento",
                    "accent",
                    "material you",
                ),
        ) {
            SettingToggleRow(
                title = "Color dinámico",
                subtitle = "Armoniza la interfaz con los colores de la carátula actual.",
                checked = settings.dynamicColor,
                onCheckedChange = actions.onDynamicColorToggle,
            )
        },
    )
    add(
        SettingEntry(
            id = "appearance_theme_preset",
            category = SettingsCategory.APPEARANCE,
            section = section,
            title = "Paleta de color",
            description = "Selecciona una paleta de acento fija para la interfaz.",
            keywords =
                listOf(
                    "paleta",
                    "palette",
                    "color",
                    "tema",
                    "theme",
                    "preset",
                    "catppuccin",
                    "mocha",
                    "gruvbox",
                    "nord",
                    "tokyo night",
                    "acento",
                ),
        ) {
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
        },
    )

    val effectsSection = "Efectos y rendimiento"
    add(
        SettingEntry(
            id = "appearance_ambient_motion",
            category = SettingsCategory.APPEARANCE,
            section = effectsSection,
            title = "Fondo ambiental animado",
            description = "Mueve suavemente el fondo durante la reproducción (usa más GPU).",
            keywords =
                listOf(
                    "fondo",
                    "ambient",
                    "animación",
                    "animation",
                    "movimiento",
                    "motion",
                    "gpu",
                    "rendimiento",
                    "performance",
                    "canvas",
                ),
        ) {
            SettingToggleRow(
                title = "Fondo ambiental animado",
                subtitle = "Mueve suavemente el fondo durante la reproducción (usa más GPU).",
                checked = settings.ambientMotion,
                onCheckedChange = actions.onAmbientMotionToggle,
            )
        },
    )
    add(
        SettingEntry(
            id = "appearance_now_playing_blur",
            category = SettingsCategory.APPEARANCE,
            section = effectsSection,
            title = "Desenfoque en Reproduciendo",
            description = "Muestra la carátula difuminada de fondo en pantalla completa (usa más GPU).",
            keywords =
                listOf(
                    "blur",
                    "desenfoque",
                    "fondo",
                    "reproduciendo",
                    "now playing",
                    "carátula",
                    "gpu",
                    "rendimiento",
                    "performance",
                    "efectos",
                ),
        ) {
            SettingToggleRow(
                title = "Desenfoque en Reproduciendo",
                subtitle = "Muestra la carátula difuminada de fondo en pantalla completa (usa más GPU).",
                checked = settings.nowPlayingBlur,
                onCheckedChange = actions.onNowPlayingBlurToggle,
            )
        },
    )
}

private fun MutableList<SettingEntry>.addPlaybackEntries(
    settings: Settings,
    actions: SettingsActions,
    platformType: PlatformType,
) {
    val section = "Reproducción y streaming"
    val stopOnCloseDescription =
        "Al quitar Melo de recientes, pausa la reproducción y cierra el servicio en segundo plano. " +
            "Puedes reanudar al volver a abrir la app."
    add(
        SettingEntry(
            id = "playback_data_saver",
            category = SettingsCategory.PLAYBACK,
            section = section,
            title = "Ahorro de datos",
            description = "Prioriza bitrates más bajos para minimizar el consumo móvil.",
            keywords =
                listOf(
                    "ahorro",
                    "datos",
                    "data saver",
                    "móvil",
                    "mobile",
                    "bitrate",
                    "consumo",
                    "network",
                ),
        ) {
            SettingToggleRow(
                title = "Ahorro de datos",
                subtitle = "Prioriza bitrates más bajos para minimizar el consumo móvil.",
                checked = settings.dataSaver,
                onCheckedChange = actions.onDataSaverToggle,
            )
        },
    )
    add(
        SettingEntry(
            id = "playback_audio_quality",
            category = SettingsCategory.PLAYBACK,
            section = section,
            title = "Calidad de streaming",
            description = "Tasa de bits para la reproducción online.",
            keywords =
                listOf(
                    "calidad",
                    "quality",
                    "audio",
                    "streaming",
                    "bitrate",
                    "kbps",
                    "sonido",
                    "sound",
                ),
        ) {
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
        },
    )
    add(
        SettingEntry(
            id = "playback_autoplay",
            category = SettingsCategory.PLAYBACK,
            section = section,
            title = "Reproducción continua",
            description = "Añade canciones similares al finalizar la cola.",
            keywords =
                listOf(
                    "reproducción",
                    "continua",
                    "autoplay",
                    "cola",
                    "queue",
                    "radio",
                    "similares",
                    "playback",
                ),
        ) {
            SettingToggleRow(
                title = "Reproducción continua",
                subtitle = "Añade canciones similares al finalizar la cola.",
                checked = settings.autoplay,
                onCheckedChange = actions.onAutoplayToggle,
            )
        },
    )
    add(
        SettingEntry(
            id = "playback_resume_last_session",
            category = SettingsCategory.PLAYBACK,
            section = section,
            title = "Reanudar la última sesión activa",
            description = "Al arrancar, recupera la cola más reciente aunque pertenezca a otra instancia de Melo.",
            keywords =
                listOf(
                    "reanudar",
                    "resume",
                    "sesión",
                    "session",
                    "cola",
                    "queue",
                    "arranque",
                    "startup",
                    "última",
                ),
        ) {
            SettingToggleRow(
                title = "Reanudar la última sesión activa",
                subtitle = "Al arrancar, recupera la cola más reciente aunque pertenezca a otra instancia de Melo.",
                checked = settings.resumeLastActiveSession,
                onCheckedChange = actions.onResumeLastActiveSessionToggle,
            )
        },
    )
    add(
        SettingEntry(
            id = "playback_stop_on_task_removal",
            category = SettingsCategory.PLAYBACK,
            section = section,
            title = "Detener al cerrar la app",
            description = stopOnCloseDescription,
            keywords =
                listOf(
                    "cerrar",
                    "close",
                    "recientes",
                    "recents",
                    "deslizar",
                    "swipe",
                    "segundo plano",
                    "background",
                    "notificación",
                    "notification",
                    "servicio",
                    "service",
                    "detener",
                    "stop",
                ),
            visible = platformType == PlatformType.ANDROID,
        ) {
            SettingToggleRow(
                title = "Detener al cerrar la app",
                subtitle = stopOnCloseDescription,
                checked = settings.stopPlaybackOnTaskRemoval,
                onCheckedChange = actions.onStopPlaybackOnTaskRemovalToggle,
            )
        },
    )
}

private fun MutableList<SettingEntry>.addLibraryEntries(
    settings: Settings,
    actions: SettingsActions,
    initialManualFolderExpanded: Boolean,
) {
    val downloadsSection = "Descargas y caché offline"
    add(
        SettingEntry(
            id = "library_offline_mode",
            category = SettingsCategory.LIBRARY,
            section = downloadsSection,
            title = "Modo sin conexión",
            description = "Bloquea peticiones de red y prioriza únicamente contenido descargado y local.",
            keywords =
                listOf(
                    "sin conexión",
                    "offline",
                    "red",
                    "network",
                    "descargado",
                    "local",
                    "avión",
                    "airplane",
                ),
        ) {
            SettingToggleRow(
                title = "Modo sin conexión",
                subtitle = "Bloquea peticiones de red y prioriza únicamente contenido descargado y local.",
                checked = settings.offlineMode,
                onCheckedChange = actions.onOfflineModeToggle,
            )
        },
    )
    add(
        SettingEntry(
            id = "library_cache_size",
            category = SettingsCategory.LIBRARY,
            section = downloadsSection,
            title = "Límite de tamaño de caché",
            description = "Espacio máximo para canciones escuchadas recientemente (limpieza LRU automática).",
            keywords =
                listOf(
                    "caché",
                    "cache",
                    "tamaño",
                    "size",
                    "límite",
                    "limit",
                    "almacenamiento",
                    "storage",
                    "espacio",
                ),
        ) {
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
        },
    )
    add(
        SettingEntry(
            id = "library_download_format",
            category = SettingsCategory.LIBRARY,
            section = downloadsSection,
            title = "Formato de descarga",
            description = "Extensión y contenedor preferido para guardar pistas.",
            keywords =
                listOf(
                    "formato",
                    "format",
                    "descarga",
                    "download",
                    "mp3",
                    "opus",
                    "flac",
                    "aac",
                    "m4a",
                    "ogg",
                    "wav",
                ),
        ) {
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
        },
    )
    add(
        SettingEntry(
            id = "library_download_quality",
            category = SettingsCategory.LIBRARY,
            section = downloadsSection,
            title = "Calidad de descarga",
            description = "Tasa de bits máxima al descargar.",
            keywords =
                listOf(
                    "calidad",
                    "quality",
                    "descarga",
                    "download",
                    "bitrate",
                    "kbps",
                    "audio",
                ),
        ) {
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
        },
    )
    add(
        SettingEntry(
            id = "library_clear_cache",
            category = SettingsCategory.LIBRARY,
            section = downloadsSection,
            title = "Limpiar caché de audio",
            description = "Libera espacio eliminando las pistas cacheadas temporalmente.",
            keywords =
                listOf(
                    "limpiar",
                    "clear",
                    "caché",
                    "cache",
                    "borrar",
                    "delete",
                    "espacio",
                    "space",
                ),
        ) {
            SettingActionRow(
                title = "Limpiar caché de audio",
                subtitle = "Libera espacio eliminando las pistas cacheadas temporalmente.",
            ) {
                SettingActionButton(
                    text = "Limpiar caché",
                    onClick = actions.onClearCache,
                    emphasis = SettingActionEmphasis.DESTRUCTIVE,
                    icon = Icons.Default.DeleteSweep,
                    modifier = Modifier.actionWidth(),
                )
            }
        },
    )
    add(
        SettingEntry(
            id = "library_local_folders",
            category = SettingsCategory.LIBRARY,
            section = "Carpetas de música local",
            title = "Carpetas de música local",
            description = "Configura carpetas de tu dispositivo para indexar canciones locales.",
            keywords =
                listOf(
                    "carpetas",
                    "folders",
                    "local",
                    "música",
                    "music",
                    "ruta",
                    "path",
                    "directorio",
                    "directory",
                    "indexar",
                ),
            searchDisplay = SettingSearchDisplay.LINK,
        ) {
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
                    initialManualExpanded = initialManualFolderExpanded,
                )
            }
        },
    )
}

private fun MutableList<SettingEntry>.addAccountEntries(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: String?,
    actions: SettingsActions,
) {
    val section = "Cuenta y sesión"
    val sessionTitle =
        if (isLoggedIn) {
            "Sesión iniciada con YouTube Music"
        } else {
            "Sin conexión de cuenta"
        }
    val sessionDescription =
        if (isLoggedIn) {
            "Tus playlists, likes y biblioteca están sincronizados."
        } else {
            "Inicia sesión para sincronizar tus playlists y favoritos."
        }
    add(
        SettingEntry(
            id = "account_session",
            category = SettingsCategory.ACCOUNT,
            section = section,
            title = sessionTitle,
            description = sessionDescription,
            keywords =
                listOf(
                    "cuenta",
                    "account",
                    "sesión",
                    "session",
                    "iniciar sesión",
                    "cerrar sesión",
                    "login",
                    "logout",
                    "youtube music",
                ),
        ) {
            AccountSessionEntryRow(
                isLoggedIn = isLoggedIn,
                onLogout = actions.onLogout,
                onOpenLogin = actions.onOpenLogin,
            )
        },
    )
    add(
        SettingEntry(
            id = "account_profiles",
            category = SettingsCategory.ACCOUNT,
            section = section,
            title = "Canales de la cuenta",
            description = "Cambia el canal activo para sincronizar su biblioteca, likes y playlists.",
            keywords =
                listOf(
                    "canales",
                    "channels",
                    "perfil",
                    "profile",
                    "cuentas",
                    "accounts",
                    "cambiar",
                    "switch",
                    "youtube",
                ),
            visible = isLoggedIn,
            searchDisplay = SettingSearchDisplay.LINK,
        ) {
            AccountProfilesList(
                profiles = profiles,
                activePageId = activePageId,
                isLoading = isLoadingProfiles,
                isSwitching = isSwitchingProfile,
                error = profilesError,
                onSelect = actions.onSelectAccountProfile,
                onRefresh = actions.onRefreshAccountProfiles,
            )
        },
    )
    add(
        SettingEntry(
            id = "account_sync_history",
            category = SettingsCategory.ACCOUNT,
            section = section,
            title = "Registrar reproducciones en YouTube",
            description = "Sincroniza automáticamente las pistas que escuchas con tu historial de YouTube Music.",
            keywords =
                listOf(
                    "historial",
                    "history",
                    "sincronizar",
                    "sync",
                    "youtube",
                    "reproducciones",
                    "scrobble",
                    "registrar",
                ),
        ) {
            SettingToggleRow(
                title = "Registrar reproducciones en YouTube",
                subtitle = "Sincroniza automáticamente las pistas que escuchas con tu historial de YouTube Music.",
                checked = settings.syncHistoryToYouTube,
                onCheckedChange = actions.onSyncHistoryToYouTubeChanged,
                enabled = isLoggedIn,
                disabledNote = "Inicia sesión para activarlo",
            )
        },
    )
}

private fun MutableList<SettingEntry>.addIntegrationsEntries(
    settings: Settings,
    actions: SettingsActions,
    platformType: PlatformType,
) {
    val section = "Discord Rich Presence"
    val isVisible = platformType != PlatformType.IOS
    val isLinked = !settings.discordRpcToken.isNullOrBlank()
    val accountDescription =
        if (isLinked) {
            "Cuenta vinculada con token de sesión activo."
        } else {
            "Requiere inicio de sesión para conectar con el Gateway de Discord."
        }
    val rpcDescription =
        if (platformType == PlatformType.ANDROID) {
            "Muestra en tiempo real tu música en reproducción usando la API de Discord."
        } else {
            "Muestra en tiempo real tu música en reproducción en tu estado de Discord Desktop."
        }

    add(
        SettingEntry(
            id = "integrations_discord_account",
            category = SettingsCategory.INTEGRATIONS,
            section = section,
            title = "Estado de la cuenta",
            description = accountDescription,
            keywords =
                listOf(
                    "discord",
                    "cuenta",
                    "account",
                    "vincular",
                    "link",
                    "desvincular",
                    "unlink",
                    "token",
                    "rpc",
                    "rich presence",
                ),
            visible = isVisible && platformType == PlatformType.ANDROID,
        ) {
            DiscordRpcAccountEntry(
                token = settings.discordRpcToken,
                onTokenChanged = actions.onDiscordTokenChanged,
            )
        },
    )
    add(
        SettingEntry(
            id = "integrations_discord_rpc",
            category = SettingsCategory.INTEGRATIONS,
            section = section,
            title = "Mostrar actividad en Discord",
            description = rpcDescription,
            keywords =
                listOf(
                    "discord",
                    "rich presence",
                    "rpc",
                    "actividad",
                    "activity",
                    "estado",
                    "status",
                    "integraciones",
                    "integrations",
                ),
            visible = isVisible,
        ) {
            SettingToggleRow(
                title = "Mostrar actividad en Discord",
                subtitle = rpcDescription,
                checked = settings.discordRpcEnabled,
                onCheckedChange = actions.onDiscordRpcToggle,
                enabled = platformType != PlatformType.ANDROID || isLinked,
                disabledNote = "Vincula tu cuenta para activarlo",
            )
        },
    )
}

private fun MutableList<SettingEntry>.addUpdateEntries(
    autoCheckUpdates: Boolean,
    updateChannel: UpdateChannel,
    updateState: UpdateState,
    currentVersion: String,
    onToggleAutoCheckUpdates: (Boolean) -> Unit,
    onUpdateChannelSelected: (UpdateChannel) -> Unit,
    onCheckForUpdates: (UpdateChannel) -> Unit,
    onStartDownload: (AppRelease, ReleaseAsset) -> Unit,
    onCancelDownload: () -> Unit,
    onInstallUpdate: (String) -> Unit,
) {
    add(
        SettingEntry(
            id = "updates_installed_version",
            category = SettingsCategory.UPDATES,
            section = "Versión",
            title = "Versión instalada",
            description = "v$currentVersion",
            keywords =
                listOf(
                    "versión",
                    "version",
                    "instalada",
                    "installed",
                    "actualización",
                    "update",
                    "buscar",
                    "check",
                    "nightly",
                    "estable",
                    "stable",
                ),
        ) {
            UpdateVersionHeaderRow(
                currentVersion = currentVersion,
                canCheck = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading,
                onCheckForUpdates = { onCheckForUpdates(updateChannel) },
            )
        },
    )
    add(
        SettingEntry(
            id = "updates_auto_check",
            category = SettingsCategory.UPDATES,
            section = "Versión",
            title = "Buscar automáticamente",
            description = "Comprueba si hay nuevas versiones al abrir la app.",
            keywords =
                listOf(
                    "buscar",
                    "automáticamente",
                    "auto",
                    "check",
                    "actualizaciones",
                    "updates",
                    "al abrir",
                ),
        ) {
            SettingToggleRow(
                title = "Buscar automáticamente",
                subtitle = "Comprueba si hay nuevas versiones al abrir la app.",
                checked = autoCheckUpdates,
                onCheckedChange = onToggleAutoCheckUpdates,
            )
        },
    )
    val channelDescription =
        if (updateChannel == UpdateChannel.NIGHTLY) {
            "Novedades continuas y correcciones automáticas de desarrollo."
        } else {
            "Versiones oficiales recomendadas y probadas."
        }
    add(
        SettingEntry(
            id = "updates_channel",
            category = SettingsCategory.UPDATES,
            section = "Canal",
            title = "Canal de actualización",
            description = channelDescription,
            keywords =
                listOf(
                    "canal",
                    "channel",
                    "nightly",
                    "estable",
                    "stable",
                    "beta",
                    "desarrollo",
                    "actualización",
                    "update",
                ),
        ) {
            SettingInlineChoiceRow(
                title = "Canal de actualización",
                subtitle = channelDescription,
            ) { isInline ->
                SegmentedControl(
                    options = UpdateChannel.entries,
                    selected = updateChannel,
                    onSelect = { channel ->
                        if (channel != updateChannel) {
                            onUpdateChannelSelected(channel)
                        }
                    },
                    label = { it.displayName },
                    fillMaxWidth = !isInline,
                )
            }
        },
    )
    add(
        SettingEntry(
            id = "updates_status_banner",
            category = SettingsCategory.UPDATES,
            section = null,
            title = "Estado de actualización",
            description = "Descarga, notas de versión e instalación de actualizaciones.",
            keywords =
                listOf(
                    "estado",
                    "status",
                    "novedades",
                    "changelog",
                    "release notes",
                    "descargar",
                    "download",
                    "instalar",
                    "install",
                    "actualizar",
                    "update",
                ),
            visible = updateState !is UpdateState.Idle,
            searchDisplay = SettingSearchDisplay.LINK,
        ) {
            UpdateStatusContent(
                state = updateState,
                currentVersion = currentVersion,
                updateChannel = updateChannel,
                onCheckForUpdates = onCheckForUpdates,
                onStartDownload = onStartDownload,
                onCancelDownload = onCancelDownload,
                onInstallUpdate = onInstallUpdate,
            )
        },
    )
}

@Composable
private fun AccountSessionEntryRow(
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
