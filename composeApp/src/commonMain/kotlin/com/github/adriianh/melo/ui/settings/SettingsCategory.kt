@file:Suppress(
    "FunctionNaming",
    "LongMethod",
    "LongParameterList",
    "TooManyFunctions",
    "ktlint:standard:function-naming",
)

package com.github.adriianh.melo.ui.settings

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.AudioQuality
import com.github.adriianh.core.domain.model.CacheSizeLimit
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType

internal enum class SettingsCategoryGroup(
    val caption: String,
) {
    EXPERIENCE("Experiencia"),
    ACCOUNT_AND_APP("Cuenta y app"),
}

internal enum class SettingsCategory(
    val title: String,
    val icon: ImageVector,
    val group: SettingsCategoryGroup,
) {
    APPEARANCE("Apariencia", Icons.Default.ColorLens, SettingsCategoryGroup.EXPERIENCE),
    PLAYBACK("Reproducción", Icons.Default.Headphones, SettingsCategoryGroup.EXPERIENCE),
    LIBRARY("Biblioteca", Icons.Default.Download, SettingsCategoryGroup.EXPERIENCE),
    ACCOUNT("Cuenta", Icons.Default.Person, SettingsCategoryGroup.ACCOUNT_AND_APP),
    INTEGRATIONS("Integraciones", Icons.Default.Share, SettingsCategoryGroup.ACCOUNT_AND_APP),
    UPDATES("Actualizaciones", Icons.Default.SystemUpdate, SettingsCategoryGroup.ACCOUNT_AND_APP),
    ;

    companion object {
        fun visibleFor(platformType: PlatformType): List<SettingsCategory> =
            if (platformType == PlatformType.IOS) {
                entries.filterNot { it == INTEGRATIONS }
            } else {
                entries
            }

        fun grouped(categories: List<SettingsCategory>): List<Pair<SettingsCategoryGroup, List<SettingsCategory>>> =
            SettingsCategoryGroup.entries.mapNotNull { group ->
                val items = categories.filter { it.group == group }
                if (items.isEmpty()) null else group to items
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
    val grouped = remember(categories) { SettingsCategory.grouped(categories) }
    val showCaptions = grouped.size > 1

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(
                    state = scrollState,
                    enabled = scrollState.maxValue > 0,
                ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        grouped.forEach { (group, groupCategories) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if (showCaptions) {
                    SettingsSectionCaption(text = group.caption)
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    groupCategories.forEachIndexed { index, category ->
                        val showUpdateHighlight =
                            category == SettingsCategory.UPDATES && updateState.hasAvailableUpdate()
                        SettingsCategoryListRow(
                            category = category,
                            summary = category.summary(settings, isLoggedIn, updateState),
                            highlightSummary = showUpdateHighlight,
                            showUpdateDot = showUpdateHighlight,
                            shape = tileShapeForPosition(index, groupCategories.size),
                            onClick = { onSelectCategory(category) },
                        )
                    }
                }
            }
        }
    }
}

private fun tileShapeForPosition(
    index: Int,
    count: Int,
): Shape =
    when {
        count <= 1 -> RoundedCornerShape(18.dp)
        index == 0 -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 6.dp, bottomStart = 6.dp)
        index == count - 1 ->
            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
        else -> RoundedCornerShape(6.dp)
    }

@Composable
private fun SettingsCategoryListRow(
    category: SettingsCategory,
    summary: String,
    highlightSummary: Boolean,
    showUpdateDot: Boolean,
    shape: Shape,
    onClick: () -> Unit,
) {
    Surface(
        color = MeloColors.surface2.copy(alpha = 0.5f),
        shape = shape,
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
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = MeloColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary,
                    style = MeloType.labelSmall,
                    color = if (highlightSummary) MaterialTheme.colorScheme.primary else MeloColors.textSecondary,
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
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp),
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
                        .size(10.dp)
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
    updateState: UpdateState?,
    onSelectCategory: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val grouped = remember(categories) { SettingsCategory.grouped(categories) }
    val showCaptions = grouped.size > 1
    val focusRequesters = remember(categories) { categories.associateWith { FocusRequester() } }

    Column(
        modifier =
            modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(MeloColors.surface0),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 16.dp, top = 20.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Ajustes",
                style = MeloType.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MeloColors.textPrimary,
            )
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .selectableGroup()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val delta =
                            when (event.key) {
                                Key.DirectionUp -> -1
                                Key.DirectionDown -> 1
                                else -> return@onPreviewKeyEvent false
                            }
                        if (categories.isEmpty()) return@onPreviewKeyEvent false
                        val currentIndex = categories.indexOf(selectedCategory).coerceAtLeast(0)
                        val nextIndex = (currentIndex + delta).coerceIn(0, categories.lastIndex)
                        val nextCategory = categories[nextIndex]
                        if (nextCategory != selectedCategory) {
                            onSelectCategory(nextCategory)
                        }
                        focusRequesters[nextCategory]?.requestFocus()
                        true
                    }.verticalScroll(
                        state = scrollState,
                        enabled = scrollState.maxValue > 0,
                    ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            grouped.forEach { (group, groupCategories) ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showCaptions) {
                        SettingsSectionCaption(text = group.caption)
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        groupCategories.forEach { category ->
                            val selected = category == selectedCategory
                            val showDot =
                                category == SettingsCategory.UPDATES && updateState.hasAvailableUpdate()
                            SettingsSidebarItem(
                                category = category,
                                selected = selected,
                                showUpdateDot = showDot,
                                focusRequester = focusRequesters.getValue(category),
                                onClick = { onSelectCategory(category) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSidebarItem(
    category: SettingsCategory,
    selected: Boolean,
    showUpdateDot: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val primary = MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor =
        when {
            selected -> primary.copy(alpha = 0.14f)
            isHovered -> Color.White.copy(alpha = 0.06f)
            else -> Color.Transparent
        }
    val focusModifier =
        if (isFocused) {
            Modifier.border(width = 2.dp, color = primary, shape = shape)
        } else {
            Modifier
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(shape)
                .background(backgroundColor)
                .then(focusModifier)
                .focusRequester(focusRequester)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Tab,
                    onClick = onClick,
                ).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = if (selected) primary else MeloColors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = category.title,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) primary else MeloColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (showUpdateDot) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(primary),
            )
        }
    }
}
