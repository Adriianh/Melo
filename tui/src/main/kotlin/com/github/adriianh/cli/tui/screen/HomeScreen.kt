package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.HomeFeedFocus
import com.github.adriianh.cli.tui.HomeTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_BLUE
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_RED
import com.github.adriianh.cli.tui.MeloTheme.BG_DARK
import com.github.adriianh.cli.tui.MeloTheme.BG_ELEVATED
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_ARROW
import com.github.adriianh.cli.tui.MeloTheme.ICON_CLOCK
import com.github.adriianh.cli.tui.MeloTheme.ICON_ERROR
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_LOADING
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.SECONDARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.allLibraryFavorites
import com.github.adriianh.cli.tui.allRecentTracks
import com.github.adriianh.cli.tui.isFavoriteTrack
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.cli.tui.util.TextMessagesUtil.buildGreeting
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.dock
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun renderHomeScreen(
    state: MeloState,
    feedSectionList: ListElement<*>,
    feedItemList: ListElement<*>,
    recentList: ListElement<*>,
    favoritesList: ListElement<*>,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val s = state.screen as? ScreenState.Home
        ?: return panel(text("Home not active").centered()).rounded()

    val feedTab = tabPill("1: Explore Feed", s.homeTab == HomeTab.FEED)
    val recentTab = tabPill("2: Recently Played", s.homeTab == HomeTab.RECENT)
    val favTab = tabPill("3: Favorites", s.homeTab == HomeTab.FAVORITES)

    val userBadge = if (state.youtubeAccountName != null) {
        text("✓ ${state.youtubeAccountName}  ").fg(ACCENT_BLUE).bold()
    } else {
        text("○ Guest  ").fg(TEXT_DIM)
    }

    val headerRow = row(
        text(buildGreeting()).bold().fg(PRIMARY_COLOR),
        spacer(),
        userBadge,
        feedTab,
        text("  ").length(2),
        recentTab,
        text("  ").length(2),
        favTab
    ).margin(Margin.horizontal(1)).length(1)

    val content: StyledElement<*> = when (s.homeTab) {
        HomeTab.FEED -> renderFeedTab(state, s, feedSectionList, feedItemList)
        HomeTab.RECENT -> renderRecentTab(state, s, recentList)
        HomeTab.FAVORITES -> renderFavoritesTab(state, s, favoritesList)
    }

    val footerText = when {
        state.selection.isNotEmpty -> "[Space/v] Toggle  [Ctrl+A] All  [m] Batch (${state.selection.count})  [Esc] Clear"
        s.homeTab == HomeTab.FEED -> when (s.feedFocus) {
            HomeFeedFocus.CHIPS -> "[Tab] Focus  [←→] Moods  [Enter] Apply filter  [↓] Sections  [R] Refresh"
            HomeFeedFocus.SECTIONS -> "[Tab] Focus  [↑↓] Sections  [→/Enter] Items  [[/]] Jump  [1..3] Tabs  [R] Refresh"
            HomeFeedFocus.ITEMS -> "[Tab] Focus  [↑↓] Move  [v] Select  [←/Esc] Sections  [[/]] Jump  [Enter] Play  [Q] Queue  [F] Fav  [M] Options"
        }

        else -> "[1..3] Tabs  [Tab] Switch Tab  [↑↓] Move  [v] Select  [Enter] Play  [Q] Queue  [F] Favorite  [R] Refresh"
    }

    val footer = row(
        spacer(),
        text(footerText)
            .fg(TEXT_DIM)
            .ellipsis(),
        spacer()
    ).length(1)

    return panel(
        column(
            headerRow,
            text("").length(1),
            content.fill(),
            text("").length(1),
            footer
        )
    ).title(" Home ")
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("home-panel")
        .onKeyEvent(onKeyEvent)
        .fill()
}

