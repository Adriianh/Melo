package com.github.adriianh.cli.tui.component.screen

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SearchTab
import com.github.adriianh.cli.tui.SidebarSection
import com.github.adriianh.cli.tui.component.buildCommandBar
import com.github.adriianh.cli.tui.component.buildDetailPanel
import com.github.adriianh.cli.tui.component.buildEntityDetailPanel
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
import com.github.adriianh.cli.tui.handler.playback.handleNowPlayingKey
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
import com.github.adriianh.cli.tui.handler.toggleDetailPanel
import com.github.adriianh.cli.tui.screen.renderEntityDetailScreen
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
        ::toggleShuffle, ::cycleRepeat, ::toggleQueue, ::toggleDetailPanel,
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

    val terminalSize = try {
        appRunner()?.tuiRunner()?.terminal()?.size()
    } catch (_: Exception) {
        null
    }
    val terminalWidth = terminalSize?.width() ?: 120
    val terminalHeight = terminalSize?.height() ?: 30

    val minDetailWidth = if (state.screen is ScreenState.Home) 135 else 100
    val canShowDetail = state.detail.isVisible &&
            terminalWidth >= minDetailWidth &&
            state.screen !is ScreenState.NowPlaying &&
            state.screen !is ScreenState.EntityDetail

    val detailElement: Element? = if (canShowDetail) {
        if (state.screen is ScreenState.Search) {
            val actualSearch = state.screen as ScreenState.Search
            val isPlayable = actualSearch.tab == SearchTab.SONGS
            if (isPlayable && state.detail.selectedTrack != null) {
                buildDetailPanel(state, lyricsArea, similarArea, ::handleDetailKey, terminalHeight)
            } else if (!isPlayable && state.detail.selectedEntity != null) {
                buildEntityDetailPanel(state, entityDescriptionArea, ::handleEntityDetailKey)
            } else if (state.player.nowPlaying != null) {
                buildDetailPanel(state, lyricsArea, similarArea, ::handleDetailKey, terminalHeight)
            } else null
        } else {
            val track = state.detail.selectedTrack ?: state.player.nowPlaying
            if (track != null) {
                buildDetailPanel(state, lyricsArea, similarArea, ::handleDetailKey, terminalHeight)
            } else null
        }
    } else null

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

    val dockWithBottom = layoutDock
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

    val dockWithRight = if (detailElement != null) {
        val detailConstraint = if (terminalWidth < 120) Constraint.percentage(30) else Constraint.percentage(33)
        dockWithBottom.right(detailElement, detailConstraint)
    } else {
        dockWithBottom
    }

    val mainLayout = dockWithRight.center(renderMainContentInternal(terminalWidth, terminalHeight))

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

    val withPlaylist = when (state.playlistInteraction.playlistInputMode) {
        PlaylistInputMode.CREATE,
        PlaylistInputMode.RENAME -> stack(withCommandBarSuggestions, playlistInputOverlay)

        PlaylistInputMode.PICKER -> stack(withCommandBarSuggestions, playlistPickerOverlay)
        PlaylistInputMode.NONE -> withCommandBarSuggestions
    }

    return if (state.languagePicker.isVisible) stack(
        withPlaylist,
        languagePickerOverlay
    ) else withPlaylist
}

internal fun MeloScreen.renderMainContentInternal(
    terminalWidth: Int = 120,
    terminalHeight: Int = 30
): Element {
    return when (state.screen) {
        is ScreenState.Home -> renderHomeScreen(
            state,
            homeFeedSectionList,
            homeFeedItemList,
            homeRecentList,
            homeFavoritesList,
            onKeyEvent = ::handleHomeKey,
        )

        is ScreenState.Search -> {
            renderSearchScreen(
                state,
                resultList,
                ::marqueeText,
                ::handleResultsKey,
            )
        }

        is ScreenState.Library -> renderLibraryScreen(
            state,
            settingsViewState,
            favoritesList,
            playlistsList,
            playlistTracksList,
            localLibraryList,
            ::handleLibraryKey,
            terminalWidth,
        )

        is ScreenState.NowPlaying -> renderNowPlayingScreen(
            state,
            ::marqueeText,
            ::handleNowPlayingKey,
            terminalHeight,
        )

        is ScreenState.Stats -> renderStatsScreen(state, ::handleStatsKey)
        is ScreenState.Offline -> renderOfflineScreen(state, offlineList, ::handleOfflineKey)
        is ScreenState.EntityDetail -> renderEntityDetailScreen(
            state,
            entityTracksList,
            artistDashboardList,
            entityDescriptionArea,
            ::marqueeText,
            ::handleEntityDetailKey,
        )
    }
}