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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.adriianh.core.domain.model.CacheSizeLimit
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.settings_cat_account
import com.github.adriianh.melo.resources.settings_cat_appearance
import com.github.adriianh.melo.resources.settings_cat_general
import com.github.adriianh.melo.resources.settings_cat_integrations
import com.github.adriianh.melo.resources.settings_cat_library
import com.github.adriianh.melo.resources.settings_cat_now_playing
import com.github.adriianh.melo.resources.settings_cat_playback
import com.github.adriianh.melo.resources.settings_cat_updates
import com.github.adriianh.melo.resources.settings_data_saver_title
import com.github.adriianh.melo.resources.settings_group_account
import com.github.adriianh.melo.resources.settings_group_app
import com.github.adriianh.melo.resources.settings_group_experience
import com.github.adriianh.melo.resources.settings_summary_account_connected
import com.github.adriianh.melo.resources.settings_summary_account_disconnected
import com.github.adriianh.melo.resources.settings_summary_cache
import com.github.adriianh.melo.resources.settings_summary_discord_active
import com.github.adriianh.melo.resources.settings_summary_discord_disabled
import com.github.adriianh.melo.resources.settings_summary_dynamic_color
import com.github.adriianh.melo.resources.settings_summary_offline_suffix
import com.github.adriianh.melo.resources.settings_summary_quality
import com.github.adriianh.melo.resources.settings_summary_queue_continuous
import com.github.adriianh.melo.resources.settings_summary_queue_fixed
import com.github.adriianh.melo.resources.settings_summary_update_available
import com.github.adriianh.melo.resources.settings_summary_update_channel
import com.github.adriianh.melo.resources.settings_summary_update_downloading
import com.github.adriianh.melo.resources.settings_summary_update_ready
import com.github.adriianh.melo.ui.i18n.uiLabel
import com.github.adriianh.melo.ui.i18n.uiTitle
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal enum class SettingsCategoryGroup(
    val captionRes: StringResource,
) {
    EXPERIENCE(Res.string.settings_group_experience),
    ACCOUNT(Res.string.settings_group_account),
    APP(Res.string.settings_group_app),
}

