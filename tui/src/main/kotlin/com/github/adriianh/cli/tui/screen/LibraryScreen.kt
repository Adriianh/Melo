package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.FavoritesSubTab
import com.github.adriianh.cli.tui.LibraryTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_LIBRARY
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.component.SettingsViewState
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun renderLibraryScreen(
    state: MeloState,
    settingsViewState: SettingsViewState,
    favoritesList: ListElement<*>,
    playlistsList: ListElement<*>,
    playlistTracksList: ListElement<*>,
    localLibraryList: ListElement<*>,
    onKeyEvent: (KeyEvent) -> EventResult,
    terminalWidth: Int = 120,
): Element {
    val actualState = state.screen as? ScreenState.Library ?: return panel(text("Library not active").centered()).rounded()

    val favTab = tabLabel("$ICON_HEART Favorites", actualState.libraryTab == LibraryTab.FAVORITES)
    val plTab  = tabLabel("$ICON_LIBRARY Playlists", actualState.libraryTab == LibraryTab.PLAYLISTS)
    val locTab = tabLabel("${MeloTheme.ICON_SEARCH} Local", actualState.libraryTab == LibraryTab.LOCAL)
    val tabBar = row(favTab, text("  "), plTab, text("  "), locTab, spacer())
        .margin(Margin.horizontal(1))

    val content: StyledElement<*> = when (actualState.libraryTab) {
        LibraryTab.FAVORITES -> buildFavoritesContent(
            state,
            actualState,
            favoritesList,
            terminalWidth
        )
        LibraryTab.PLAYLISTS -> if (actualState.isInPlaylistDetail)
            buildPlaylistDetailContent(state, actualState, playlistTracksList)
        else
            buildPlaylistsContent(state, actualState, playlistsList, terminalWidth)

        LibraryTab.LOCAL -> buildLocalContent(state, settingsViewState, actualState, localLibraryList)
    }

    val isCompact = terminalWidth < 90
    val hints = if (actualState.isTyping) {
        "[Enter] Finish  [Esc] Clear"
    } else if (state.selection.isNotEmpty && (actualState.libraryTab == LibraryTab.FAVORITES || actualState.libraryTab == LibraryTab.LOCAL || actualState.isInPlaylistDetail)) {
        if (isCompact) "[Space] Toggle  [m] Batch (${state.selection.count})  [Esc] Clear"
        else "[Space/v] Toggle  [Ctrl+A] All  [m] Batch (${state.selection.count})  [Esc] Clear"
    } else {
        when (actualState.libraryTab) {
            LibraryTab.FAVORITES -> when (actualState.favoritesSubTab) {
                FavoritesSubTab.SONGS -> if (isCompact) "[Enter] Play  [m] Opts  [v] Sel  [f] Unfav  [◄/►] Tabs"
                else "[Enter] Play  [m] Options  [v] Select  [f] Remove  [◄/►, h/l] Subtabs  [1..3] Tabs"

                else -> if (isCompact) "[Enter] Open  [f] Unfav  [◄/►] Tabs"
                else "[Enter] Open  [f] Remove  [◄/►, h/l] Subtabs  [1..3] Tabs"
            }

            LibraryTab.PLAYLISTS -> if (actualState.isInPlaylistDetail) {
                if (isCompact) "[Enter] Play  [Shift+↑/↓] Move  [d] Del  [Esc] Back"
                else "[Enter] Play  [m] Options  [v] Select  [Shift+↑/↓] Move  [d] Remove  [Esc] Back"
            } else {
                if (isCompact) "[Enter] Open  [n] New  [d] Del"
                else "[Enter] Open  [n] New  [r] Rename  [d] Delete  [1..3] Tabs"
            }

            LibraryTab.LOCAL -> if (isCompact) "[Enter] Play  [Tab] Dir  [r] Rescan"
            else "[Enter] Play  [m] Options  [v] Select  [Tab] Folder  [r] Rescan  [1..3] Tabs"
        }
    }

    val footer = row(
        text(" $hints").fg(TEXT_DIM)
    )

    val body = column(
        tabBar,
        text("").length(1),
        content.fill(),
        text("").length(1),
        footer,
    )

    return panel(body)
        .title(" $ICON_LIBRARY Your Library ")
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("library-panel")
        .onKeyEvent(onKeyEvent)
        .fill()
}

internal fun tabLabel(label: String, active: Boolean): Element =
    text(label).fg(if (active) PRIMARY_COLOR else TEXT_DIM).apply { if (active) bold() }