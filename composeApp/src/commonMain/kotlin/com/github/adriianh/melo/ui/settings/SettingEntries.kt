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
import androidx.compose.material.icons.filled.RestartAlt
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
import com.github.adriianh.core.domain.model.AppLanguage
import com.github.adriianh.core.domain.model.ArtworkCornerStyle
import com.github.adriianh.core.domain.model.AudioQuality
import com.github.adriianh.core.domain.model.CacheSizeLimit
import com.github.adriianh.core.domain.model.DownloadFormat
import com.github.adriianh.core.domain.model.DownloadQuality
import com.github.adriianh.core.domain.model.FullscreenIdleHideDelay
import com.github.adriianh.core.domain.model.LyricsActiveLineColor
import com.github.adriianh.core.domain.model.LyricsActiveLineWeight
import com.github.adriianh.core.domain.model.LyricsAlignment
import com.github.adriianh.core.domain.model.LyricsAnchorPosition
import com.github.adriianh.core.domain.model.LyricsFontScale
import com.github.adriianh.core.domain.model.LyricsInactiveOpacity
import com.github.adriianh.core.domain.model.LyricsLineSpacing
import com.github.adriianh.core.domain.model.NowPlayingBackdropScrim
import com.github.adriianh.core.domain.model.NowPlayingBlurIntensity
import com.github.adriianh.core.domain.model.NowPlayingInitialPanel
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.model.ThemeMode
import com.github.adriianh.core.domain.model.ThemePreset
import com.github.adriianh.core.domain.model.update.AppRelease
import com.github.adriianh.core.domain.model.update.ReleaseAsset
import com.github.adriianh.core.domain.model.update.UpdateChannel
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.core.util.MeloVersion
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.settings_account_channels_desc
import com.github.adriianh.melo.resources.settings_account_channels_title
import com.github.adriianh.melo.resources.settings_account_connected_desc
import com.github.adriianh.melo.resources.settings_account_connected_title
import com.github.adriianh.melo.resources.settings_account_disconnected_desc
import com.github.adriianh.melo.resources.settings_account_disconnected_title
import com.github.adriianh.melo.resources.settings_ambient_motion_desc
import com.github.adriianh.melo.resources.settings_ambient_motion_title
import com.github.adriianh.melo.resources.settings_app_language_desc
import com.github.adriianh.melo.resources.settings_app_language_title
import com.github.adriianh.melo.resources.settings_artwork_corners_desc
import com.github.adriianh.melo.resources.settings_artwork_corners_title
import com.github.adriianh.melo.resources.settings_audio_quality_desc
import com.github.adriianh.melo.resources.settings_audio_quality_disabled
import com.github.adriianh.melo.resources.settings_audio_quality_title
import com.github.adriianh.melo.resources.settings_auto_check_desc
import com.github.adriianh.melo.resources.settings_auto_check_title
import com.github.adriianh.melo.resources.settings_autoplay_desc
import com.github.adriianh.melo.resources.settings_autoplay_title
import com.github.adriianh.melo.resources.settings_backdrop_scrim_desc
import com.github.adriianh.melo.resources.settings_backdrop_scrim_disabled
import com.github.adriianh.melo.resources.settings_backdrop_scrim_title
import com.github.adriianh.melo.resources.settings_blur_intensity_desc
import com.github.adriianh.melo.resources.settings_blur_intensity_title
import com.github.adriianh.melo.resources.settings_cache_size_desc
import com.github.adriianh.melo.resources.settings_cache_size_title
import com.github.adriianh.melo.resources.settings_clear_cache_action
import com.github.adriianh.melo.resources.settings_clear_cache_desc
import com.github.adriianh.melo.resources.settings_clear_cache_title
import com.github.adriianh.melo.resources.settings_data_saver_desc
import com.github.adriianh.melo.resources.settings_data_saver_title
import com.github.adriianh.melo.resources.settings_discord_account_title
import com.github.adriianh.melo.resources.settings_discord_linked_desc
import com.github.adriianh.melo.resources.settings_discord_rpc_android_desc
import com.github.adriianh.melo.resources.settings_discord_rpc_desktop_desc
import com.github.adriianh.melo.resources.settings_discord_rpc_disabled
import com.github.adriianh.melo.resources.settings_discord_rpc_title
import com.github.adriianh.melo.resources.settings_discord_unlinked_desc
import com.github.adriianh.melo.resources.settings_download_format_desc
import com.github.adriianh.melo.resources.settings_download_format_title
import com.github.adriianh.melo.resources.settings_download_quality_desc
import com.github.adriianh.melo.resources.settings_download_quality_title
import com.github.adriianh.melo.resources.settings_dynamic_color_desc
import com.github.adriianh.melo.resources.settings_dynamic_color_title
import com.github.adriianh.melo.resources.settings_idle_hide_desc
import com.github.adriianh.melo.resources.settings_idle_hide_title
import com.github.adriianh.melo.resources.settings_initial_panel_desc
import com.github.adriianh.melo.resources.settings_initial_panel_title
import com.github.adriianh.melo.resources.settings_installed_version_title
import com.github.adriianh.melo.resources.settings_keep_screen_on_desc
import com.github.adriianh.melo.resources.settings_keep_screen_on_title
import com.github.adriianh.melo.resources.settings_kw_account_channels
import com.github.adriianh.melo.resources.settings_kw_account_session
import com.github.adriianh.melo.resources.settings_kw_ambient_motion
import com.github.adriianh.melo.resources.settings_kw_app_language
import com.github.adriianh.melo.resources.settings_kw_artwork_corners
import com.github.adriianh.melo.resources.settings_kw_audio_quality
import com.github.adriianh.melo.resources.settings_kw_auto_check
import com.github.adriianh.melo.resources.settings_kw_autoplay
import com.github.adriianh.melo.resources.settings_kw_backdrop_scrim
import com.github.adriianh.melo.resources.settings_kw_blur_intensity
import com.github.adriianh.melo.resources.settings_kw_cache_size
import com.github.adriianh.melo.resources.settings_kw_clear_cache
import com.github.adriianh.melo.resources.settings_kw_data_saver
import com.github.adriianh.melo.resources.settings_kw_discord_account
import com.github.adriianh.melo.resources.settings_kw_discord_rpc
import com.github.adriianh.melo.resources.settings_kw_download_format
import com.github.adriianh.melo.resources.settings_kw_download_quality
import com.github.adriianh.melo.resources.settings_kw_dynamic_color
import com.github.adriianh.melo.resources.settings_kw_idle_hide
import com.github.adriianh.melo.resources.settings_kw_initial_panel
import com.github.adriianh.melo.resources.settings_kw_installed_version
import com.github.adriianh.melo.resources.settings_kw_keep_screen_on
import com.github.adriianh.melo.resources.settings_kw_local_folders
import com.github.adriianh.melo.resources.settings_kw_lyrics_alignment
import com.github.adriianh.melo.resources.settings_kw_lyrics_anchor
import com.github.adriianh.melo.resources.settings_kw_lyrics_auto_translate
import com.github.adriianh.melo.resources.settings_kw_lyrics_blur
import com.github.adriianh.melo.resources.settings_kw_lyrics_color
import com.github.adriianh.melo.resources.settings_kw_lyrics_instrumental_breaks
import com.github.adriianh.melo.resources.settings_kw_lyrics_offset
import com.github.adriianh.melo.resources.settings_kw_lyrics_opacity
import com.github.adriianh.melo.resources.settings_kw_lyrics_scale
import com.github.adriianh.melo.resources.settings_kw_lyrics_source_badge
import com.github.adriianh.melo.resources.settings_kw_lyrics_spacing
import com.github.adriianh.melo.resources.settings_kw_lyrics_target_language
import com.github.adriianh.melo.resources.settings_kw_lyrics_weight
import com.github.adriianh.melo.resources.settings_kw_lyrics_zoom
import com.github.adriianh.melo.resources.settings_kw_offline_mode
import com.github.adriianh.melo.resources.settings_kw_reduce_motion
import com.github.adriianh.melo.resources.settings_kw_reduce_transparency
import com.github.adriianh.melo.resources.settings_kw_reset_now_playing
import com.github.adriianh.melo.resources.settings_kw_resume_session
import com.github.adriianh.melo.resources.settings_kw_show_remaining_time
import com.github.adriianh.melo.resources.settings_kw_stop_on_close
import com.github.adriianh.melo.resources.settings_kw_sync_history
import com.github.adriianh.melo.resources.settings_kw_theme_mode
import com.github.adriianh.melo.resources.settings_kw_theme_preset
import com.github.adriianh.melo.resources.settings_kw_update_channel
import com.github.adriianh.melo.resources.settings_kw_update_status
import com.github.adriianh.melo.resources.settings_local_folders_desc
import com.github.adriianh.melo.resources.settings_local_folders_title
import com.github.adriianh.melo.resources.settings_login_action
import com.github.adriianh.melo.resources.settings_logout_action
import com.github.adriianh.melo.resources.settings_logout_confirm_message
import com.github.adriianh.melo.resources.settings_logout_confirm_title
import com.github.adriianh.melo.resources.settings_lyrics_alignment_desc
import com.github.adriianh.melo.resources.settings_lyrics_alignment_title
import com.github.adriianh.melo.resources.settings_lyrics_anchor_desc
import com.github.adriianh.melo.resources.settings_lyrics_anchor_title
import com.github.adriianh.melo.resources.settings_lyrics_auto_translate_desc
import com.github.adriianh.melo.resources.settings_lyrics_auto_translate_title
import com.github.adriianh.melo.resources.settings_lyrics_blur_desc
import com.github.adriianh.melo.resources.settings_lyrics_blur_disabled_motion
import com.github.adriianh.melo.resources.settings_lyrics_blur_title
import com.github.adriianh.melo.resources.settings_lyrics_color_desc
import com.github.adriianh.melo.resources.settings_lyrics_color_title
import com.github.adriianh.melo.resources.settings_lyrics_instrumental_breaks_desc
import com.github.adriianh.melo.resources.settings_lyrics_instrumental_breaks_title
import com.github.adriianh.melo.resources.settings_lyrics_offset_desc
import com.github.adriianh.melo.resources.settings_lyrics_offset_title
import com.github.adriianh.melo.resources.settings_lyrics_opacity_desc
import com.github.adriianh.melo.resources.settings_lyrics_opacity_title
import com.github.adriianh.melo.resources.settings_lyrics_scale_desc
import com.github.adriianh.melo.resources.settings_lyrics_scale_title
import com.github.adriianh.melo.resources.settings_lyrics_source_badge_desc
import com.github.adriianh.melo.resources.settings_lyrics_source_badge_title
import com.github.adriianh.melo.resources.settings_lyrics_spacing_desc
import com.github.adriianh.melo.resources.settings_lyrics_spacing_title
import com.github.adriianh.melo.resources.settings_lyrics_target_language_desc
import com.github.adriianh.melo.resources.settings_lyrics_target_language_disabled
import com.github.adriianh.melo.resources.settings_lyrics_target_language_title
import com.github.adriianh.melo.resources.settings_lyrics_weight_desc
import com.github.adriianh.melo.resources.settings_lyrics_weight_title
import com.github.adriianh.melo.resources.settings_lyrics_zoom_desc
import com.github.adriianh.melo.resources.settings_lyrics_zoom_title
import com.github.adriianh.melo.resources.settings_offline_mode_desc
import com.github.adriianh.melo.resources.settings_offline_mode_title
import com.github.adriianh.melo.resources.settings_reduce_motion_desc
import com.github.adriianh.melo.resources.settings_reduce_motion_title
import com.github.adriianh.melo.resources.settings_reduce_transparency_desc
import com.github.adriianh.melo.resources.settings_reduce_transparency_title
import com.github.adriianh.melo.resources.settings_reset_now_playing_action
import com.github.adriianh.melo.resources.settings_reset_now_playing_confirm_message
import com.github.adriianh.melo.resources.settings_reset_now_playing_confirm_title
import com.github.adriianh.melo.resources.settings_reset_now_playing_desc
import com.github.adriianh.melo.resources.settings_reset_now_playing_title
import com.github.adriianh.melo.resources.settings_resume_session_desc
import com.github.adriianh.melo.resources.settings_resume_session_title
import com.github.adriianh.melo.resources.settings_section_account
import com.github.adriianh.melo.resources.settings_section_channel
import com.github.adriianh.melo.resources.settings_section_discord
import com.github.adriianh.melo.resources.settings_section_downloads
import com.github.adriianh.melo.resources.settings_section_effects
import com.github.adriianh.melo.resources.settings_section_language
import com.github.adriianh.melo.resources.settings_section_lyrics_appearance
import com.github.adriianh.melo.resources.settings_section_lyrics_behavior
import com.github.adriianh.melo.resources.settings_section_now_playing_backdrop
import com.github.adriianh.melo.resources.settings_section_now_playing_reset
import com.github.adriianh.melo.resources.settings_section_playback
import com.github.adriianh.melo.resources.settings_section_theme_color
import com.github.adriianh.melo.resources.settings_section_version
import com.github.adriianh.melo.resources.settings_show_remaining_time_desc
import com.github.adriianh.melo.resources.settings_show_remaining_time_title
import com.github.adriianh.melo.resources.settings_stop_on_close_desc
import com.github.adriianh.melo.resources.settings_stop_on_close_title
import com.github.adriianh.melo.resources.settings_sync_history_desc
import com.github.adriianh.melo.resources.settings_sync_history_disabled
import com.github.adriianh.melo.resources.settings_sync_history_title
import com.github.adriianh.melo.resources.settings_theme_mode_desc
import com.github.adriianh.melo.resources.settings_theme_mode_title
import com.github.adriianh.melo.resources.settings_theme_preset_desc
import com.github.adriianh.melo.resources.settings_theme_preset_disabled
import com.github.adriianh.melo.resources.settings_theme_preset_title
import com.github.adriianh.melo.resources.settings_update_channel_nightly_desc
import com.github.adriianh.melo.resources.settings_update_channel_stable_desc
import com.github.adriianh.melo.resources.settings_update_channel_title
import com.github.adriianh.melo.resources.settings_update_status_desc
import com.github.adriianh.melo.resources.settings_update_status_title
import com.github.adriianh.melo.ui.components.LocalFolderPathsEditor
import com.github.adriianh.melo.ui.components.controls.SegmentedControl
import com.github.adriianh.melo.ui.i18n.UiText
import com.github.adriianh.melo.ui.i18n.uiHint
import com.github.adriianh.melo.ui.i18n.uiLabel
import com.github.adriianh.melo.ui.i18n.uiTitle
import com.github.adriianh.melo.ui.player.components.SUPPORTED_LYRICS_LANGUAGES
import com.github.adriianh.melo.util.MeloColors
import com.github.adriianh.melo.util.MeloType
import com.github.adriianh.melo.util.PlatformType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

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
    val categoryTitle: String = "",
    val visible: Boolean = true,
    val searchDisplay: SettingSearchDisplay = SettingSearchDisplay.INLINE,
    val content: @Composable () -> Unit,
)

