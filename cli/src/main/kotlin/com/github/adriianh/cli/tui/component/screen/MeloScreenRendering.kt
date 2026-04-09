package com.github.adriianh.cli.tui.component.screen

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.PlaylistInputMode
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.SidebarSection
import com.github.adriianh.cli.tui.component.buildPlayerBar
import com.github.adriianh.cli.tui.component.buildSearchBar
import com.github.adriianh.cli.tui.component.buildSidebar
import com.github.adriianh.cli.tui.graphics.ClearGraphicsElement
import com.github.adriianh.cli.tui.handler.*
import com.github.adriianh.cli.tui.handler.playback.*
import com.github.adriianh.cli.tui.handler.search.handleDetailKey
import com.github.adriianh.cli.tui.handler.search.handleEntityDetailKey
import com.github.adriianh.cli.tui.handler.search.handleResultsKey
import com.github.adriianh.cli.tui.handler.search.handleSearchBarKey
import com.github.adriianh.cli.tui.handler.search.performSearch
import com.github.adriianh.cli.tui.screen.*
import com.github.adriianh.cli.tui.util.TextAnimationUtil.marqueeText
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import dev.tamboui.layout.Constraint
import dev.tamboui.toolkit.Toolkit.dock
import dev.tamboui.toolkit.Toolkit.stack
import dev.tamboui.toolkit.element.Element

internal fun MeloScreen.renderRoot(): Element {
    val mainLayout = dock()
        .top(
            buildSearchBar(searchInputState, ::performSearch, ::handleSearchBarKey),
            Constraint.length(3)
        )
        .bottom(
            buildPlayerBar(
                state, ::formatDuration, ::handlePlayerBarKey,
                ::togglePlayPause, ::adjustVolume, ::seekForward, ::seekBackward,
                ::toggleShuffle, ::cycleRepeat, ::toggleQueue,
            ),
            Constraint.length(4),
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

    return when (state.playlistInteraction.playlistInputMode) {
        PlaylistInputMode.CREATE,
        PlaylistInputMode.RENAME -> stack(withTrackOptions, playlistInputOverlay)

        PlaylistInputMode.PICKER -> stack(withTrackOptions, playlistPickerOverlay)
        PlaylistInputMode.NONE -> withTrackOptions
    }
}

internal fun MeloScreen.renderMainContentInternal(): Element {
    if (state.needsGraphicsClear) {
        val pending = state.navigation.pendingSection
        val targetSection = pending ?: state.navigation.activeSection
        val targetScreen = when (targetSection) {
            SidebarSection.HOME -> ScreenState.Home()
            SidebarSection.SEARCH -> ScreenState.Search()
            SidebarSection.LIBRARY -> ScreenState.Library()
            SidebarSection.NOW_PLAYING -> ScreenState.NowPlaying()
            SidebarSection.STATS -> ScreenState.Stats()
            SidebarSection.OFFLINE -> ScreenState.Offline(downloads = state.collections.offlineTracks)
            SidebarSection.SETTINGS -> state.screen
        }
        state = state.copy(
            needsGraphicsClear = false,
            navigation = state.navigation.copy(
                activeSection = targetSection,
                pendingSection = null
            ),
            screen = targetScreen,
            detail = state.detail.copy(artworkData = if (targetSection != SidebarSection.SEARCH) null else state.detail.artworkData),
        )
        if (targetSection == SidebarSection.NOW_PLAYING) {
            appRunner()?.focusManager()?.setFocus("now-playing-panel")
        }
        val targetContent = when (targetSection) {
            SidebarSection.HOME -> renderHomeScreen(
                state, homeRecentList, homeFavoritesList,
                onKeyEvent = ::handleHomeKey,
            )

            SidebarSection.SEARCH -> renderSearchScreen(
                state, resultList, entityTracksList, lyricsArea, similarArea,
                ::marqueeText,
                ::handleResultsKey,
                ::handleEntityDetailKey,
                ::handleDetailKey,
            )

            SidebarSection.LIBRARY -> renderLibraryScreen(
                state,
                settingsViewState,
                favoritesList,
                playlistsList,
                playlistTracksList,
                localLibraryList,
                ::handleLibraryKey,
            )

            SidebarSection.NOW_PLAYING -> renderNowPlayingScreen(
                state,
                ::marqueeText,
                ::handlePlayerBarKey
            )

            SidebarSection.STATS -> renderStatsScreen(state, ::handleStatsKey)
            SidebarSection.OFFLINE -> renderOfflineScreen(state, offlineList, ::handleOfflineKey)
            SidebarSection.SETTINGS -> renderHomeScreen(
                state,
                homeRecentList,
                homeFavoritesList,
                onKeyEvent = ::handleHomeKey
            )
        }
        return stack(ClearGraphicsElement().fill(), targetContent)
    }

    return when (state.screen) {
        is ScreenState.Home -> renderHomeScreen(
            state, homeRecentList, homeFavoritesList,
            onKeyEvent = ::handleHomeKey,
        )

        is ScreenState.Search -> renderSearchScreen(
            state, resultList, entityTracksList, lyricsArea, similarArea,
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