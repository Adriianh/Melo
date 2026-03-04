package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.ClearGraphicsElement
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.image.Image
import dev.tamboui.image.ImageScaling
import dev.tamboui.layout.Flex
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

fun buildDetailPanel(
    state: MeloState,
    lyricsArea: dev.tamboui.toolkit.elements.MarkupTextAreaElement,
    similarArea: ListElement<*>,
    marqueeText: (String, Int, Int) -> String,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val track = state.selectedTrack ?: return spacer()

    val detailTabs = tabs("Info", "Lyrics", "Similar")
        .selected(state.detailTab.ordinal)
        .highlightColor(PRIMARY_COLOR)
        .divider(" │ ")

    val tabContent: StyledElement<*> = when (state.detailTab) {
        DetailTab.INFO    -> renderTrackMetadata(track, state, marqueeText)
        DetailTab.LYRICS  -> renderLyricsTab(state, lyricsArea)
        DetailTab.SIMILAR -> renderSimilarTab(state, similarArea)
    }

    val layeredContent = if (state.detailTab != DetailTab.INFO) {
        stack(
            ClearGraphicsElement().fill(),
            tabContent.fill()
        )
    } else {
        column(
            renderArtwork(state),
            tabContent.fill()
        )
    }

    return panel(
        detailTabs.length(1),
        layeredContent.fill()
    ).title("Now Playing")
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .focusable()
        .id("detail-panel")
        .onKeyEvent(onKeyEvent)
}

private fun renderTrackMetadata(
    track: Track,
    state: MeloState,
    marqueeText: (String, Int, Int) -> String,
): StyledElement<*> = column(
    text(marqueeText(track.title, state.marqueeOffset, 30)).bold().fg(TEXT_PRIMARY),
    text(marqueeText(track.artist, state.marqueeOffset, 30)).fg(TEXT_SECONDARY),
    text(""),
    if (track.sourceId != null) text("✓ Available for streaming").dim().fg(PRIMARY_COLOR)
    else text("✗ Not available for streaming").dim()
).flex(Flex.START)

private fun renderArtwork(state: MeloState): StyledElement<*> =
    if (state.artworkData != null) {
        widget(
            Image.builder()
                .data(state.artworkData)
                .scaling(ImageScaling.FIT)
                .block(
                    dev.tamboui.widgets.block.Block.builder()
                        .borders(dev.tamboui.widgets.block.Borders.ALL)
                        .borderType(dev.tamboui.widgets.block.BorderType.ROUNDED)
                        .build()
                )
                .build()
        ).length(18)
    } else {
        stack(
            ClearGraphicsElement().fill(),
            panel(text(" [ No Artwork ] ").dim().centered()).rounded().fit().length(5)
        )
    }

private fun renderLyricsTab(
    state: MeloState,
    lyricsArea: dev.tamboui.toolkit.elements.MarkupTextAreaElement,
): StyledElement<*> = when {
    state.isLoadingLyrics -> column(
        spacer(),
        text("  Loading lyrics...").dim().centered(),
        spacer()
    )
    state.lyrics != null  -> lyricsArea.markup(state.lyrics).fill()
    else -> column(
        spacer(),
        text("  Press Enter to load lyrics").fg(TEXT_SECONDARY).centered(),
        spacer()
    )
}

private fun renderSimilarTab(
    state: MeloState,
    similarArea: ListElement<*>,
): StyledElement<*> {
    if (state.similarTracks.isEmpty()) {
        return column(
            spacer(),
            text("  No similar tracks found").fg(TEXT_SECONDARY).centered(),
            spacer()
        )
    }
    val items = state.similarTracks.map { similar ->
        val matchPercent = (similar.match * 100).toInt()
        row(
            text("• ").fg(PRIMARY_COLOR).length(2),
            text(similar.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
            text(similar.artist).fg(TEXT_SECONDARY).ellipsis().percent(30),
            text("($matchPercent%)").dim().length(6)
        )
    }
    similarArea.elements(*items.toTypedArray())
    return similarArea.fill()
}

