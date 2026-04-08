package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.graphics.ClearGraphicsElement
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.image.Image
import dev.tamboui.image.ImageScaling
import dev.tamboui.layout.Flex
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.elements.MarkupTextAreaElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.block.Block
import dev.tamboui.widgets.block.BorderType
import dev.tamboui.widgets.block.Borders
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.layout.Margin

fun buildDetailPanel(
    state: MeloState,
    lyricsArea: MarkupTextAreaElement,
    similarArea: ListElement<*>,
    marqueeText: (String, Int, Int) -> String,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val track = state.detail.selectedTrack ?: return spacer()

    val detailTabs = tabs("Info", "Lyrics", "Similar")
        .selected(state.detail.detailTab.ordinal)
        .highlightColor(PRIMARY_COLOR)
        .divider(" │ ")

    val tabContent: StyledElement<*> = when (state.detail.detailTab) {
        DetailTab.INFO -> renderTrackMetadata(track, state, marqueeText)
        DetailTab.LYRICS -> renderLyricsTab(state, lyricsArea)
        DetailTab.SIMILAR -> renderSimilarTab(state, similarArea)
    }

    val layeredContent = if (state.detail.detailTab != DetailTab.INFO) {
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
        .id("detail-panel")
        .focusable()
        .onKeyEvent(onKeyEvent)
}

fun buildEntityDetailPanel(
    state: MeloState
): Element {
    val entity = state.detail.selectedEntity ?: return spacer()

    val infoTab = column(
        renderArtwork(state),
        text(""),
        when (entity) {
            is SearchResult.Album -> {
                column(
                    text(" Album ").fg(PRIMARY_COLOR),
                    text(""),
                    text(entity.title).fg(TEXT_PRIMARY),
                    text(entity.author).fg(TEXT_SECONDARY),
                    if (entity.year != null) text(entity.year!!).dim() else spacer(),
                    if (!entity.description.isNullOrEmpty()) {
                        column(
                            text(""),
                            text(entity.description!!).dim().overflow(dev.tamboui.style.Overflow.WRAP_WORD)
                        )
                    } else spacer(),
                    if (!entity.otherVersions.isNullOrEmpty()) {
                        column(
                            text(""),
                            text("Other versions:").fg(TEXT_SECONDARY),
                            *entity.otherVersions!!.map { text("• ${it.title}").dim().overflow(dev.tamboui.style.Overflow.WRAP_WORD) }.toTypedArray()
                        )
                    } else spacer()
                ).margin(Margin.horizontal(2)).fill()
            }
            is SearchResult.Artist -> {
                column(
                    text(" Artist ").fg(PRIMARY_COLOR),
                    text(""),
                    text(entity.name).fg(TEXT_PRIMARY).overflow(dev.tamboui.style.Overflow.WRAP_WORD),
                    if (entity.subscriberCountText != null) text(entity.subscriberCountText!!).dim() else spacer(),
                    if (state.detail.entityGenres.isNotEmpty()) {
                        column(
                            text(""),
                            text(state.detail.entityGenres.joinToString(", ")).fg(TEXT_SECONDARY).overflow(dev.tamboui.style.Overflow.WRAP_WORD)
                        )
                    } else spacer(),
                    if (!entity.description.isNullOrEmpty()) {
                        column(
                            text(""),
                            text(entity.description!!).dim().overflow(dev.tamboui.style.Overflow.WRAP_WORD)
                        )
                    } else spacer()
                ).margin(Margin.horizontal(2)).fill()
            }
            is SearchResult.Playlist -> {
                column(
                    text(" Playlist ").fg(PRIMARY_COLOR),
                    text(""),
                    text(entity.title).fg(TEXT_PRIMARY).overflow(dev.tamboui.style.Overflow.WRAP_WORD),
                    text(entity.author).fg(TEXT_SECONDARY).overflow(dev.tamboui.style.Overflow.WRAP_WORD),
                    if (entity.trackCount != null) text("${entity.trackCount} tracks").dim() else spacer(),
                    if (!entity.description.isNullOrEmpty()) {
                        column(
                            text(""),
                            text(entity.description!!).dim().overflow(dev.tamboui.style.Overflow.WRAP_WORD)
                        )
                    } else spacer()
                ).margin(Margin.horizontal(2)).fill()
            }
            else -> spacer()
        }
    )

    return panel(infoTab.fill())
        .title("Details")
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .id("entity-detail-panel")
}

private fun renderTrackMetadata(
    track: Track,
    state: MeloState,
    marqueeText: (String, Int, Int) -> String,
): StyledElement<*> = column(
    text(marqueeText(track.title, state.player.marqueeOffset, 30)).bold().fg(TEXT_PRIMARY),
    text(marqueeText(track.artist, state.player.marqueeOffset, 30)).fg(TEXT_SECONDARY),
    text("")
).flex(Flex.START)

private fun renderArtwork(state: MeloState): StyledElement<*> =
    if (state.detail.artworkData != null && !state.player.isQueueVisible) {
        widget(
            Image.builder()
                .data(state.detail.artworkData)
                .scaling(ImageScaling.FIT)
                .block(
                    Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
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
    lyricsArea: MarkupTextAreaElement,
): StyledElement<*> = when {
    state.detail.isLoadingLyrics -> column(
        spacer(),
        text("  Loading lyrics...").dim().centered(),
        spacer()
    )

    state.detail.lyrics != null -> lyricsArea.markup(state.detail.lyrics).fill()
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
    if (state.detail.isLoadingSimilar) {
        return column(
            spacer(),
            text("  Loading similar tracks...").dim().centered(),
            spacer()
        )
    }
    if (state.detail.similarTracks.isEmpty()) {
        return column(
            spacer(),
            text("  No similar tracks found").fg(TEXT_SECONDARY).centered(),
            spacer()
        )
    }
    val items = state.detail.similarTracks.mapIndexed { index, similar ->
        val isSelected = index == state.detail.similarCursor
        val isPlayable = state.isPlayable(similar)
        val titleColor = if (isSelected) PRIMARY_COLOR else if (isPlayable) TEXT_PRIMARY else TEXT_DIM
        row(
            text(similar.title).fg(titleColor).apply { if (isSelected) bold() }.ellipsisMiddle().fill(),
            text(similar.artist).fg(TEXT_SECONDARY).ellipsis().percent(30),
        )
    }.toMutableList()

    if (state.detail.isLoadingMoreSimilar) {
        items.add(
            row(
                text("  "),
                text("Loading more...").fg(TEXT_DIM).centered().fill(),
                text("")
            )
        )
    }

    similarArea.elements(*items.toTypedArray())
    similarArea.selected(state.detail.similarCursor)
    return similarArea.fill()
}