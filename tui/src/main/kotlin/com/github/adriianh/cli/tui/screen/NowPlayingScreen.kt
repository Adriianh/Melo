package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.util.LrcParser
import com.github.adriianh.core.domain.model.LyricsTranslationMode
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.image.Image
import dev.tamboui.image.ImageScaling
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Flex
import dev.tamboui.style.Overflow
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.dock
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.Toolkit.widget
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.block.Block
import dev.tamboui.widgets.block.BorderType
import dev.tamboui.widgets.block.Borders

fun renderNowPlayingScreen(
    state: MeloState,
    marqueeText: (String, Int, Int) -> String,
    onKeyEvent: (KeyEvent) -> EventResult,
    terminalHeight: Int = 30
): Element {
    val track = state.player.nowPlaying ?: return renderNoTrackPlaying()

    val artworkPanel = buildArtworkPanel(state)
    val infoPanel    = buildInfoPanel(state, track, marqueeText)
    val lyricsPanel = buildLyricsPanel(state, terminalHeight)

    val leftColumn = dock()
        .top(artworkPanel, Constraint.length(18))
        .center(infoPanel)

    val content = dock()
        .left(leftColumn, Constraint.percentage(40))
        .center(lyricsPanel)

    return panel(content)
        .borderless()
        .focusable()
        .id("now-playing-panel")
        .onKeyEvent(onKeyEvent)
}

private fun renderNoTrackPlaying(): Element = panel(
    column(
        spacer(),
        text("$ICON_NOTE  Nothing is playing").fg(TEXT_SECONDARY).centered(),
        text("Search for a song and press Enter to start").fg(TEXT_DIM).centered(),
        spacer(),
    )
).title("Now Playing")
    .rounded()
    .borderColor(BORDER_DEFAULT)

private fun buildArtworkPanel(state: MeloState): Element =
    if (state.player.nowPlayingArtwork != null && !state.player.isQueueVisible) {
        widget(
            Image.builder()
                .data(state.player.nowPlayingArtwork)
                .scaling(ImageScaling.FIT)
                .block(
                    Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .build()
                )
                .build()
        )
    } else {
        panel(text("[ No Artwork ]").dim().centered()).rounded()
    }

private fun buildInfoPanel(
    state: MeloState,
    track: Track,
    marqueeText: (String, Int, Int) -> String,
): Element = panel(
    column(
        spacer(),
        text(marqueeText(track.title, state.player.marqueeOffset, 28)).bold().fg(TEXT_PRIMARY).centered(),
        text(track.artist).fg(TEXT_SECONDARY).centered(),
        text(track.album).fg(TEXT_DIM).centered(),
        spacer(),
    ).flex(Flex.CENTER)
).rounded().borderColor(BORDER_DEFAULT)

