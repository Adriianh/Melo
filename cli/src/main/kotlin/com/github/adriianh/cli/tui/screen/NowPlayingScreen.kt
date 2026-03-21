package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.util.LrcParser
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.image.Image
import dev.tamboui.image.ImageScaling
import dev.tamboui.layout.Constraint
import dev.tamboui.layout.Flex
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.block.Block
import dev.tamboui.widgets.block.BorderType
import dev.tamboui.widgets.block.Borders

fun renderNowPlayingScreen(
    state: MeloState,
    marqueeText: (String, Int, Int) -> String,
    onKeyEvent: (KeyEvent) -> EventResult
): Element {
    val track = state.player.nowPlaying ?: return renderNoTrackPlaying()

    val artworkPanel = buildArtworkPanel(state)
    val infoPanel    = buildInfoPanel(state, track, marqueeText)
    val lyricsPanel  = buildLyricsPanel(state)

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

private fun buildLyricsPanel(state: MeloState): Element {
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

    val currentIndex = LrcParser.currentLineIndex(lines, state.player.nowPlayingPositionMs)

    val windowSize = 20
    val half = windowSize / 2
    val start = (currentIndex - half).coerceAtLeast(0)
    val end   = (start + windowSize).coerceAtMost(lines.size)
    val visibleLines = lines.subList(start, end)
    val visibleCurrentIndex = currentIndex - start

    val lineElements = visibleLines.mapIndexed { i, lrcLine ->
        val isCurrent = i == visibleCurrentIndex
        val displayText = lrcLine.text.ifBlank { " " }
        val t = text(displayText)
        when {
            isCurrent -> t.bold().fg(PRIMARY_COLOR).centered()
            i < visibleCurrentIndex -> t.fg(TEXT_DIM).centered()
            else -> t.fg(TEXT_SECONDARY).centered()
        }
    }

    return panel(
        column(
            spacer(),
            *lineElements.toTypedArray(),
            spacer())
    ).title(title).rounded().borderColor(BORDER_DEFAULT)
}