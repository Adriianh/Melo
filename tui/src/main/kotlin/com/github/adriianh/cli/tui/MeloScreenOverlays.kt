package com.github.adriianh.cli.tui

import com.github.adriianh.cli.tui.component.CommandBarSuggestionsOverlay
import com.github.adriianh.cli.tui.component.DirectoryPickerOverlay
import com.github.adriianh.cli.tui.component.LanguagePickerOverlay
import com.github.adriianh.cli.tui.component.PlaylistInputOverlay
import com.github.adriianh.cli.tui.component.PlaylistPickerOverlay
import com.github.adriianh.cli.tui.component.QueueOverlay
import com.github.adriianh.cli.tui.component.SearchSuggestionsOverlay
import com.github.adriianh.cli.tui.component.SettingsOverlay
import com.github.adriianh.cli.tui.component.ToastOverlay
import com.github.adriianh.cli.tui.component.TrackOptionsOverlay
import com.github.adriianh.cli.tui.handler.playback.handleQueueKey
import com.github.adriianh.cli.tui.handler.playback.handleTrackOptionsKey
import com.github.adriianh.cli.tui.handler.search.handleLanguagePickerKey
import com.github.adriianh.cli.tui.handler.settings.handleSettingsKey

/**
 * Builders for the overlay elements owned by [MeloScreen]. Each captures the
 * screen receiver (for state access) and any key handler references.
 */

internal fun MeloScreen.buildPlaylistInputOverlay() = PlaylistInputOverlay { state }

internal fun MeloScreen.buildSearchSuggestionsOverlay() = SearchSuggestionsOverlay { state }

internal fun MeloScreen.buildPlaylistPickerOverlay() = PlaylistPickerOverlay { state }

internal fun MeloScreen.buildQueueOverlay() = QueueOverlay(
    { state },
    queueList,
    ::handleQueueKey
)

internal fun MeloScreen.buildSettingsOverlay() = SettingsOverlay(
    { state },
    { settingsViewState },
    settingsSectionList,
    settingsList,
    ::handleSettingsKey
)

internal fun MeloScreen.buildDirectoryPickerOverlay() = DirectoryPickerOverlay(
    { settingsViewState },
    ::handleSettingsKey
)

internal fun MeloScreen.buildTrackOptionsOverlay() = TrackOptionsOverlay(
    { state },
    ::handleTrackOptionsKey
)

internal fun MeloScreen.buildCommandBarSuggestionsOverlay() = CommandBarSuggestionsOverlay { state }

internal fun MeloScreen.buildLanguagePickerOverlay() = LanguagePickerOverlay(
    { state },
    ::handleLanguagePickerKey
)

internal fun MeloScreen.buildToastOverlay() = ToastOverlay { state }