private fun buildLyricsPanel(state: MeloState, terminalHeight: Int): Element {
    val title = "Lyrics"

    if (state.player.isLoadingSyncedLyrics) {
        return panel(
            column(spacer(), text("  Loading lyrics...").dim().centered(), spacer())
        ).title(title).rounded().borderColor(BORDER_DEFAULT)
    }

    val lines = state.player.syncedLyrics
    if (lines.isEmpty()) {
        return panel(
            column(
                spacer(),
                text("  No synced lyrics available").fg(TEXT_SECONDARY).centered(),
                spacer())
        ).title(title).rounded().borderColor(BORDER_DEFAULT)
    }

    val nowPlayingState = (state.screen as? ScreenState.NowPlaying) ?: ScreenState.NowPlaying()
    val mode = state.player.lyricsTranslationMode
    val isBilingual = mode == LyricsTranslationMode.BILINGUAL
    val isAutoScroll = nowPlayingState.isAutoScrollLyrics

    val currentIndex = LrcParser.currentLineIndex(lines, state.player.nowPlayingPositionMs)
    val centerIndex = if (isAutoScroll) {
        if (currentIndex >= 0) currentIndex else 0
    } else {
        nowPlayingState.lyricsScrollOffset.coerceIn(0, lines.lastIndex)
    }

    val rawWindow = (terminalHeight - 10).coerceIn(8, 50)
    val windowSize = if (isBilingual) (rawWindow / 2).coerceAtLeast(4) else rawWindow
    val half = windowSize / 2
    val start = (centerIndex - half).coerceAtLeast(0)
    val end = (start + windowSize).coerceAtMost(lines.size)
    val visibleLines = lines.subList(start, end)
    val visibleCurrentIndex = if (currentIndex >= 0) currentIndex - start else -1
    val visibleCenterIndex = centerIndex - start

    val headerBadge = when {
        state.player.isTranslatingLyrics -> text("● Translating lyrics...").dim().centered()
        !isAutoScroll -> text("↕ MANUAL SCROLL [Enter to seek, 'a' to sync]").dim().centered()
        mode == LyricsTranslationMode.BILINGUAL -> text("● SYNCED [Bilingual]").bold()
            .fg(PRIMARY_COLOR).centered()

        mode == LyricsTranslationMode.TRANSLATION_ONLY -> text("● SYNCED [Translated]").bold()
            .fg(PRIMARY_COLOR).centered()

        else -> text("● SYNCED").bold().fg(PRIMARY_COLOR).centered()
    }

    val lineElements = mutableListOf<Element>()
    visibleLines.forEachIndexed { i, lrcLine ->
        val isCurrentPlaying = (i == visibleCurrentIndex)
        val isManualFocused = (!isAutoScroll && i == visibleCenterIndex)
        val isHighlighted = isCurrentPlaying || isManualFocused

        when (mode) {
            LyricsTranslationMode.ORIGINAL -> {
                val displayText = lrcLine.text.ifBlank { "♪ ♫ ♪" }
                val t = text(displayText).overflow(Overflow.WRAP_WORD)
                val styled = when {
                    isHighlighted -> t.bold().fg(PRIMARY_COLOR).centered()
                    visibleCurrentIndex >= 0 && i < visibleCurrentIndex -> t.fg(TEXT_DIM).centered()
                    else -> t.fg(TEXT_SECONDARY).centered()
                }
                lineElements.add(styled)
            }

            LyricsTranslationMode.TRANSLATION_ONLY -> {
                val displayText = lrcLine.translation?.takeIf { it.isNotBlank() }
                    ?: lrcLine.text.ifBlank { "♪ ♫ ♪" }
                val t = text(displayText).overflow(Overflow.WRAP_WORD)
                val styled = when {
                    isHighlighted -> t.bold().fg(PRIMARY_COLOR).centered()
                    visibleCurrentIndex >= 0 && i < visibleCurrentIndex -> t.fg(TEXT_DIM).centered()
                    else -> t.fg(TEXT_SECONDARY).centered()
                }
                lineElements.add(styled)
            }

            LyricsTranslationMode.BILINGUAL -> {
                val displayText = lrcLine.text.ifBlank { "♪ ♫ ♪" }
                val t = text(displayText).overflow(Overflow.WRAP_WORD)
                val styledOrig = when {
                    isHighlighted -> t.bold().fg(PRIMARY_COLOR).centered()
                    visibleCurrentIndex >= 0 && i < visibleCurrentIndex -> t.fg(TEXT_DIM).centered()
                    else -> t.fg(TEXT_SECONDARY).centered()
                }
                lineElements.add(styledOrig)

                val trans = lrcLine.translation
                if (!trans.isNullOrBlank()) {
                    val sub = text("↳ $trans").overflow(Overflow.WRAP_WORD)
                    val styledSub = when {
                        isHighlighted -> sub.fg(TEXT_PRIMARY).dim().centered()
                        else -> sub.fg(TEXT_DIM).centered()
                    }
                    lineElements.add(styledSub)
                }
            }
        }
    }

    val bottomHelp = if (isAutoScroll) {
        " [↑/↓] Scroll  [t] Mode "
    } else {
        " [↑/↓] Move  [Enter] Seek  [a] Sync  [t] Mode "
    }

    return panel(
        column(
            headerBadge.length(1),
            spacer(),
            *lineElements.toTypedArray(),
            spacer())
    ).title(title)
        .bottomTitle(bottomHelp)
        .rounded()
        .borderColor(BORDER_DEFAULT)
}