private fun tabPill(label: String, active: Boolean): Element =
    if (active) {
        text(" $label ").bold().fg(TEXT_PRIMARY).bg(PRIMARY_COLOR)
    } else {
        text(" $label ").fg(TEXT_DIM)
    }

private fun getSectionIcon(title: String): String {
    if (!MeloTheme.supportsUnicode) return "•"
    val lower = title.lowercase()
    return when {
        lower.contains("quick") || lower.contains("rápida") || lower.contains("picks") -> "▶"
        lower.contains("again") || lower.contains("otra vez") || lower.contains("repetir") || lower.contains(
            "volver"
        ) -> "↻"

        lower.contains("mixed") || lower.contains("mix") || lower.contains("mezcla") -> "★"
        lower.contains("library") || lower.contains("biblioteca") || lower.contains("tu música") -> "≡"
        lower.contains("similar") || lower.contains("parecido") || lower.contains("radio") -> "~"
        lower.contains("artist") || lower.contains("artista") -> "●"
        lower.contains("trend") || lower.contains("popular") || lower.contains("éxito") || lower.contains(
            "chart"
        ) -> "▲"

        lower.contains("new") || lower.contains("nuevo") || lower.contains("lanzamiento") -> "✦"
        lower.contains("relax") || lower.contains("chill") || lower.contains("sleep") || lower.contains(
            "dormir"
        ) -> "☾"

        lower.contains("workout") || lower.contains("gym") || lower.contains("entrenar") || lower.contains(
            "ejercicio"
        ) -> "■"

        lower.contains("focus") || lower.contains("concentra") || lower.contains("estudiar") -> "◎"
        lower.contains("party") || lower.contains("fiesta") || lower.contains("energ") -> "◆"
        lower.contains("feel good") || lower.contains("ánimo") -> "☼"
        lower.contains("commute") || lower.contains("viaje") -> "»"
        else -> "♪"
    }
}