@Composable
private fun keywordsResource(res: StringResource): List<String> =
    stringResource(res).split(",").map { it.trim() }.filter { it.isNotEmpty() }

@Composable
internal fun rememberSettingEntries(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: UiText?,
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
    buildList {
        addAppearanceEntries(
            settings = settings,
            actions = actions,
        )
        addNowPlayingEntries(
            settings = settings,
            actions = actions,
            platformType = platformType,
        )
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
        addGeneralEntries(
            settings = settings,
            actions = actions,
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

@Composable
private fun MutableList<SettingEntry>.addAppearanceEntries(
    settings: Settings,
    actions: SettingsActions,
) {
    val categoryTitle = stringResource(SettingsCategory.APPEARANCE.titleRes)
    val section = stringResource(Res.string.settings_section_theme_color)
    val themeModeTitle = stringResource(Res.string.settings_theme_mode_title)
    val themeModeDesc = stringResource(Res.string.settings_theme_mode_desc)
    add(
        SettingEntry(
            id = "appearance_theme_mode",
            category = SettingsCategory.APPEARANCE,
            section = section,
            title = themeModeTitle,
            description = themeModeDesc,
            keywords = keywordsResource(Res.string.settings_kw_theme_mode),
            categoryTitle = categoryTitle,
        ) {
            SettingInlineChoiceRow(
                title = themeModeTitle,
                subtitle = themeModeDesc,
            ) { isInline ->
                val themeModeLabels = ThemeMode.entries.associateWith { it.uiLabel() }
                SegmentedControl(
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    onSelect = actions.onThemeModeSelected,
                    label = { themeModeLabels.getValue(it) },
                    fillMaxWidth = !isInline,
                )
            }
        },
    )
    val dynamicColorTitle = stringResource(Res.string.settings_dynamic_color_title)
    val dynamicColorDesc = stringResource(Res.string.settings_dynamic_color_desc)
    add(
        SettingEntry(
            id = "appearance_dynamic_color",
            category = SettingsCategory.APPEARANCE,
            section = section,
            title = dynamicColorTitle,
            description = dynamicColorDesc,
            keywords = keywordsResource(Res.string.settings_kw_dynamic_color),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = dynamicColorTitle,
                subtitle = dynamicColorDesc,
                checked = settings.dynamicColor,
                onCheckedChange = actions.onDynamicColorToggle,
            )
        },
    )
    val themePresetTitle = stringResource(Res.string.settings_theme_preset_title)
    val themePresetDesc = stringResource(Res.string.settings_theme_preset_desc)
    add(
        SettingEntry(
            id = "appearance_theme_preset",
            category = SettingsCategory.APPEARANCE,
            section = section,
            title = themePresetTitle,
            description = themePresetDesc,
            keywords = keywordsResource(Res.string.settings_kw_theme_preset),
            categoryTitle = categoryTitle,
        ) {
            val paletteEnabled = !settings.dynamicColor
            SettingChoiceRow(
                title = themePresetTitle,
                subtitle = themePresetDesc,
                enabled = paletteEnabled,
                disabledNote = stringResource(Res.string.settings_theme_preset_disabled),
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

    val effectsSection = stringResource(Res.string.settings_section_effects)
    val ambientTitle = stringResource(Res.string.settings_ambient_motion_title)
    val ambientDesc = stringResource(Res.string.settings_ambient_motion_desc)
    add(
        SettingEntry(
            id = "appearance_ambient_motion",
            category = SettingsCategory.APPEARANCE,
            section = effectsSection,
            title = ambientTitle,
            description = ambientDesc,
            keywords = keywordsResource(Res.string.settings_kw_ambient_motion),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = ambientTitle,
                subtitle = ambientDesc,
                checked = settings.ambientMotion,
                onCheckedChange = actions.onAmbientMotionToggle,
            )
        },
    )
    val reduceMotionTitle = stringResource(Res.string.settings_reduce_motion_title)
    val reduceMotionDesc = stringResource(Res.string.settings_reduce_motion_desc)
    add(
        SettingEntry(
            id = "appearance_reduce_motion",
            category = SettingsCategory.APPEARANCE,
            section = effectsSection,
            title = reduceMotionTitle,
            description = reduceMotionDesc,
            keywords = keywordsResource(Res.string.settings_kw_reduce_motion),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = reduceMotionTitle,
                subtitle = reduceMotionDesc,
                checked = settings.reduceMotion,
                onCheckedChange = actions.onReduceMotionToggle,
            )
        },
    )
    val reduceTitle = stringResource(Res.string.settings_reduce_transparency_title)
    val reduceDesc = stringResource(Res.string.settings_reduce_transparency_desc)
    add(
        SettingEntry(
            id = "appearance_reduce_transparency",
            category = SettingsCategory.APPEARANCE,
            section = effectsSection,
            title = reduceTitle,
            description = reduceDesc,
            keywords = keywordsResource(Res.string.settings_kw_reduce_transparency),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = reduceTitle,
                subtitle = reduceDesc,
                checked = settings.reduceTransparency,
                onCheckedChange = actions.onReduceTransparencyToggle,
            )
        },
    )
}

@Composable
private fun MutableList<SettingEntry>.addNowPlayingEntries(
    settings: Settings,
    actions: SettingsActions,
    platformType: PlatformType,
) {
    val categoryTitle = stringResource(SettingsCategory.NOW_PLAYING.titleRes)
    addNowPlayingBackdropEntries(
        settings = settings,
        actions = actions,
        platformType = platformType,
        categoryTitle = categoryTitle,
    )
    addLyricsAppearanceEntries(
        settings = settings,
        actions = actions,
        categoryTitle = categoryTitle,
    )
    addLyricsBehaviorEntries(
        settings = settings,
        actions = actions,
        categoryTitle = categoryTitle,
    )
    addNowPlayingResetEntry(
        actions = actions,
        categoryTitle = categoryTitle,
    )
}

@Composable
private fun MutableList<SettingEntry>.addNowPlayingBackdropEntries(
    settings: Settings,
    actions: SettingsActions,
    platformType: PlatformType,
    categoryTitle: String,
) {
    val section = stringResource(Res.string.settings_section_now_playing_backdrop)
    val effectiveBlurIntensity =
        if (!settings.nowPlayingBlur || settings.nowPlayingBlurIntensity == NowPlayingBlurIntensity.OFF) {
            NowPlayingBlurIntensity.OFF
        } else {
            settings.nowPlayingBlurIntensity
        }
    val isNowPlayingBlurEnabled = effectiveBlurIntensity != NowPlayingBlurIntensity.OFF

    val blurTitle = stringResource(Res.string.settings_blur_intensity_title)
    val blurDesc = stringResource(Res.string.settings_blur_intensity_desc)
    add(
        SettingEntry(
            id = "now_playing_blur_intensity",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = blurTitle,
            description = blurDesc,
            keywords = keywordsResource(Res.string.settings_kw_blur_intensity),
            categoryTitle = categoryTitle,
        ) {
            SettingInlineChoiceRow(
                title = blurTitle,
                subtitle = blurDesc,
            ) { isInline ->
                val blurLabels = NowPlayingBlurIntensity.entries.associateWith { it.uiLabel() }
                SegmentedControl(
                    options = NowPlayingBlurIntensity.entries,
                    selected = effectiveBlurIntensity,
                    onSelect = actions.onNowPlayingBlurIntensitySelected,
                    label = { blurLabels.getValue(it) },
                    fillMaxWidth = !isInline,
                )
            }
        },
    )
    val scrimTitle = stringResource(Res.string.settings_backdrop_scrim_title)
    val scrimDesc = stringResource(Res.string.settings_backdrop_scrim_desc)
    add(
        SettingEntry(
            id = "now_playing_backdrop_scrim",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = scrimTitle,
            description = scrimDesc,
            keywords = keywordsResource(Res.string.settings_kw_backdrop_scrim),
            categoryTitle = categoryTitle,
        ) {
            SettingInlineChoiceRow(
                title = scrimTitle,
                subtitle = scrimDesc,
                enabled = isNowPlayingBlurEnabled,
                disabledNote = stringResource(Res.string.settings_backdrop_scrim_disabled),
            ) { isInline ->
                val scrimLabels = NowPlayingBackdropScrim.entries.associateWith { it.uiLabel() }
                SegmentedControl(
                    options = NowPlayingBackdropScrim.entries,
                    selected = settings.nowPlayingBackdropScrim,
                    onSelect = { scrim ->
                        if (isNowPlayingBlurEnabled) {
                            actions.onNowPlayingBackdropScrimSelected(scrim)
                        }
                    },
                    label = { scrimLabels.getValue(it) },
                    fillMaxWidth = !isInline,
                )
            }
        },
    )

    val cornersTitle = stringResource(Res.string.settings_artwork_corners_title)
    val cornersDesc = stringResource(Res.string.settings_artwork_corners_desc)
    add(
        SettingEntry(
            id = "now_playing_artwork_corners",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = cornersTitle,
            description = cornersDesc,
            keywords = keywordsResource(Res.string.settings_kw_artwork_corners),
            categoryTitle = categoryTitle,
        ) {
            val labels = ArtworkCornerStyle.entries.associateWith { it.uiLabel() }
            SettingPresetWithCustomRow(
                title = cornersTitle,
                subtitle = cornersDesc,
                options = ArtworkCornerStyle.entries,
                selected = settings.artworkCornerStyle,
                isCustomSelected = settings.artworkCornerStyle == ArtworkCornerStyle.CUSTOM,
                onSelect = actions.onArtworkCornerStyleSelected,
                optionLabel = { labels.getValue(it) },
                customValue = settings.artworkCustomCornerRadiusDp.toFloat(),
                valueRange = 0f..36f,
                step = 2f,
                valueLabel = { "${it.roundToInt()} dp" },
                onCustomValueChange = { actions.onArtworkCustomCornerRadiusChanged(it.roundToInt()) },
                onResetCustom = { actions.onArtworkCustomCornerRadiusChanged(ArtworkCornerStyle.ROUNDED.radiusDp) },
            )
        },
    )

    val initialPanelTitle = stringResource(Res.string.settings_initial_panel_title)
    val initialPanelDesc = stringResource(Res.string.settings_initial_panel_desc)
    add(
        SettingEntry(
            id = "now_playing_initial_panel",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = initialPanelTitle,
            description = initialPanelDesc,
            keywords = keywordsResource(Res.string.settings_kw_initial_panel),
            categoryTitle = categoryTitle,
        ) {
            SettingInlineChoiceRow(
                title = initialPanelTitle,
                subtitle = initialPanelDesc,
            ) { isInline ->
                val labels = NowPlayingInitialPanel.entries.associateWith { it.uiLabel() }
                SegmentedControl(
                    options = NowPlayingInitialPanel.entries,
                    selected = settings.nowPlayingInitialPanel,
                    onSelect = actions.onNowPlayingInitialPanelSelected,
                    label = { labels.getValue(it) },
                    fillMaxWidth = !isInline,
                )
            }
        },
    )

    val remainingTimeTitle = stringResource(Res.string.settings_show_remaining_time_title)
    val remainingTimeDesc = stringResource(Res.string.settings_show_remaining_time_desc)
    add(
        SettingEntry(
            id = "now_playing_show_remaining_time",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = remainingTimeTitle,
            description = remainingTimeDesc,
            keywords = keywordsResource(Res.string.settings_kw_show_remaining_time),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = remainingTimeTitle,
                subtitle = remainingTimeDesc,
                checked = settings.playerShowRemainingTime,
                onCheckedChange = actions.onPlayerShowRemainingTimeToggle,
            )
        },
    )

    if (platformType == PlatformType.DESKTOP) {
        val idleHideTitle = stringResource(Res.string.settings_idle_hide_title)
        val idleHideDesc = stringResource(Res.string.settings_idle_hide_desc)
        add(
            SettingEntry(
                id = "now_playing_fullscreen_idle_hide",
                category = SettingsCategory.NOW_PLAYING,
                section = section,
                title = idleHideTitle,
                description = idleHideDesc,
                keywords = keywordsResource(Res.string.settings_kw_idle_hide),
                categoryTitle = categoryTitle,
            ) {
                val labels = FullscreenIdleHideDelay.entries.associateWith { it.uiLabel() }
                SettingPresetWithCustomRow(
                    title = idleHideTitle,
                    subtitle = idleHideDesc,
                    options = FullscreenIdleHideDelay.entries,
                    selected = settings.fullscreenIdleHideDelay,
                    isCustomSelected = settings.fullscreenIdleHideDelay == FullscreenIdleHideDelay.CUSTOM,
                    onSelect = actions.onFullscreenIdleHideDelaySelected,
                    optionLabel = { labels.getValue(it) },
                    customValue = settings.fullscreenCustomIdleHideSeconds.toFloat(),
                    valueRange = 1f..30f,
                    step = 1f,
                    valueLabel = { "${it.roundToInt()}s" },
                    onCustomValueChange = { actions.onFullscreenCustomIdleHideSecondsChanged(it.roundToInt()) },
                    onResetCustom = { actions.onFullscreenCustomIdleHideSecondsChanged(4) },
                )
            },
        )
    }

    if (platformType == PlatformType.ANDROID) {
        val keepScreenTitle = stringResource(Res.string.settings_keep_screen_on_title)
        val keepScreenDesc = stringResource(Res.string.settings_keep_screen_on_desc)
        add(
            SettingEntry(
                id = "now_playing_keep_screen_on",
                category = SettingsCategory.NOW_PLAYING,
                section = section,
                title = keepScreenTitle,
                description = keepScreenDesc,
                keywords = keywordsResource(Res.string.settings_kw_keep_screen_on),
                categoryTitle = categoryTitle,
            ) {
                SettingToggleRow(
                    title = keepScreenTitle,
                    subtitle = keepScreenDesc,
                    checked = settings.nowPlayingKeepScreenOn,
                    onCheckedChange = actions.onNowPlayingKeepScreenOnToggle,
                )
            },
        )
    }
}

@Composable
private fun MutableList<SettingEntry>.addLyricsAppearanceEntries(
    settings: Settings,
    actions: SettingsActions,
    categoryTitle: String,
) {
    val section = stringResource(Res.string.settings_section_lyrics_appearance)

    val lyricsScaleTitle = stringResource(Res.string.settings_lyrics_scale_title)
    val lyricsScaleDesc = stringResource(Res.string.settings_lyrics_scale_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_font_scale",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = lyricsScaleTitle,
            description = lyricsScaleDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_scale),
            categoryTitle = categoryTitle,
        ) {
            val labels = LyricsFontScale.entries.associateWith { it.uiLabel() }
            SettingPresetWithCustomRow(
                title = lyricsScaleTitle,
                subtitle = lyricsScaleDesc,
                options = LyricsFontScale.entries,
                selected = settings.lyricsFontScale,
                isCustomSelected = settings.lyricsFontScale == LyricsFontScale.CUSTOM,
                onSelect = actions.onLyricsFontScaleSelected,
                optionLabel = { labels.getValue(it) },
                customValue = settings.lyricsCustomFontScale,
                valueRange = 0.75f..1.40f,
                step = 0.05f,
                valueLabel = { "${(it * 100).roundToInt()}%" },
                onCustomValueChange = actions.onLyricsCustomFontScaleChanged,
                onResetCustom = { actions.onLyricsCustomFontScaleChanged(LyricsFontScale.NORMAL.scaleFactor) },
            )
        },
    )

    val spacingTitle = stringResource(Res.string.settings_lyrics_spacing_title)
    val spacingDesc = stringResource(Res.string.settings_lyrics_spacing_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_spacing",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = spacingTitle,
            description = spacingDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_spacing),
            categoryTitle = categoryTitle,
        ) {
            val labels = LyricsLineSpacing.entries.associateWith { it.uiLabel() }
            SettingPresetWithCustomRow(
                title = spacingTitle,
                subtitle = spacingDesc,
                options = LyricsLineSpacing.entries,
                selected = settings.lyricsLineSpacing,
                isCustomSelected = settings.lyricsLineSpacing == LyricsLineSpacing.CUSTOM,
                onSelect = actions.onLyricsLineSpacingSelected,
                optionLabel = { labels.getValue(it) },
                customValue = settings.lyricsCustomLineSpacingDp.toFloat(),
                valueRange = 8f..40f,
                step = 2f,
                valueLabel = { "${it.roundToInt()} dp" },
                onCustomValueChange = { actions.onLyricsCustomLineSpacingChanged(it.roundToInt()) },
                onResetCustom = { actions.onLyricsCustomLineSpacingChanged(LyricsLineSpacing.NORMAL.spacingDp) },
            )
        },
    )

    val weightTitle = stringResource(Res.string.settings_lyrics_weight_title)
    val weightDesc = stringResource(Res.string.settings_lyrics_weight_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_weight",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = weightTitle,
            description = weightDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_weight),
            categoryTitle = categoryTitle,
        ) {
            SettingInlineChoiceRow(
                title = weightTitle,
                subtitle = weightDesc,
            ) { isInline ->
                val labels = LyricsActiveLineWeight.entries.associateWith { it.uiLabel() }
                SegmentedControl(
                    options = LyricsActiveLineWeight.entries,
                    selected = settings.lyricsActiveLineWeight,
                    onSelect = actions.onLyricsActiveLineWeightSelected,
                    label = { labels.getValue(it) },
                    fillMaxWidth = !isInline,
                )
            }
        },
    )

    val lyricsZoomTitle = stringResource(Res.string.settings_lyrics_zoom_title)
    val lyricsZoomDesc = stringResource(Res.string.settings_lyrics_zoom_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_zoom",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = lyricsZoomTitle,
            description = lyricsZoomDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_zoom),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = lyricsZoomTitle,
                subtitle = lyricsZoomDesc,
                checked = settings.lyricsActiveLineZoom,
                onCheckedChange = actions.onLyricsActiveLineZoomToggle,
            )
        },
    )

    val lyricsColorTitle = stringResource(Res.string.settings_lyrics_color_title)
    val lyricsColorDesc = stringResource(Res.string.settings_lyrics_color_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_color",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = lyricsColorTitle,
            description = lyricsColorDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_color),
            categoryTitle = categoryTitle,
        ) {
            SettingInlineChoiceRow(
                title = lyricsColorTitle,
                subtitle = lyricsColorDesc,
            ) { isInline ->
                val labels = LyricsActiveLineColor.entries.associateWith { it.uiLabel() }
                SegmentedControl(
                    options = LyricsActiveLineColor.entries,
                    selected = settings.lyricsActiveLineColor,
                    onSelect = actions.onLyricsActiveLineColorSelected,
                    label = { labels.getValue(it) },
                    fillMaxWidth = !isInline,
                )
            }
        },
    )

    val opacityTitle = stringResource(Res.string.settings_lyrics_opacity_title)
    val opacityDesc = stringResource(Res.string.settings_lyrics_opacity_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_opacity",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = opacityTitle,
            description = opacityDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_opacity),
            categoryTitle = categoryTitle,
        ) {
            val labels = LyricsInactiveOpacity.entries.associateWith { it.uiLabel() }
            SettingPresetWithCustomRow(
                title = opacityTitle,
                subtitle = opacityDesc,
                options = LyricsInactiveOpacity.entries,
                selected = settings.lyricsInactiveOpacity,
                isCustomSelected = settings.lyricsInactiveOpacity == LyricsInactiveOpacity.CUSTOM,
                onSelect = actions.onLyricsInactiveOpacitySelected,
                optionLabel = { labels.getValue(it) },
                customValue = settings.lyricsCustomInactiveOpacity,
                valueRange = 0.20f..0.85f,
                step = 0.05f,
                valueLabel = { "${(it * 100).roundToInt()}%" },
                onCustomValueChange = actions.onLyricsCustomInactiveOpacityChanged,
                onResetCustom = {
                    actions.onLyricsCustomInactiveOpacityChanged(LyricsInactiveOpacity.NORMAL.baseAlpha)
                },
            )
        },
    )

    val lyricsBlurTitle = stringResource(Res.string.settings_lyrics_blur_title)
    val lyricsBlurDesc = stringResource(Res.string.settings_lyrics_blur_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_blur",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = lyricsBlurTitle,
            description = lyricsBlurDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_blur),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = lyricsBlurTitle,
                subtitle = lyricsBlurDesc,
                checked = settings.lyricsBlurEnabled,
                onCheckedChange = actions.onLyricsBlurToggle,
                enabled = !settings.reduceMotion,
                disabledNote = stringResource(Res.string.settings_lyrics_blur_disabled_motion),
            )
        },
    )

    val lyricsAlignTitle = stringResource(Res.string.settings_lyrics_alignment_title)
    val lyricsAlignDesc = stringResource(Res.string.settings_lyrics_alignment_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_alignment",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = lyricsAlignTitle,
            description = lyricsAlignDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_alignment),
            categoryTitle = categoryTitle,
        ) {
            SettingInlineChoiceRow(
                title = lyricsAlignTitle,
                subtitle = lyricsAlignDesc,
            ) { isInline ->
                val labels = LyricsAlignment.entries.associateWith { it.uiLabel() }
                SegmentedControl(
                    options = LyricsAlignment.entries,
                    selected = settings.lyricsAlignment,
                    onSelect = actions.onLyricsAlignmentSelected,
                    label = { labels.getValue(it) },
                    fillMaxWidth = !isInline,
                )
            }
        },
    )
}

