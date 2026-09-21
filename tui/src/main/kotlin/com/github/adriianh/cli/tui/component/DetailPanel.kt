package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.tabs
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.elements.MarkupTextAreaElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun buildDetailPanel(
    state: MeloState,
    lyricsArea: MarkupTextAreaElement,
    similarArea: ListElement<*>,
    onKeyEvent: (KeyEvent) -> EventResult,
    terminalHeight: Int = 30,
    trackOverride: Track? = null,
): Element {
    val track = trackOverride ?: state.detail.selectedTrack ?: state.player.nowPlaying ?: return spacer()

    val isNowPlaying = state.player.nowPlaying?.id == track.id
    val detailTabs = tabs("i: Info", "l: Lyrics", "s: Similar")
        .selected(state.detail.detailTab.ordinal)
        .highlightColor(PRIMARY_COLOR)
        .divider(" │ ")

    val tabContent: StyledElement<*> = when (state.detail.detailTab) {
        DetailTab.INFO -> renderTrackMetadata(track, state, isNowPlaying)
        DetailTab.LYRICS -> renderLyricsTab(state, lyricsArea, terminalHeight)
        DetailTab.SIMILAR -> renderSimilarTab(state, similarArea)
    }

    val layeredContent = if (state.detail.detailTab != DetailTab.INFO) {
        tabContent.fill()
    } else {
        column(
            renderArtwork(state, terminalHeight, isNowPlaying),
            tabContent.fill()
        )
    }

    val bottomHelp = when (state.detail.detailTab) {
        DetailTab.INFO -> " [Enter] Play  [q] Queue  [f] Fav  [o] Options  [Esc] Back "
        DetailTab.LYRICS -> {
            val hasSync =
                (if (isNowPlaying && state.player.syncedLyrics.isNotEmpty()) state.player.syncedLyrics else state.detail.syncedLyrics).isNotEmpty()
            val langSuffix = "  [T] Lang (${state.languagePicker.currentLanguage.uppercase()}) "
            if (hasSync) {
                if (isNowPlaying) {
                    if (state.detail.isAutoScrollLyrics) " [↑/↓] Scroll  [t] Mode$langSuffix [Esc] Back "
                    else " [↑/↓] Move  [Enter] Seek  [a] Sync  [t] Mode$langSuffix [Esc] Back "
                } else {
                    " [↑/↓] Move  [Enter] Play  [t] Mode$langSuffix [Esc] Back "
                }
            } else {
                " [i/l/s] Tabs  [t] Mode$langSuffix [Esc] Back "
            }
        }

        DetailTab.SIMILAR -> " [↑/↓] Select  [Enter] Play  [q] Queue  [f] Fav  [o] Options  [Esc] Back "
    }

    return panel(
        detailTabs.length(1),
        layeredContent.fill()
    ).title(" Track Details ")
        .bottomTitle(bottomHelp)
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .id("detail-panel")
        .focusable()
        .onKeyEvent(onKeyEvent)
}