private fun renderFeedTab(
    state: MeloState,
    s: ScreenState.Home,
    feedSectionList: ListElement<*>,
    feedItemList: ListElement<*>
): StyledElement<*> {
    if (s.isLoadingFeed) {
        return column(
            spacer(),
            text("$ICON_LOADING  Loading music recommendations...").fg(PRIMARY_COLOR).centered(),
            spacer()
        )
    }

    if (s.feedError != null) {
        return column(
            spacer(),
            text("$ICON_ERROR  ${s.feedError}").fg(ACCENT_RED).centered(),
            text("Press R to retry").fg(TEXT_DIM).centered(),
            spacer()
        )
    }

    if (s.feedSections.isEmpty()) {
        return column(
            spacer(),
            text("No recommendations found").fg(TEXT_SECONDARY).centered(),
            text("Press R to reload feed or search music").fg(TEXT_DIM).centered(),
            if (state.youtubeAccountName == null) {
                text("Tip: Log in to YouTube Music for personalized feeds [melo auth youtube import]").fg(
                    TEXT_DIM
                ).centered()
            } else {
                text("").length(1)
            },
            spacer()
        )
    }

    val sectionItems = s.feedSections.mapIndexed { index, section ->
        val isSelected = index == s.selectedSectionIndex
        val isSectionFocused = s.feedFocus == HomeFeedFocus.SECTIONS
        val icon = getSectionIcon(section.title)
        val count = section.items.size
        row(
            text(if (isSelected && isSectionFocused) "$ICON_ARROW " else if (isSelected) "● " else "  ")
                .fg(PRIMARY_COLOR).length(2),
            text("$icon ").length(2),
            text(section.title).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                .apply { if (isSelected) bold() }.ellipsis().fill(),
            text("$count").fg(TEXT_DIM).length(count.toString().length + 1)
        )
    }
    feedSectionList.elements(*sectionItems.toTypedArray())
    feedSectionList.selected(s.selectedSectionIndex)

    val leftPanelTitle =
        if (s.isLoadingMoreSections) " Sections (${s.feedSections.size}...) " else " Sections (${s.feedSections.size}) "
    val leftPanel = panel(
        feedSectionList.fill()
    ).title(leftPanelTitle)
        .rounded()
        .borderColor(if (s.feedFocus == HomeFeedFocus.SECTIONS) BORDER_FOCUSED else BORDER_DEFAULT)
        .fill()

    val currentSection = s.feedSections.getOrNull(s.selectedSectionIndex)
    val sectionTitle = currentSection?.title ?: "Items"
    val sectionIcon = getSectionIcon(sectionTitle)
    val rawItems = currentSection?.items.orEmpty()

    val itemElements = rawItems.mapIndexed { index, item ->
        val isSelected = index == s.selectedItemIndex
        when (item) {
            is SearchResult.Song -> {
                val track = item.track
                val isPlaying = track.id == state.player.nowPlaying?.id
                val isPlayable = state.isPlayable(track)
                val indicator = if (isPlaying) "$ICON_NOTE " else "${index + 1} "
                val isFav = state.isFavoriteTrack(track)
                val isBatchSelected = state.selection.isSelected(track.id)
                val rowElements = mutableListOf<Element>()
                if (state.selection.isNotEmpty) {
                    rowElements.add(
                        text(if (isBatchSelected) "[x] " else "[ ] ")
                            .fg(if (isBatchSelected) PRIMARY_COLOR else TEXT_DIM)
                            .length(4)
                    )
                }
                rowElements.add(
                    text(indicator).fg(if (isPlaying) PRIMARY_COLOR else TEXT_DIM).length(3)
                )
                rowElements.add(text("♪ ").fg(PRIMARY_COLOR).length(2))
                rowElements.add(
                    text(track.title).fg(if (isSelected) PRIMARY_COLOR else if (isPlayable) TEXT_PRIMARY else TEXT_DIM)
                        .apply { if (isPlaying || isSelected) bold() }.ellipsisMiddle().fill()
                )
                rowElements.add(text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25))
                rowElements.add(text(if (isFav) ICON_HEART else " ").fg(PRIMARY_COLOR).length(2))
                rowElements.add(
                    text(if (track.durationMs > 0L) formatDuration(track.durationMs) else "—").fg(
                        TEXT_DIM
                    ).length(6)
                )
                row(*rowElements.toTypedArray())
            }

            is SearchResult.Album -> {
                val rowElements = mutableListOf<Element>()
                if (state.selection.isNotEmpty) {
                    rowElements.add(text("    ").length(4))
                }
                rowElements.add(text("${index + 1} ").dim().length(3))
                rowElements.add(text("◎ ").fg(ACCENT_BLUE).length(2))
                rowElements.add(
                    text(item.title).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                        .apply { if (isSelected) bold() }.ellipsisMiddle().fill()
                )
                rowElements.add(text(item.author).fg(TEXT_SECONDARY).ellipsis().percent(25))
                rowElements.add(text(" ").length(2))
                val countStr = if (!item.songs.isNullOrEmpty()) "${item.songs!!.size} tr" else "—"
                rowElements.add(text(countStr).fg(TEXT_DIM).length(6))
                row(*rowElements.toTypedArray())
            }

            is SearchResult.Playlist -> {
                val countText = when {
                    item.trackCount != null && item.trackCount!! > 0 -> "${item.trackCount} tr"
                    !item.songs.isNullOrEmpty() -> "${item.songs!!.size} tr"
                    else -> "—"
                }
                val rowElements = mutableListOf<Element>()
                if (state.selection.isNotEmpty) {
                    rowElements.add(text("    ").length(4))
                }
                rowElements.add(text("${index + 1} ").dim().length(3))
                rowElements.add(text("≡ ").fg(SECONDARY_COLOR).length(2))
                rowElements.add(
                    text(item.title).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                        .apply { if (isSelected) bold() }.ellipsisMiddle().fill()
                )
                rowElements.add(
                    text(item.author.ifBlank { "Curator" }).fg(TEXT_SECONDARY).ellipsis()
                        .percent(25)
                )
                rowElements.add(text(" ").length(2))
                rowElements.add(text(countText).fg(TEXT_DIM).length(6))
                row(*rowElements.toTypedArray())
            }

            is SearchResult.Artist -> {
                val rowElements = mutableListOf<Element>()
                if (state.selection.isNotEmpty) {
                    rowElements.add(text("    ").length(4))
                }
                rowElements.add(text("${index + 1} ").dim().length(3))
                rowElements.add(text("● ").fg(SECONDARY_COLOR).length(2))
                rowElements.add(
                    text(item.name).fg(if (isSelected) PRIMARY_COLOR else TEXT_PRIMARY)
                        .apply { if (isSelected) bold() }.ellipsis().fill()
                )
                rowElements.add(text("Artist").fg(TEXT_SECONDARY).ellipsis().percent(25))
                rowElements.add(text(" ").length(2))
                rowElements.add(text("—").fg(TEXT_DIM).length(6))
                row(*rowElements.toTypedArray())
            }
        }
    }
    feedItemList.elements(*itemElements.toTypedArray())
    if (rawItems.isNotEmpty()) {
        feedItemList.selected(s.selectedItemIndex)
    }

    val headerElements = mutableListOf<Element>()
    if (state.selection.isNotEmpty) {
        headerElements.add(text("Sel ").dim().length(4))
    }
    headerElements.add(text("#").dim().length(3))
    headerElements.add(text(" ").length(2))
    headerElements.add(text("Title").dim().fill())
    headerElements.add(text("Artist / Curator").dim().percent(25))
    headerElements.add(text(ICON_HEART).dim().length(2))
    headerElements.add(text("Time").dim().length(6))
    val tableHeader = row(*headerElements.toTypedArray()).length(1)

    val bottomBar = when (val selectedItem = rawItems.getOrNull(s.selectedItemIndex)) {
        is SearchResult.Song -> {
            val t = selectedItem.track
            val isPlaying = t.id == state.player.nowPlaying?.id
            val isFav = state.isFavoriteTrack(t)
            row(
                text(" $ICON_NOTE ").fg(PRIMARY_COLOR).length(3),
                text(t.title).bold().fg(TEXT_PRIMARY).ellipsisMiddle(),
                text(" by ${t.artist}").fg(TEXT_SECONDARY).ellipsis(),
                if (t.album.isNotBlank()) text(" • ${t.album}").dim().ellipsis() else text(""),
                if (t.durationMs > 0L) text(" • ${formatDuration(t.durationMs)}").dim() else text(""),
                if (isFav) text(" $ICON_HEART").fg(PRIMARY_COLOR) else text(""),
                if (isPlaying) text(" [Now Playing]").fg(PRIMARY_COLOR) else text(""),
                spacer(),
                text("[Enter] Play  [Q] Queue  [F] Fav  [O] Options ").dim()
            ).length(1)
        }
        is SearchResult.Album -> {
            row(
                text(" ◎ ").fg(ACCENT_BLUE).length(3),
                text(selectedItem.title).bold().fg(TEXT_PRIMARY).ellipsisMiddle(),
                text(" by ${selectedItem.author}").fg(TEXT_SECONDARY).ellipsis(),
                if (selectedItem.year != null) text(" (${selectedItem.year})").dim() else text(""),
                spacer(),
                text("[Enter] Open Album tracks ").dim()
            ).length(1)
        }
        is SearchResult.Playlist -> {
            row(
                text(" ≡ ").fg(SECONDARY_COLOR).length(3),
                text(selectedItem.title).bold().fg(TEXT_PRIMARY).ellipsisMiddle(),
                if (selectedItem.author.isNotBlank()) text(" by ${selectedItem.author}").fg(
                    TEXT_SECONDARY
                ).ellipsis() else text(""),
                spacer(),
                text("[Enter] Open Playlist tracks ").dim()
            ).length(1)
        }
        is SearchResult.Artist -> {
            row(
                text(" ● ").fg(SECONDARY_COLOR).length(3),
                text(selectedItem.name).bold().fg(TEXT_PRIMARY).ellipsis(),
                spacer(),
                text("[Enter] View Artist profile ").dim()
            ).length(1)
        }

        null -> row(text("  No item selected").dim()).length(1)
    }

    val rightPanelContent = if (rawItems.isEmpty()) {
        column(
            spacer(),
            text("No items in this section").fg(TEXT_SECONDARY).centered(),
            spacer()
        )
    } else {
        column(
            tableHeader,
            text("").length(1),
            feedItemList.fill(),
            text("").length(1),
            bottomBar
        )
    }

    val rightPanel = panel(
        rightPanelContent
    ).title(" $sectionIcon $sectionTitle (${rawItems.size}) ")
        .rounded()
        .borderColor(if (s.feedFocus == HomeFeedFocus.ITEMS) BORDER_FOCUSED else BORDER_DEFAULT)
        .fill()

    val masterDetail = dock()
        .left(leftPanel, Constraint.length(22))
        .center(rightPanel)
        .fill()

    if (s.feedChips.isNotEmpty()) {
        val isChipsFocused = s.feedFocus == HomeFeedFocus.CHIPS
        val chipPills = mutableListOf<Element>()
        chipPills.add(text("  Moods: ").dim())

        val isAllSelected = s.selectedChipIndex == 0
        val allPill = if (isAllSelected) {
            if (isChipsFocused) text(" ● All ").bold().fg(BG_DARK).bg(PRIMARY_COLOR)
            else text(" All ").bold().fg(PRIMARY_COLOR).bg(BG_ELEVATED)
        } else {
            if (isChipsFocused) text(" All ").fg(TEXT_SECONDARY)
            else text(" All ").fg(TEXT_DIM)
        }
        chipPills.add(allPill)
        chipPills.add(text(" ").length(1))

        s.feedChips.forEachIndexed { idx, chip ->
            val chipIdx = idx + 1
            val isSelected = s.selectedChipIndex == chipIdx
            val pill = if (isSelected) {
                if (isChipsFocused) text(" ● ${chip.title} ").bold().fg(BG_DARK).bg(PRIMARY_COLOR)
                else text(" ${chip.title} ").bold().fg(PRIMARY_COLOR).bg(BG_ELEVATED)
            } else {
                if (isChipsFocused) text(" ${chip.title} ").fg(TEXT_SECONDARY)
                else text(" ${chip.title} ").fg(TEXT_DIM)
            }
            chipPills.add(pill)
            chipPills.add(text(" ").length(1))
        }

        if (isChipsFocused) {
            chipPills.add(spacer())
            chipPills.add(text("[Enter] Filter  [Esc/↓] Sections  ").fg(PRIMARY_COLOR))
        }

        val chipsRow = row(*chipPills.toTypedArray()).length(1)
        return column(
            chipsRow,
            text("").length(1),
            masterDetail.fill()
        )
    }

    return masterDetail
}

