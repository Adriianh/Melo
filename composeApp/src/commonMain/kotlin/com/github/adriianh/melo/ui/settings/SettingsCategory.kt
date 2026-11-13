@file:Suppress("FunctionNaming", "LongParameterList", "TooManyFunctions", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.AudioQuality
import com.github.adriianh.core.domain.model.CacheSizeLimit
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType

internal enum class SettingsCategory(
    val title: String,
    val icon: ImageVector,
) {
    APPEARANCE("Apariencia", Icons.Default.ColorLens),
    PLAYBACK("Reproducción", Icons.Default.Headphones),
    LIBRARY("Biblioteca", Icons.Default.Download),
    ACCOUNT("Cuenta", Icons.Default.Person),
    INTEGRATIONS("Integraciones", Icons.Default.Share),
    UPDATES("Actualizaciones", Icons.Default.SystemUpdate),
    ;

    companion object {
        fun visibleFor(platformType: PlatformType): List<SettingsCategory> =
            if (platformType == PlatformType.IOS) {
                entries.filterNot { it == INTEGRATIONS }
            } else {
                entries
            }
    }
}

internal fun SettingsCategory.summary(
    settings: Settings,
    isLoggedIn: Boolean,
    updateState: UpdateState?,
): String =
    when (this) {
        SettingsCategory.APPEARANCE -> appearanceSummary(settings)
        SettingsCategory.PLAYBACK -> playbackSummary(settings)
        SettingsCategory.LIBRARY -> librarySummary(settings)
        SettingsCategory.ACCOUNT -> if (isLoggedIn) "YouTube Music conectado" else "Sin sesión iniciada"
        SettingsCategory.INTEGRATIONS -> if (settings.discordRpcEnabled) "Discord activo" else "Discord desactivado"
        SettingsCategory.UPDATES -> updatesSummary(settings, updateState)
    }

private fun appearanceSummary(settings: Settings): String {
    val mode = settings.themeMode.uiLabel()
    val palette = if (settings.dynamicColor) "Color dinámico" else settings.theme.displayName
    return "$mode · $palette"
}

private fun playbackSummary(settings: Settings): String {
    val quality =
        if (settings.dataSaver) {
            "Ahorro de datos"
        } else {
            "Calidad ${settings.audioQuality.uiTitle().lowercase()}"
        }
    val queue = if (settings.autoplay) "Continua" else "Cola fija"
    return "$quality · $queue"
}

private fun librarySummary(settings: Settings): String {
    val cache = "Caché ${CacheSizeLimit.fromSizeMb(settings.maxOfflineSizeMb).displayName}"
    val format = settings.downloadFormat.name
    val offline = if (settings.offlineMode) " · Sin conexión" else ""
    return "$cache · $format$offline"
}

private fun updatesSummary(
    settings: Settings,
    updateState: UpdateState?,
): String =
    when (updateState) {
        is UpdateState.UpdateAvailable -> "v${updateState.release.version} disponible"
        is UpdateState.ManualUpdate -> "v${updateState.release.version} disponible"
        is UpdateState.ReadyToInstall -> "Lista para instalar"
        is UpdateState.Downloading -> "Descargando actualización..."
        else -> "Canal ${settings.updateChannel.displayName}"
    }

internal fun UpdateState?.hasAvailableUpdate(): Boolean =
    this is UpdateState.UpdateAvailable ||
        this is UpdateState.ManualUpdate ||
        this is UpdateState.ReadyToInstall

internal fun ThemeMode.uiLabel(): String =
    when (this) {
        ThemeMode.SYSTEM -> "Sistema"
        ThemeMode.LIGHT -> "Claro"
        ThemeMode.DARK -> "Oscuro"
    }

internal fun AudioQuality.uiTitle(): String =
    when (this) {
        AudioQuality.AUTO -> "Automática"
        AudioQuality.HIGH -> "Alta"
        AudioQuality.MEDIUM -> "Estándar"
        AudioQuality.LOW -> "Baja"
    }

internal fun AudioQuality.uiHint(): String =
    when (this) {
        AudioQuality.AUTO -> "Adaptativa"
        AudioQuality.HIGH -> "160–256 kbps"
        AudioQuality.MEDIUM -> "128 kbps"
        AudioQuality.LOW -> "48–64 kbps"
    }

internal fun CacheSizeLimit.uiHint(): String =
    when (this) {
        CacheSizeLimit.MB_500 -> "Mínimo"
        CacheSizeLimit.GB_1 -> "Estándar"
        CacheSizeLimit.GB_2 -> "Equilibrado"
        CacheSizeLimit.GB_5 -> "Amplio"
        CacheSizeLimit.GB_10 -> "Extenso"
        CacheSizeLimit.UNLIMITED -> "Sin restricción"
    }

@Composable
internal fun SettingsCategoryList(
    categories: List<SettingsCategory>,
    settings: Settings,
    isLoggedIn: Boolean,
    updateState: UpdateState?,
    onSelectCategory: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(
                    state = scrollState,
                    enabled = scrollState.maxValue > 0,
                ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        categories.forEach { category ->
            val showDot = category == SettingsCategory.UPDATES && updateState.hasAvailableUpdate()
            SettingsCategoryListRow(
                category = category,
                summary = category.summary(settings, isLoggedIn, updateState),
                showUpdateDot = showDot,
                onClick = { onSelectCategory(category) },
            )
        }
    }
}

@Composable
private fun SettingsCategoryListRow(
    category: SettingsCategory,
    summary: String,
    showUpdateDot: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)

    Surface(
        color = MeloColors.surface2.copy(alpha = 0.45f),
        shape = shape,
        border = BorderStroke(1.dp, MeloColors.border),
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(shape)
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryBadgeIcon(icon = category.icon, showDot = showUpdateDot)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = category.title,
                    style = MeloType.body,
                    fontWeight = FontWeight.SemiBold,
                    color = MeloColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    style = MeloType.labelSmall,
                    color = MeloColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MeloColors.textMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun CategoryBadgeIcon(
    icon: ImageVector,
    showDot: Boolean,
) {
    val primary = MaterialTheme.colorScheme.primary
    Box {
        Surface(
            color = primary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.size(38.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (showDot) {
            Box(
                modifier =
                    Modifier
                        .size(9.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(primary),
            )
        }
    }
}

@Composable
internal fun SettingsSidebar(
    categories: List<SettingsCategory>,
    selectedCategory: SettingsCategory,
    settings: Settings,
    isLoggedIn: Boolean,
    updateState: UpdateState?,
    onSelectCategory: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .width(216.dp)
                .fillMaxHeight()
                .selectableGroup()
                .verticalScroll(
                    state = scrollState,
                    enabled = scrollState.maxValue > 0,
                ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        categories.forEach { category ->
            val selected = category == selectedCategory
            val showDot = category == SettingsCategory.UPDATES && updateState.hasAvailableUpdate()
            SettingsSidebarItem(
                category = category,
                summary = category.summary(settings, isLoggedIn, updateState),
                selected = selected,
                showUpdateDot = showDot,
                onClick = { onSelectCategory(category) },
            )
        }
    }
}

@Composable
private fun SettingsSidebarItem(
    category: SettingsCategory,
    summary: String,
    selected: Boolean,
    showUpdateDot: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val primary = MaterialTheme.colorScheme.primary
    val itemModifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)

    Surface(
        color = if (selected) primary.copy(alpha = 0.16f) else MeloColors.surface2.copy(alpha = 0.25f),
        shape = shape,
        border = BorderStroke(1.dp, if (selected) primary else MeloColors.border.copy(alpha = 0.5f)),
        modifier = itemModifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SidebarIconWithDot(
                icon = category.icon,
                selected = selected,
                showUpdateDot = showUpdateDot,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = category.title,
                    style = MeloType.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) primary else MeloColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    style = MeloType.labelSmall,
                    color = if (selected) primary.copy(alpha = 0.85f) else MeloColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SidebarIconWithDot(
    icon: ImageVector,
    selected: Boolean,
    showUpdateDot: Boolean,
) {
    val primary = MaterialTheme.colorScheme.primary
    Box {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) primary else MeloColors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        if (showUpdateDot) {
            Box(
                modifier =
                    Modifier
                        .size(7.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(primary),
            )
        }
    }
}
