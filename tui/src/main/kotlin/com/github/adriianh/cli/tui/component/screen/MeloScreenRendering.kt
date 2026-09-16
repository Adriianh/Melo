package com.github.adriianh.cli.tui.component.screen

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SidebarSection
import com.github.adriianh.cli.tui.component.buildCommandBar
import com.github.adriianh.cli.tui.component.buildPlayerBar
import com.github.adriianh.cli.tui.component.buildSearchBar
import com.github.adriianh.cli.tui.component.buildSidebar
import com.github.adriianh.cli.tui.handler.CommandBarHandlers.handleCommandBarKey
import com.github.adriianh.cli.tui.handler.handleHomeKey
import com.github.adriianh.cli.tui.handler.handleLibraryKey
import com.github.adriianh.cli.tui.handler.handleOfflineKey
import com.github.adriianh.cli.tui.handler.handleSidebarKey
import com.github.adriianh.cli.tui.handler.handleStatsKey
import com.github.adriianh.cli.tui.handler.playback.adjustVolume
import com.github.adriianh.cli.tui.handler.playback.cycleRepeat
import com.github.adriianh.cli.tui.handler.playback.handlePlayerBarKey
import com.github.adriianh.cli.tui.handler.playback.seekBackward
import com.github.adriianh.cli.tui.handler.playback.seekForward
import com.github.adriianh.cli.tui.handler.playback.togglePlayPause
import com.github.adriianh.cli.tui.handler.playback.toggleQueue
import com.github.adriianh.cli.tui.handler.playback.toggleShuffle
import com.github.adriianh.cli.tui.handler.search.handleDetailKey
import com.github.adriianh.cli.tui.handler.search.handleEntityDetailKey
import com.github.adriianh.cli.tui.handler.search.handleResultsKey
import com.github.adriianh.cli.tui.handler.search.handleSearchBarKey
import com.github.adriianh.cli.tui.handler.search.performSearch
import com.github.adriianh.cli.tui.screen.renderHomeScreen
import com.github.adriianh.cli.tui.screen.renderLibraryScreen
import com.github.adriianh.cli.tui.screen.renderNowPlayingScreen
import com.github.adriianh.cli.tui.screen.renderOfflineScreen
import com.github.adriianh.cli.tui.screen.renderSearchScreen
import com.github.adriianh.cli.tui.screen.renderStatsScreen
import com.github.adriianh.cli.tui.util.TextAnimationUtil.marqueeText
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import dev.tamboui.layout.Constraint
import dev.tamboui.toolkit.Toolkit.dock
import dev.tamboui.toolkit.Toolkit.stack
import dev.tamboui.toolkit.element.Element

internal fun MeloScreen.renderRoot(): Element {
    val playerBar = buildPlayerBar(
        state, ::formatDuration, ::handlePlayerBarKey,
        ::togglePlayPause, ::adjustVolume, ::seekForward, ::seekBackward,
        ::toggleShuffle, ::cycleRepeat, ::toggleQueue,
    )

    val bottomContent = if (state.commandBar.isVisible) {
        dock()
            .top(playerBar, Constraint.length(4))
            .bottom(
                buildCommandBar(state) {
                    handleCommandBarKey(it)
                }, Constraint.length(1)
            )
    } else {
        playerBar
    }

    val isSearch =
        state.navigation.activeSection == SidebarSection.SEARCH && state.screen is ScreenState.Search

    val layoutDock = dock()
    if (isSearch) {
        layoutDock.top(
            buildSearchBar(
                searchInputState,
                state.screen as? ScreenState.Search,
                ::performSearch,
                ::handleSearchBarKey
            ),
            Constraint.length(3)
        )
    }

    val mainLayout = layoutDock
        .bottom(
            bottomContent,
            Constraint.length(if (state.commandBar.isVisible) 5 else 4),
        )
        .left(
            buildSidebar(
                sidebarNavList,
                sidebarUtilList,
                state.navigation.sidebarInUtil,
                ::handleSidebarKey
            ),
            Constraint.length(22)
        )
        .center(renderMainContentInternal())

    val withQueue = if (state.player.isQueueVisible) stack(mainLayout, queueOverlay) else mainLayout
    val withSettings = if (state.isSettingsVisible) stack(withQueue, settingsOverlay) else withQueue
    val withDirectoryPicker = if (settingsViewState.isPickingDirectory)
        stack(withSettings, directoryPickerOverlay) else withSettings
    val withTrackOptions =
        if (state.trackOptions.isVisible) stack(
            withDirectoryPicker,
            trackOptionsOverlay
        ) else withDirectoryPicker

    val withSearchSuggestions =
        if (state.screen is ScreenState.Search && (state.screen as ScreenState.Search).isShowingSuggestions && (state.screen as ScreenState.Search).searchSuggestions.isNotEmpty())
            stack(withTrackOptions, searchSuggestionsOverlay)
        else withTrackOptions

    val withCommandBarSuggestions =
        if (state.commandBar.isVisible && state.commandBar.suggestions.isNotEmpty())
            stack(withSearchSuggestions, commandBarSuggestionsOverlay)
        else withSearchSuggestions

    return when (state.playlistInteraction.playlistInputMode) {
        PlaylistInputMode.CREATE,
        PlaylistInputMode.RENAME -> stack(withCommandBarSuggestions, playlistInputOverlay)

        PlaylistInputMode.PICKER -> stack(withCommandBarSuggestions, playlistPickerOverlay)
        PlaylistInputMode.NONE -> withCommandBarSuggestions
    }
}

internal fun MeloScreen.renderMainContentInternal(): Element {
    return when (state.screen) {
        is ScreenState.Home -> renderHomeScreen(
            state,
            homeFeedSectionList,
            homeFeedItemList,
            homeRecentList,
            homeFavoritesList,
            onKeyEvent = ::handleHomeKey,
        )

        is ScreenState.Search -> renderSearchScreen(
            state,
            resultList,
            entityTracksList,
            artistDashboardList,
            lyricsArea,
            similarArea,
            entityDescriptionArea,
            ::marqueeText,
            ::handleResultsKey,
            ::handleEntityDetailKey,
            ::handleDetailKey,
        )

        is ScreenState.Library -> renderLibraryScreen(
            state,
            settingsViewState,
            favoritesList,
            playlistsList,
            playlistTracksList,
            localLibraryList,
            ::handleLibraryKey,
        )

        is ScreenState.NowPlaying -> renderNowPlayingScreen(
            state,
            ::marqueeText,
            ::handlePlayerBarKey
        )

        is ScreenState.Stats -> renderStatsScreen(state, ::handleStatsKey)
        is ScreenState.Offline -> renderOfflineScreen(state, offlineList, ::handleOfflineKey)
    }
}