private fun buildTrackPreview(track: Track?, state: MeloState, title: String): Element {
    if (track == null) {
        return panel(
            column(
                spacer(),
                text("Select a track to view preview").fg(TEXT_DIM).centered(),
                spacer()
            )
        ).title(" $title ").rounded().borderColor(BORDER_DEFAULT).fill()
    }

    val isPlaying = track.id == state.player.nowPlaying?.id
    val isFav = state.isFavoriteTrack(track)
    val nowPlayingText = if (isPlaying) " $ICON_NOTE Now Playing" else ""

    return panel(
        column(
            row(
                text("[Track]").bold().fg(PRIMARY_COLOR),
                if (isFav) text(" $ICON_HEART Favorited").fg(PRIMARY_COLOR) else text(""),
                if (isPlaying) text(nowPlayingText).fg(PRIMARY_COLOR) else text("")
            ).length(1),
            text(""),
            text(track.title).bold().fg(TEXT_PRIMARY).ellipsisMiddle(),
            text("by ${track.artist}").fg(TEXT_SECONDARY).ellipsis(),
            if (track.album.isNotBlank()) text("Album: ${track.album}").dim()
                .ellipsis() else text(""),
            if (track.durationMs > 0L) text("Duration: ${formatDuration(track.durationMs)}").dim() else text(
                ""
            ),
            spacer(),
            text("────────────────────────────────").fg(BORDER_DEFAULT),
            text("[Enter] Play track").fg(PRIMARY_COLOR),
            text("[Q] Add to queue").fg(TEXT_DIM),
            text("[F] Toggle favorite").fg(TEXT_DIM),
            text("[O] Track options").fg(TEXT_DIM),
        ).margin(Margin.symmetric(1, 1))
    ).title(" $title ").rounded().borderColor(BORDER_DEFAULT).fill()
}