@Composable
private fun MutableList<SettingEntry>.addLyricsBehaviorEntries(
    settings: Settings,
    actions: SettingsActions,
    categoryTitle: String,
) {
    val section = stringResource(Res.string.settings_section_lyrics_behavior)

    val anchorTitle = stringResource(Res.string.settings_lyrics_anchor_title)
    val anchorDesc = stringResource(Res.string.settings_lyrics_anchor_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_anchor",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = anchorTitle,
            description = anchorDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_anchor),
            categoryTitle = categoryTitle,
        ) {
            SettingInlineChoiceRow(
                title = anchorTitle,
                subtitle = anchorDesc,
            ) { isInline ->
                val labels = LyricsAnchorPosition.entries.associateWith { it.uiLabel() }
                SegmentedControl(
                    options = LyricsAnchorPosition.entries,
                    selected = settings.lyricsAnchorPosition,
                    onSelect = actions.onLyricsAnchorPositionSelected,
                    label = { labels.getValue(it) },
                    fillMaxWidth = !isInline,
                )
            }
        },
    )

    val offsetTitle = stringResource(Res.string.settings_lyrics_offset_title)
    val offsetDesc = stringResource(Res.string.settings_lyrics_offset_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_offset",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = offsetTitle,
            description = offsetDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_offset),
            categoryTitle = categoryTitle,
        ) {
            SettingSliderRow(
                title = offsetTitle,
                subtitle = offsetDesc,
                value = settings.lyricsSyncOffsetMs.toFloat(),
                valueRange = -2_000f..2_000f,
                step = 100f,
                valueLabel = { raw ->
                    val ms = raw.roundToInt()
                    when {
                        ms > 0 -> "+$ms ms"
                        else -> "$ms ms"
                    }
                },
                onValueChange = { actions.onLyricsSyncOffsetChanged(it.roundToInt()) },
                onReset =
                    if (settings.lyricsSyncOffsetMs != 0) {
                        { actions.onLyricsSyncOffsetChanged(0) }
                    } else {
                        null
                    },
            )
        },
    )

    val sourceBadgeTitle = stringResource(Res.string.settings_lyrics_source_badge_title)
    val sourceBadgeDesc = stringResource(Res.string.settings_lyrics_source_badge_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_source_badge",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = sourceBadgeTitle,
            description = sourceBadgeDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_source_badge),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = sourceBadgeTitle,
                subtitle = sourceBadgeDesc,
                checked = settings.lyricsShowSourceBadge,
                onCheckedChange = actions.onLyricsShowSourceBadgeToggle,
            )
        },
    )

    val instrumentalBreaksTitle = stringResource(Res.string.settings_lyrics_instrumental_breaks_title)
    val instrumentalBreaksDesc = stringResource(Res.string.settings_lyrics_instrumental_breaks_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_instrumental_breaks",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = instrumentalBreaksTitle,
            description = instrumentalBreaksDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_instrumental_breaks),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = instrumentalBreaksTitle,
                subtitle = instrumentalBreaksDesc,
                checked = settings.lyricsShowInstrumentalBreaks,
                onCheckedChange = actions.onLyricsShowInstrumentalBreaksToggle,
            )
        },
    )

    val autoTranslateTitle = stringResource(Res.string.settings_lyrics_auto_translate_title)
    val autoTranslateDesc = stringResource(Res.string.settings_lyrics_auto_translate_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_auto_translate",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = autoTranslateTitle,
            description = autoTranslateDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_auto_translate),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = autoTranslateTitle,
                subtitle = autoTranslateDesc,
                checked = settings.lyricsAutoTranslate,
                onCheckedChange = actions.onLyricsAutoTranslateToggle,
            )
        },
    )

    val targetLangTitle = stringResource(Res.string.settings_lyrics_target_language_title)
    val targetLangDesc = stringResource(Res.string.settings_lyrics_target_language_desc)
    add(
        SettingEntry(
            id = "now_playing_lyrics_target_language",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = targetLangTitle,
            description = targetLangDesc,
            keywords = keywordsResource(Res.string.settings_kw_lyrics_target_language),
            categoryTitle = categoryTitle,
        ) {
            val targetLangEnabled = settings.lyricsAutoTranslate
            SettingChoiceRow(
                title = targetLangTitle,
                subtitle = targetLangDesc,
                enabled = targetLangEnabled,
                disabledNote = stringResource(Res.string.settings_lyrics_target_language_disabled),
            ) {
                SettingOptionGrid(
                    options = SUPPORTED_LYRICS_LANGUAGES,
                    isSelected = { (code, _) -> code == settings.lyricsDefaultTargetLanguage },
                    onSelect = { (code, _) -> actions.onLyricsDefaultTargetLanguageSelected(code) },
                    title = { (_, label) -> label },
                    enabled = targetLangEnabled,
                )
            }
        },
    )
}

