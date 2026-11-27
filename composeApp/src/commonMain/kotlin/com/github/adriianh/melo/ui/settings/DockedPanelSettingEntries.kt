@file:Suppress("FunctionNaming", "LongMethod", "ktlint:standard:function-naming")

package com.github.adriianh.melo.ui.settings

import androidx.compose.runtime.Composable
import com.github.adriianh.core.domain.model.DockedCollapsedArtworkMode
import com.github.adriianh.core.domain.model.DockedInitialTab
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.player_tab_artist
import com.github.adriianh.melo.resources.player_tab_lyrics
import com.github.adriianh.melo.resources.player_tab_queue
import com.github.adriianh.melo.resources.settings_dock_artwork_compact
import com.github.adriianh.melo.resources.settings_dock_artwork_hidden
import com.github.adriianh.melo.resources.settings_dock_collapsed_artwork_desc
import com.github.adriianh.melo.resources.settings_dock_collapsed_artwork_title
import com.github.adriianh.melo.resources.settings_dock_expand_queue_desc
import com.github.adriianh.melo.resources.settings_dock_expand_queue_title
import com.github.adriianh.melo.resources.settings_dock_initial_tab_desc
import com.github.adriianh.melo.resources.settings_dock_initial_tab_title
import com.github.adriianh.melo.resources.settings_dock_show_startup_desc
import com.github.adriianh.melo.resources.settings_dock_show_startup_title
import com.github.adriianh.melo.resources.settings_dock_show_suggestions_desc
import com.github.adriianh.melo.resources.settings_dock_show_suggestions_title
import com.github.adriianh.melo.resources.settings_dock_tab_remember
import com.github.adriianh.melo.resources.settings_kw_dock_collapsed_artwork
import com.github.adriianh.melo.resources.settings_kw_dock_expand_queue
import com.github.adriianh.melo.resources.settings_kw_dock_initial_tab
import com.github.adriianh.melo.resources.settings_kw_dock_show_startup
import com.github.adriianh.melo.resources.settings_kw_dock_show_suggestions
import com.github.adriianh.melo.resources.settings_section_docked_panel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MutableList<SettingEntry>.addDockedPanelEntries(
    settings: Settings,
    actions: SettingsActions,
    categoryTitle: String,
) {
    val section = stringResource(Res.string.settings_section_docked_panel)

    val showStartupTitle = stringResource(Res.string.settings_dock_show_startup_title)
    val showStartupDesc = stringResource(Res.string.settings_dock_show_startup_desc)
    add(
        SettingEntry(
            id = "dock_show_on_startup",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = showStartupTitle,
            description = showStartupDesc,
            keywords = dockKeywords(Res.string.settings_kw_dock_show_startup),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = showStartupTitle,
                subtitle = showStartupDesc,
                checked = settings.dockShowOnStartup,
                onCheckedChange = actions.onDockShowOnStartupToggle,
            )
        },
    )

    val initialTabTitle = stringResource(Res.string.settings_dock_initial_tab_title)
    val initialTabDesc = stringResource(Res.string.settings_dock_initial_tab_desc)
    add(
        SettingEntry(
            id = "dock_initial_tab",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = initialTabTitle,
            description = initialTabDesc,
            keywords = dockKeywords(Res.string.settings_kw_dock_initial_tab),
            categoryTitle = categoryTitle,
        ) {
            val labels =
                mapOf(
                    DockedInitialTab.REMEMBER_LAST to stringResource(Res.string.settings_dock_tab_remember),
                    DockedInitialTab.QUEUE to stringResource(Res.string.player_tab_queue),
                    DockedInitialTab.LYRICS to stringResource(Res.string.player_tab_lyrics),
                    DockedInitialTab.ARTIST to stringResource(Res.string.player_tab_artist),
                )
            SettingChoiceRow(
                title = initialTabTitle,
                subtitle = initialTabDesc,
            ) {
                SettingOptionGrid(
                    options = DockedInitialTab.entries,
                    isSelected = { it == settings.dockInitialTab },
                    onSelect = actions.onDockInitialTabSelected,
                    title = { labels.getValue(it) },
                )
            }
        },
    )

    val expandQueueTitle = stringResource(Res.string.settings_dock_expand_queue_title)
    val expandQueueDesc = stringResource(Res.string.settings_dock_expand_queue_desc)
    add(
        SettingEntry(
            id = "dock_expand_queue_by_default",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = expandQueueTitle,
            description = expandQueueDesc,
            keywords = dockKeywords(Res.string.settings_kw_dock_expand_queue),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = expandQueueTitle,
                subtitle = expandQueueDesc,
                checked = settings.dockExpandQueueByDefault,
                onCheckedChange = actions.onDockExpandQueueByDefaultToggle,
            )
        },
    )

    val showSuggestionsTitle = stringResource(Res.string.settings_dock_show_suggestions_title)
    val showSuggestionsDesc = stringResource(Res.string.settings_dock_show_suggestions_desc)
    add(
        SettingEntry(
            id = "dock_show_suggestions",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = showSuggestionsTitle,
            description = showSuggestionsDesc,
            keywords = dockKeywords(Res.string.settings_kw_dock_show_suggestions),
            categoryTitle = categoryTitle,
        ) {
            SettingToggleRow(
                title = showSuggestionsTitle,
                subtitle = showSuggestionsDesc,
                checked = settings.dockShowSuggestions,
                onCheckedChange = actions.onDockShowSuggestionsToggle,
            )
        },
    )

    val collapsedArtworkTitle = stringResource(Res.string.settings_dock_collapsed_artwork_title)
    val collapsedArtworkDesc = stringResource(Res.string.settings_dock_collapsed_artwork_desc)
    add(
        SettingEntry(
            id = "dock_collapsed_artwork_mode",
            category = SettingsCategory.NOW_PLAYING,
            section = section,
            title = collapsedArtworkTitle,
            description = collapsedArtworkDesc,
            keywords = dockKeywords(Res.string.settings_kw_dock_collapsed_artwork),
            categoryTitle = categoryTitle,
        ) {
            val labels =
                mapOf(
                    DockedCollapsedArtworkMode.COMPACT to
                        stringResource(Res.string.settings_dock_artwork_compact),
                    DockedCollapsedArtworkMode.HIDDEN to
                        stringResource(Res.string.settings_dock_artwork_hidden),
                )
            SettingChoiceRow(
                title = collapsedArtworkTitle,
                subtitle = collapsedArtworkDesc,
            ) {
                SettingOptionGrid(
                    options = DockedCollapsedArtworkMode.entries,
                    isSelected = { it == settings.dockCollapsedArtworkMode },
                    onSelect = actions.onDockCollapsedArtworkModeSelected,
                    title = { labels.getValue(it) },
                )
            }
        },
    )
}

@Composable
private fun dockKeywords(res: StringResource): List<String> =
    stringResource(res)
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