internal enum class SettingsCategory(
    val titleRes: StringResource,
    val icon: ImageVector,
    val group: SettingsCategoryGroup,
) {
    APPEARANCE(Res.string.settings_cat_appearance, Icons.Default.ColorLens, SettingsCategoryGroup.EXPERIENCE),
    NOW_PLAYING(Res.string.settings_cat_now_playing, Icons.Default.MusicNote, SettingsCategoryGroup.EXPERIENCE),
    PLAYBACK(Res.string.settings_cat_playback, Icons.Default.Headphones, SettingsCategoryGroup.EXPERIENCE),
    LIBRARY(Res.string.settings_cat_library, Icons.Default.Download, SettingsCategoryGroup.EXPERIENCE),
    ACCOUNT(Res.string.settings_cat_account, Icons.Default.Person, SettingsCategoryGroup.ACCOUNT),
    INTEGRATIONS(Res.string.settings_cat_integrations, Icons.Default.Share, SettingsCategoryGroup.ACCOUNT),
    GENERAL(Res.string.settings_cat_general, Icons.Default.Tune, SettingsCategoryGroup.APP),
    UPDATES(Res.string.settings_cat_updates, Icons.Default.SystemUpdate, SettingsCategoryGroup.APP),
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

@Composable
internal fun SettingsCategory.summary(
    settings: Settings,
    isLoggedIn: Boolean,
    updateState: UpdateState?,
): String =
    when (this) {
        SettingsCategory.APPEARANCE -> appearanceSummary(settings)
        SettingsCategory.NOW_PLAYING -> nowPlayingSummary(settings)
        SettingsCategory.PLAYBACK -> playbackSummary(settings)
        SettingsCategory.LIBRARY -> librarySummary(settings)
        SettingsCategory.ACCOUNT ->
            stringResource(
                if (isLoggedIn) {
                    Res.string.settings_summary_account_connected
                } else {
                    Res.string.settings_summary_account_disconnected
                },
            )

        SettingsCategory.INTEGRATIONS ->
            stringResource(
                if (settings.discordRpcEnabled) {
                    Res.string.settings_summary_discord_active
                } else {
                    Res.string.settings_summary_discord_disabled
                },
            )

        SettingsCategory.GENERAL -> settings.appLanguage.uiLabel()

        SettingsCategory.UPDATES -> updatesSummary(settings, updateState)
    }

@Composable
private fun appearanceSummary(settings: Settings): String {
    val mode = settings.themeMode.uiLabel()
    val palette =
        if (settings.dynamicColor) {
            stringResource(Res.string.settings_summary_dynamic_color)
        } else {
            settings.theme.displayName
        }
    return "$mode · $palette"
}

@Composable
private fun nowPlayingSummary(settings: Settings): String {
    val blur = settings.nowPlayingBlurIntensity.uiLabel()
    val lyricsScale = settings.lyricsFontScale.uiLabel()
    return "$blur · $lyricsScale"
}

@Composable
private fun playbackSummary(settings: Settings): String {
    val quality =
        if (settings.dataSaver) {
            stringResource(Res.string.settings_data_saver_title)
        } else {
            stringResource(Res.string.settings_summary_quality, settings.audioQuality.uiTitle().lowercase())
        }
    val queue =
        stringResource(
            if (settings.autoplay) {
                Res.string.settings_summary_queue_continuous
            } else {
                Res.string.settings_summary_queue_fixed
            },
        )
    return "$quality · $queue"
}

@Composable
private fun librarySummary(settings: Settings): String {
    val cacheLabel = CacheSizeLimit.fromSizeMb(settings.maxOfflineSizeMb).uiTitle()
    val cache = stringResource(Res.string.settings_summary_cache, cacheLabel)
    val format = settings.downloadFormat.name
    val offline = if (settings.offlineMode) stringResource(Res.string.settings_summary_offline_suffix) else ""
    return "$cache · $format$offline"
}

@Composable
private fun updatesSummary(
    settings: Settings,
    updateState: UpdateState?,
): String =
    when (updateState) {
        is UpdateState.UpdateAvailable ->
            stringResource(Res.string.settings_summary_update_available, updateState.release.version)
        is UpdateState.ManualUpdate ->
            stringResource(Res.string.settings_summary_update_available, updateState.release.version)
        is UpdateState.ReadyToInstall -> stringResource(Res.string.settings_summary_update_ready)
        is UpdateState.Downloading -> stringResource(Res.string.settings_summary_update_downloading)
        else -> stringResource(Res.string.settings_summary_update_channel, settings.updateChannel.uiLabel())
    }

internal fun UpdateState?.hasAvailableUpdate(): Boolean =
    this is UpdateState.UpdateAvailable ||
        this is UpdateState.ManualUpdate ||
        this is UpdateState.ReadyToInstall

internal val LocalHasAvailableUpdate = compositionLocalOf { false }

@Composable
internal fun SettingsEntryUpdateBadge(
    modifier: Modifier = Modifier,
    showDot: Boolean = LocalHasAvailableUpdate.current,
    dotSize: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier = modifier) {
        content()
        if (showDot) {
            Box(
                modifier =
                    Modifier
                        .size(dotSize)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(primary),
            )
        }
    }
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
                    SettingsSectionCaption(text = stringResource(group.captionRes))
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
                    text = stringResource(category.titleRes),
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
    modifier: Modifier = Modifier,
    categories: List<SettingsCategory>,
    selectedCategory: SettingsCategory?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    updateState: UpdateState?,
    onSelectCategory: (SettingsCategory) -> Unit,
    onEscapeClose: (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    val grouped = remember(categories) { SettingsCategory.grouped(categories) }
    val showCaptions = grouped.size > 1
    val isSearching = searchQuery.isNotBlank()
    val focusRequesters = remember(categories) { categories.associateWith { FocusRequester() } }

    Column(
        modifier =
            modifier
                .width(240.dp)
                .fillMaxHeight()
                .background(MeloColors.surface0),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            SettingsSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onClearOrEscape = onEscapeClose,
                modifier = Modifier.fillMaxWidth(),
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
                        handleSidebarKeyNavigation(
                            event = event,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            focusRequesters = focusRequesters,
                            onSelectCategory = onSelectCategory,
                        )
                    }.verticalScroll(
                        state = scrollState,
                        enabled = scrollState.maxValue > 0,
                    ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            grouped.forEach { (group, groupCategories) ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showCaptions) {
                        SettingsSectionCaption(text = stringResource(group.captionRes))
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        groupCategories.forEach { category ->
                            val selected = !isSearching && category == selectedCategory
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
            text = stringResource(category.titleRes),
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

private fun handleSidebarKeyNavigation(
    event: KeyEvent,
    categories: List<SettingsCategory>,
    selectedCategory: SettingsCategory?,
    focusRequesters: Map<SettingsCategory, FocusRequester>,
    onSelectCategory: (SettingsCategory) -> Unit,
): Boolean {
    val delta =
        when {
            event.type != KeyEventType.KeyDown || categories.isEmpty() -> null
            event.key == Key.DirectionUp -> -1
            event.key == Key.DirectionDown -> 1
            else -> null
        } ?: return false

    val currentIndex = selectedCategory?.let { categories.indexOf(it) } ?: -1
    val nextIndex =
        if (currentIndex < 0) {
            if (delta > 0) 0 else categories.lastIndex
        } else {
            (currentIndex + delta).coerceIn(0, categories.lastIndex)
        }
    val nextCategory = categories[nextIndex]
    if (nextCategory != selectedCategory) {
        onSelectCategory(nextCategory)
    }
    focusRequesters[nextCategory]?.requestFocus()
    return true
}