@Composable
private fun MutableList<SettingEntry>.addNowPlayingResetEntry(
    actions: SettingsActions,
    categoryTitle: String,
) {
    val resetSection = stringResource(Res.string.settings_section_now_playing_reset)
    val resetTitle = stringResource(Res.string.settings_reset_now_playing_title)
    val resetDesc = stringResource(Res.string.settings_reset_now_playing_desc)
    add(
        SettingEntry(
            id = "now_playing_reset_defaults",
            category = SettingsCategory.NOW_PLAYING,
            section = resetSection,
            title = resetTitle,
            description = resetDesc,
            keywords = keywordsResource(Res.string.settings_kw_reset_now_playing),
            categoryTitle = categoryTitle,
        ) {
            var confirmReset by rememberSaveable { mutableStateOf(false) }
            SettingActionRow(
                title = resetTitle,
                subtitle = resetDesc,
            ) {
                SettingActionButton(
                    text = stringResource(Res.string.settings_reset_now_playing_action),
                    onClick = { confirmReset = true },
                    emphasis = SettingActionEmphasis.DESTRUCTIVE,
                    icon = Icons.Default.RestartAlt,
                    modifier = Modifier.actionWidth(),
                )
            }
            if (confirmReset) {
                SettingsConfirmDialog(
                    title = stringResource(Res.string.settings_reset_now_playing_confirm_title),
                    message = stringResource(Res.string.settings_reset_now_playing_confirm_message),
                    confirmText = stringResource(Res.string.settings_reset_now_playing_action),
                    icon = Icons.Default.RestartAlt,
                    onConfirm = {
                        confirmReset = false
                        actions.onResetNowPlayingDefaults()
                    },
                    onDismiss = { confirmReset = false },
                )
            }
        },
    )
}

