package com.github.adriianh.cli.tui

import dev.tamboui.toolkit.Toolkit.list
import dev.tamboui.toolkit.Toolkit.markupTextArea
import dev.tamboui.toolkit.elements.ListElement

/**
 * Builders for the ListElements owned by [MeloScreen]. Each builder creates a
 * fresh element; the screen stores the resulting instance in a property so the
 * element's internal state (selection, items) is preserved between calls.
 */

internal fun buildHomeRecentList(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .focusable()
    .id("home-recent-list")

internal fun buildHomeFavoritesList(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .focusable()
    .id("home-favorites-list")

internal fun buildResultList(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .scrollbar()

internal fun buildFavoritesList(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .scrollbar()
    .focusable()
    .id("library-list")

internal fun buildPlaylistsList(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .scrollbar()
    .focusable()
    .id("playlists-list")

internal fun buildPlaylistTracksList(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .scrollbar()
    .focusable()
    .id("playlist-tracks-list")

internal fun buildEntityTracksList(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .focusable()
    .id("entity-tracks-list")

internal fun buildArtistDashboardList(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .scrollbar()
    .focusable()
    .id("artist-dashboard-list")

internal fun buildLocalLibraryList(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .scrollbar()
    .focusable()
    .id("local-library-list")

internal fun buildHomeFeedSectionList(): ListElement<*> = list()
    .highlightSymbol("")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .scrollbar()
    .focusable()
    .id("home-feed-sections")

internal fun buildHomeFeedItemList(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .scrollbar()
    .focusable()
    .id("home-feed-items")

internal fun buildSidebarNavList(): ListElement<*> = list()
    .items(
        "${MeloTheme.ICON_HOME} Home",
        "${MeloTheme.ICON_SEARCH} Search",
        "${MeloTheme.ICON_LIBRARY} Your Library",
        "${MeloTheme.ICON_NOW_PLAYING} Now Playing",
    )
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .selected(SidebarSection.HOME.ordinal)

internal fun buildSidebarUtilList(): ListElement<*> = list()
    .items(
        "${MeloTheme.ICON_STATS} Statistics",
        "${MeloTheme.ICON_OFFLINE} Downloads",
        "${MeloTheme.ICON_SETTINGS} Settings",
    )
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .selected(-1)

internal fun buildLyricsArea() = markupTextArea()
    .scrollbar()
    .wrapWord()
    .focusable()
    .id("lyrics-area")

internal fun buildEntityDescriptionArea() = markupTextArea()
    .scrollbar()
    .wrapWord()
    .focusable()
    .id("desc-area")

internal fun buildSimilarArea(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_BULLET} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .scrollbar()
    .focusable()
    .id("similar-area")

internal fun buildQueueList(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .scrollbar()
    .focusable()
    .id("queue-list")

internal fun buildOfflineList(): ListElement<*> = list()
    .highlightSymbol("${MeloTheme.ICON_ARROW} ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .scrollbar()
    .focusable()
    .id("offline-list")

internal fun buildSettingsSectionList(): ListElement<*> = list()
    .highlightSymbol("> ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .focusable()
    .id("settings-section-list")

internal fun buildSettingsList(): ListElement<*> = list()
    .highlightSymbol("> ")
    .highlightColor(MeloTheme.PRIMARY_COLOR)
    .autoScroll()
    .focusable()
    .id("settings-list")