private fun renderRecentTab(
    state: MeloState,
    s: ScreenState.Home,
    recentList: ListElement<*>,
): StyledElement<*> {
    val recentTracks = state.allRecentTracks()
    if (recentTracks.isEmpty()) {
        return column(
            spacer(),
            text("$ICON_CLOCK  No recently played tracks yet").fg(TEXT_SECONDARY).centered(),
            text("Start listening from Explore Feed or Search").fg(TEXT_DIM).centered(),
            spacer()
        )
    }

    val items = recentTracks.mapIndexed { index, entry ->
        val track = entry.track
        val isPlaying = track.id == state.player.nowPlaying?.id
        val isPlayable = state.isPlayable(track)
        val isSelected = index == s.homeRecentCursor
        val isFav = state.isFavoriteTrack(track)
        val isBatchSelected = state.selection.isSelected(track.id)
        val isRemote = state.collections.remoteRecentTracks.any { it.track.id == track.id } &&
                state.collections.recentTracks.none { it.track.id == track.id }
        val sourceIcon = if (isRemote) "☁" else "⌂"
        val sourceColor = if (isRemote) ACCENT_BLUE else PRIMARY_COLOR
        val rowElements = mutableListOf<Element>()
        rowElements.add(text("$sourceIcon ").fg(sourceColor).length(2))
        rowElements.add(text(if (isPlaying) "$ICON_NOTE " else "  ").fg(PRIMARY_COLOR).length(2))
        if (state.selection.isNotEmpty) {
            rowElements.add(
                text(if (isBatchSelected) "[x] " else "[ ] ")
                    .fg(if (isBatchSelected) PRIMARY_COLOR else TEXT_DIM)
                    .length(4)
            )
        }
        rowElements.add(text("${index + 1}").dim().length(3))
        rowElements.add(
            text(track.title).fg(if (isSelected) PRIMARY_COLOR else if (isPlayable) TEXT_PRIMARY else TEXT_DIM)
                .apply { if (isPlaying || isSelected) bold() }.ellipsisMiddle().fill()
        )
        rowElements.add(text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25))
        rowElements.add(text(if (isFav) ICON_HEART else " ").fg(PRIMARY_COLOR).length(2))
        rowElements.add(
            text(if (track.durationMs > 0L) formatDuration(track.durationMs) else "").fg(TEXT_DIM)
                .length(6)
        )
        row(*rowElements.toTypedArray())
    }
    recentList.elements(*items.toTypedArray())
    recentList.selected(s.homeRecentCursor)

    val selectedTrack = recentTracks.getOrNull(s.homeRecentCursor)?.track
    val previewPane = buildTrackPreview(selectedTrack, state, "Recent Track")

    val headerElements = mutableListOf<Element>()
    headerElements.add(text("").length(2))
    headerElements.add(text("").length(2))
    if (state.selection.isNotEmpty) {
        headerElements.add(text("Sel ").dim().length(4))
    }
    headerElements.add(text("#").dim().length(3))
    headerElements.add(text("Title").dim().fill())
    headerElements.add(text("Artist").dim().percent(25))
    headerElements.add(text(ICON_HEART).dim().length(2))
    headerElements.add(text("Time").dim().length(6))

    val centerContent = column(
        row(*headerElements.toTypedArray()).margin(Margin.horizontal(1)),
        text("").length(1),
        recentList.fill()
    ).fill()

    return dock()
        .center(centerContent)
        .right(previewPane, Constraint.percentage(32))
        .fill()
}