@Composable
private fun MutableList<SettingEntry>.addPlaybackEntries(
    settings: Settings,
    actions: SettingsActions,
    platformType: PlatformType,
) {
    val categoryTitle = stringResource(SettingsCategory.PLAYBACK.titleRes)
    val section = stringResource(Res.string.settings_section_playback)
    val dataSaverTitle = stringResource(Res.string.settings_data_saver_title)
    val dataSaverDesc = stringResource(Res.string.settings_data_saver_desc)
    add(
        SettingEntry(
            id = "playback_data_saver",
            category = SettingsCategory.PLAYBACK,
            section = section,
            title = dataSaverTitle,
            description = dataSaverDesc,
            keywords = keywordsResource(Res.string.settings_kw_data_saver),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = dataSaverTitle,
                subtitle = dataSaverDesc,
                checked = settings.dataSaver,
                onCheckedChange = actions.onDataSaverToggle,
            )
        },
    )
    val audioQualityTitle = stringResource(Res.string.settings_audio_quality_title)
    val audioQualityDesc = stringResource(Res.string.settings_audio_quality_desc)
    add(
        SettingEntry(
            id = "playback_audio_quality",
            category = SettingsCategory.PLAYBACK,
            section = section,
            title = audioQualityTitle,
            description = audioQualityDesc,
            keywords = keywordsResource(Res.string.settings_kw_audio_quality),
            categoryTitle = categoryTitle,
        ) {
            val streamingQualityEnabled = !settings.dataSaver
            SettingChoiceRow(
                title = audioQualityTitle,
                subtitle = audioQualityDesc,
                enabled = streamingQualityEnabled,
                disabledNote = stringResource(Res.string.settings_audio_quality_disabled),
            ) {
                val audioQualityTitles = AudioQuality.entries.associateWith { it.uiTitle() }
                val audioQualityHints = AudioQuality.entries.associateWith { it.uiHint() }
                SettingOptionGrid(
                    options = AudioQuality.entries,
                    isSelected = { it == settings.audioQuality },
                    onSelect = actions.onAudioQualitySelected,
                    title = { audioQualityTitles.getValue(it) },
                    hint = { audioQualityHints.getValue(it) },
                    enabled = streamingQualityEnabled,
                )
            }
        },
    )
    val autoplayTitle = stringResource(Res.string.settings_autoplay_title)
    val autoplayDesc = stringResource(Res.string.settings_autoplay_desc)
    add(
        SettingEntry(
            id = "playback_autoplay",
            category = SettingsCategory.PLAYBACK,
            section = section,
            title = autoplayTitle,
            description = autoplayDesc,
            keywords = keywordsResource(Res.string.settings_kw_autoplay),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = autoplayTitle,
                subtitle = autoplayDesc,
                checked = settings.autoplay,
                onCheckedChange = actions.onAutoplayToggle,
            )
        },
    )
    val resumeTitle = stringResource(Res.string.settings_resume_session_title)
    val resumeDesc = stringResource(Res.string.settings_resume_session_desc)
    add(
        SettingEntry(
            id = "playback_resume_last_session",
            category = SettingsCategory.PLAYBACK,
            section = section,
            title = resumeTitle,
            description = resumeDesc,
            keywords = keywordsResource(Res.string.settings_kw_resume_session),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = resumeTitle,
                subtitle = resumeDesc,
                checked = settings.resumeLastActiveSession,
                onCheckedChange = actions.onResumeLastActiveSessionToggle,
            )
        },
    )
    val stopTitle = stringResource(Res.string.settings_stop_on_close_title)
    val stopDesc = stringResource(Res.string.settings_stop_on_close_desc)
    add(
        SettingEntry(
            id = "playback_stop_on_task_removal",
            category = SettingsCategory.PLAYBACK,
            section = section,
            title = stopTitle,
            description = stopDesc,
            keywords = keywordsResource(Res.string.settings_kw_stop_on_close),
            categoryTitle = categoryTitle,
            visible = platformType == PlatformType.ANDROID,
        ) {
            SettingToggleRow(
                title = stopTitle,
                subtitle = stopDesc,
                checked = settings.stopPlaybackOnTaskRemoval,
                onCheckedChange = actions.onStopPlaybackOnTaskRemovalToggle,
            )
        },
    )
}

@Composable
private fun MutableList<SettingEntry>.addLibraryEntries(
    settings: Settings,
    actions: SettingsActions,
    initialManualFolderExpanded: Boolean,
) {
    val categoryTitle = stringResource(SettingsCategory.LIBRARY.titleRes)
    val downloadsSection = stringResource(Res.string.settings_section_downloads)
    val offlineTitle = stringResource(Res.string.settings_offline_mode_title)
    val offlineDesc = stringResource(Res.string.settings_offline_mode_desc)
    add(
        SettingEntry(
            id = "library_offline_mode",
            category = SettingsCategory.LIBRARY,
            section = downloadsSection,
            title = offlineTitle,
            description = offlineDesc,
            keywords = keywordsResource(Res.string.settings_kw_offline_mode),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = offlineTitle,
                subtitle = offlineDesc,
                checked = settings.offlineMode,
                onCheckedChange = actions.onOfflineModeToggle,
            )
        },
    )
    val cacheSizeTitle = stringResource(Res.string.settings_cache_size_title)
    val cacheSizeDesc = stringResource(Res.string.settings_cache_size_desc)
    add(
        SettingEntry(
            id = "library_cache_size",
            category = SettingsCategory.LIBRARY,
            section = downloadsSection,
            title = cacheSizeTitle,
            description = cacheSizeDesc,
            keywords = keywordsResource(Res.string.settings_kw_cache_size),
            categoryTitle = categoryTitle,
        ) {
            SettingChoiceRow(
                title = cacheSizeTitle,
                subtitle = cacheSizeDesc,
            ) {
                val cacheSizeTitles = CacheSizeLimit.entries.associateWith { it.uiTitle() }
                val cacheSizeHints = CacheSizeLimit.entries.associateWith { it.uiHint() }
                SettingOptionGrid(
                    options = CacheSizeLimit.entries,
                    isSelected = { it.sizeMb == settings.maxOfflineSizeMb },
                    onSelect = actions.onCacheSizeLimitSelected,
                    title = { cacheSizeTitles.getValue(it) },
                    hint = { cacheSizeHints.getValue(it) },
                )
            }
        },
    )
    val downloadFormatTitle = stringResource(Res.string.settings_download_format_title)
    val downloadFormatDesc = stringResource(Res.string.settings_download_format_desc)
    add(
        SettingEntry(
            id = "library_download_format",
            category = SettingsCategory.LIBRARY,
            section = downloadsSection,
            title = downloadFormatTitle,
            description = downloadFormatDesc,
            keywords = keywordsResource(Res.string.settings_kw_download_format),
            categoryTitle = categoryTitle,
        ) {
            SettingChoiceRow(
                title = downloadFormatTitle,
                subtitle = downloadFormatDesc,
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
    val downloadQualityTitle = stringResource(Res.string.settings_download_quality_title)
    val downloadQualityDesc = stringResource(Res.string.settings_download_quality_desc)
    add(
        SettingEntry(
            id = "library_download_quality",
            category = SettingsCategory.LIBRARY,
            section = downloadsSection,
            title = downloadQualityTitle,
            description = downloadQualityDesc,
            keywords = keywordsResource(Res.string.settings_kw_download_quality),
            categoryTitle = categoryTitle,
        ) {
            SettingChoiceRow(
                title = downloadQualityTitle,
                subtitle = downloadQualityDesc,
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
    val clearCacheTitle = stringResource(Res.string.settings_clear_cache_title)
    val clearCacheDesc = stringResource(Res.string.settings_clear_cache_desc)
    add(
        SettingEntry(
            id = "library_clear_cache",
            category = SettingsCategory.LIBRARY,
            section = downloadsSection,
            title = clearCacheTitle,
            description = clearCacheDesc,
            keywords = keywordsResource(Res.string.settings_kw_clear_cache),
            categoryTitle = categoryTitle,
        ) {
            SettingActionRow(
                title = clearCacheTitle,
                subtitle = clearCacheDesc,
            ) {
                SettingActionButton(
                    text = stringResource(Res.string.settings_clear_cache_action),
                    onClick = actions.onClearCache,
                    emphasis = SettingActionEmphasis.DESTRUCTIVE,
                    icon = Icons.Default.DeleteSweep,
                    modifier = Modifier.actionWidth(),
                )
            }
        },
    )
    val localFoldersTitle = stringResource(Res.string.settings_local_folders_title)
    val localFoldersDesc = stringResource(Res.string.settings_local_folders_desc)
    add(
        SettingEntry(
            id = "library_local_folders",
            category = SettingsCategory.LIBRARY,
            section = localFoldersTitle,
            title = localFoldersTitle,
            description = localFoldersDesc,
            keywords = keywordsResource(Res.string.settings_kw_local_folders),
            categoryTitle = categoryTitle,
            searchDisplay = SettingSearchDisplay.LINK,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = localFoldersDesc,
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

@Composable
private fun MutableList<SettingEntry>.addAccountEntries(
    settings: Settings,
    isLoggedIn: Boolean,
    profiles: List<AccountProfile>,
    activePageId: String?,
    isLoadingProfiles: Boolean,
    isSwitchingProfile: Boolean,
    profilesError: UiText?,
    actions: SettingsActions,
) {
    val categoryTitle = stringResource(SettingsCategory.ACCOUNT.titleRes)
    val section = stringResource(Res.string.settings_section_account)
    val sessionTitle =
        stringResource(
            if (isLoggedIn) {
                Res.string.settings_account_connected_title
            } else {
                Res.string.settings_account_disconnected_title
            },
        )
    val sessionDescription =
        stringResource(
            if (isLoggedIn) {
                Res.string.settings_account_connected_desc
            } else {
                Res.string.settings_account_disconnected_desc
            },
        )
    add(
        SettingEntry(
            id = "account_session",
            category = SettingsCategory.ACCOUNT,
            section = section,
            title = sessionTitle,
            description = sessionDescription,
            keywords = keywordsResource(Res.string.settings_kw_account_session),
            categoryTitle = categoryTitle,
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
            title = stringResource(Res.string.settings_account_channels_title),
            description = stringResource(Res.string.settings_account_channels_desc),
            keywords = keywordsResource(Res.string.settings_kw_account_channels),
            categoryTitle = categoryTitle,
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
    val syncHistoryTitle = stringResource(Res.string.settings_sync_history_title)
    val syncHistoryDesc = stringResource(Res.string.settings_sync_history_desc)
    add(
        SettingEntry(
            id = "account_sync_history",
            category = SettingsCategory.ACCOUNT,
            section = section,
            title = syncHistoryTitle,
            description = syncHistoryDesc,
            keywords = keywordsResource(Res.string.settings_kw_sync_history),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = syncHistoryTitle,
                subtitle = syncHistoryDesc,
                checked = settings.syncHistoryToYouTube,
                onCheckedChange = actions.onSyncHistoryToYouTubeChanged,
                enabled = isLoggedIn,
                disabledNote = stringResource(Res.string.settings_sync_history_disabled),
            )
        },
    )
}

@Composable
private fun MutableList<SettingEntry>.addIntegrationsEntries(
    settings: Settings,
    actions: SettingsActions,
    platformType: PlatformType,
) {
    val categoryTitle = stringResource(SettingsCategory.INTEGRATIONS.titleRes)
    val section = stringResource(Res.string.settings_section_discord)
    val isVisible = platformType != PlatformType.IOS
    val isLinked = !settings.discordRpcToken.isNullOrBlank()
    val accountDescription =
        stringResource(
            if (isLinked) {
                Res.string.settings_discord_linked_desc
            } else {
                Res.string.settings_discord_unlinked_desc
            },
        )
    val rpcDescription =
        stringResource(
            if (platformType == PlatformType.ANDROID) {
                Res.string.settings_discord_rpc_android_desc
            } else {
                Res.string.settings_discord_rpc_desktop_desc
            },
        )

    add(
        SettingEntry(
            id = "integrations_discord_account",
            category = SettingsCategory.INTEGRATIONS,
            section = section,
            title = stringResource(Res.string.settings_discord_account_title),
            description = accountDescription,
            keywords = keywordsResource(Res.string.settings_kw_discord_account),
            categoryTitle = categoryTitle,
            visible = isVisible && platformType == PlatformType.ANDROID,
        ) {
            DiscordRpcAccountEntry(
                token = settings.discordRpcToken,
                onTokenChanged = actions.onDiscordTokenChanged,
            )
        },
    )
    val discordRpcTitle = stringResource(Res.string.settings_discord_rpc_title)
    add(
        SettingEntry(
            id = "integrations_discord_rpc",
            category = SettingsCategory.INTEGRATIONS,
            section = section,
            title = discordRpcTitle,
            description = rpcDescription,
            keywords = keywordsResource(Res.string.settings_kw_discord_rpc),
            categoryTitle = categoryTitle,
            visible = isVisible,
        ) {
            SettingToggleRow(
                title = discordRpcTitle,
                subtitle = rpcDescription,
                checked = settings.discordRpcEnabled,
                onCheckedChange = actions.onDiscordRpcToggle,
                enabled = platformType != PlatformType.ANDROID || isLinked,
                disabledNote = stringResource(Res.string.settings_discord_rpc_disabled),
            )
        },
    )
}

@Composable
private fun MutableList<SettingEntry>.addGeneralEntries(
    settings: Settings,
    actions: SettingsActions,
) {
    val categoryTitle = stringResource(SettingsCategory.GENERAL.titleRes)
    val section = stringResource(Res.string.settings_section_language)
    val languageTitle = stringResource(Res.string.settings_app_language_title)
    val languageDesc = stringResource(Res.string.settings_app_language_desc)
    add(
        SettingEntry(
            id = "general_app_language",
            category = SettingsCategory.GENERAL,
            section = section,
            title = languageTitle,
            description = languageDesc,
            keywords = keywordsResource(Res.string.settings_kw_app_language),
            categoryTitle = categoryTitle,
        ) {
            SettingInlineChoiceRow(
                title = languageTitle,
                subtitle = languageDesc,
            ) { isInline ->
                val languageLabels = AppLanguage.entries.associateWith { it.uiLabel() }
                SegmentedControl(
                    options = AppLanguage.entries,
                    selected = settings.appLanguage,
                    onSelect = actions.onAppLanguageSelected,
                    label = { languageLabels.getValue(it) },
                    fillMaxWidth = !isInline,
                )
            }
        },
    )
}

@Composable
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
    val categoryTitle = stringResource(SettingsCategory.UPDATES.titleRes)
    val versionSection = stringResource(Res.string.settings_section_version)
    add(
        SettingEntry(
            id = "updates_installed_version",
            category = SettingsCategory.UPDATES,
            section = versionSection,
            title = stringResource(Res.string.settings_installed_version_title),
            description = "v$currentVersion",
            keywords = keywordsResource(Res.string.settings_kw_installed_version),
            categoryTitle = categoryTitle,
        ) {
            UpdateVersionHeaderRow(
                currentVersion = currentVersion,
                canCheck = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading,
                onCheckForUpdates = { onCheckForUpdates(updateChannel) },
            )
        },
    )
    val autoCheckTitle = stringResource(Res.string.settings_auto_check_title)
    val autoCheckDesc = stringResource(Res.string.settings_auto_check_desc)
    add(
        SettingEntry(
            id = "updates_auto_check",
            category = SettingsCategory.UPDATES,
            section = versionSection,
            title = autoCheckTitle,
            description = autoCheckDesc,
            keywords = keywordsResource(Res.string.settings_kw_auto_check),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = autoCheckTitle,
                subtitle = autoCheckDesc,
                checked = autoCheckUpdates,
                onCheckedChange = onToggleAutoCheckUpdates,
            )
        },
    )
    val channelDescription =
        stringResource(
            if (updateChannel == UpdateChannel.NIGHTLY) {
                Res.string.settings_update_channel_nightly_desc
            } else {
                Res.string.settings_update_channel_stable_desc
            },
        )
    val updateChannelTitle = stringResource(Res.string.settings_update_channel_title)
    add(
        SettingEntry(
            id = "updates_channel",
            category = SettingsCategory.UPDATES,
            section = stringResource(Res.string.settings_section_channel),
            title = updateChannelTitle,
            description = channelDescription,
            keywords = keywordsResource(Res.string.settings_kw_update_channel),
            categoryTitle = categoryTitle,
        ) {
            SettingInlineChoiceRow(
                title = updateChannelTitle,
                subtitle = channelDescription,
            ) { isInline ->
                val updateChannelLabels = UpdateChannel.entries.associateWith { it.uiLabel() }
                SegmentedControl(
                    options = UpdateChannel.entries,
                    selected = updateChannel,
                    onSelect = { channel ->
                        if (channel != updateChannel) {
                            onUpdateChannelSelected(channel)
                        }
                    },
                    label = { updateChannelLabels.getValue(it) },
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
            title = stringResource(Res.string.settings_update_status_title),
            description = stringResource(Res.string.settings_update_status_desc),
            keywords = keywordsResource(Res.string.settings_kw_update_status),
            categoryTitle = categoryTitle,
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
            title = stringResource(Res.string.settings_logout_confirm_title),
            message = stringResource(Res.string.settings_logout_confirm_message),
            confirmText = stringResource(Res.string.settings_logout_action),
            icon = Icons.AutoMirrored.Filled.Logout,
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            },
            onDismiss = { showLogoutConfirm = false },
        )
    }

    SettingActionRow(
        title =
            stringResource(
                if (isLoggedIn) {
                    Res.string.settings_account_connected_title
                } else {
                    Res.string.settings_account_disconnected_title
                },
            ),
        subtitle =
            stringResource(
                if (isLoggedIn) {
                    Res.string.settings_account_connected_desc
                } else {
                    Res.string.settings_account_disconnected_desc
                },
            ),
    ) {
        if (isLoggedIn) {
            SettingActionButton(
                text = stringResource(Res.string.settings_logout_action),
                onClick = { showLogoutConfirm = true },
                emphasis = SettingActionEmphasis.DESTRUCTIVE,
                icon = Icons.AutoMirrored.Filled.Logout,
                modifier = Modifier.actionWidth(),
            )
        } else {
            SettingActionButton(
                text = stringResource(Res.string.settings_login_action),
                onClick = onOpenLogin,
                emphasis = SettingActionEmphasis.PRIMARY,
                icon = Icons.AutoMirrored.Filled.Login,
                modifier = Modifier.actionWidth(),
            )
        }
    }
}