private fun renderFavoritesTab(
    state: MeloState,
    s: ScreenState.Home,
    favoritesList: ListElement<*>
): StyledElement<*> {
    val allFavorites = state.allLibraryFavorites()
    if (allFavorites.isEmpty()) {
        return column(
            spacer(),
            text("$ICON_HEART  No favorites saved yet").fg(TEXT_SECONDARY).centered(),
            text("Press F on any track to add it to your favorites").fg(TEXT_DIM).centered(),
            spacer()
        )
    }

    val items = allFavorites.mapIndexed { index, item ->
        val track = item.track
        val isPlaying = track.id == state.player.nowPlaying?.id
        val isPlayable = state.isPlayable(track)
        val isSelected = index == s.homeFavoritesCursor
        val providerColor = if (item.isRemote) ACCENT_BLUE else PRIMARY_COLOR
        val isBatchSelected = state.selection.isSelected(track.id)
        val rowElements = mutableListOf<Element>()
        rowElements.add(text("${item.icon} ").fg(providerColor).length(2))
        rowElements.add(text(if (isPlaying) "$ICON_NOTE " else "  ").fg(PRIMARY_COLOR).length(2))
        if (state.selection.isNotEmpty) {
            rowElements.add(
                text(if (isBatchSelected) "[x] " else "[ ] ")
                    .fg(if (isBatchSelected) PRIMARY_COLOR else TEXT_DIM)
                    .length(4)
            )
        }
        rowElements.add(text("${index + 1}").dim().length(3))
        rowElements.add(
            text(track.title).fg(if (isSelected) PRIMARY_COLOR else if (isPlayable) TEXT_PRIMARY else TEXT_DIM)
                .apply { if (isPlaying || isSelected) bold() }.ellipsisMiddle().fill()
        )
        rowElements.add(text(track.artist).fg(TEXT_SECONDARY).ellipsis().percent(25))
        rowElements.add(text(ICON_HEART).fg(PRIMARY_COLOR).length(2))
        rowElements.add(
            text(if (track.durationMs > 0L) formatDuration(track.durationMs) else "").fg(TEXT_DIM)
                .length(6)
        )
        row(*rowElements.toTypedArray())
    }
    favoritesList.elements(*items.toTypedArray())
    favoritesList.selected(s.homeFavoritesCursor)

    val selectedTrack = allFavorites.getOrNull(s.homeFavoritesCursor)?.track
    val previewPane = buildTrackPreview(selectedTrack, state, "Favorite Track")

    val headerElements = mutableListOf<Element>()
    headerElements.add(text("").length(2))
    headerElements.add(text("").length(2))
    if (state.selection.isNotEmpty) {
        headerElements.add(text("Sel ").dim().length(4))
    }
    headerElements.add(text("#").dim().length(3))
    headerElements.add(text("Title").dim().fill())
    headerElements.add(text("Artist").dim().percent(25))
    headerElements.add(text(ICON_HEART).dim().length(2))
    headerElements.add(text("Time").dim().length(6))

    val centerContent = column(
        row(*headerElements.toTypedArray()).margin(Margin.horizontal(1)),
        text("").length(1),
        favoritesList.fill()
    ).fill()

    return dock()
        .center(centerContent)
        .right(previewPane, Constraint.percentage(32))
        .fill()
}