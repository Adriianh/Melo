package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.LrcParser
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import dev.tamboui.image.Image
import dev.tamboui.image.ImageScaling
import dev.tamboui.layout.Flex
import dev.tamboui.layout.Margin
import dev.tamboui.style.Overflow
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.tabs
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.Toolkit.widget
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement
import dev.tamboui.toolkit.elements.MarkupTextAreaElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent
import dev.tamboui.widgets.block.Block
import dev.tamboui.widgets.block.BorderType
import dev.tamboui.widgets.block.Borders

fun buildDetailPanel(
    state: MeloState,
    lyricsArea: MarkupTextAreaElement,
    similarArea: ListElement<*>,
    marqueeText: (String, Int, Int) -> String,
    onKeyEvent: (KeyEvent) -> EventResult,
): Element {
    val track = state.detail.selectedTrack ?: return spacer()

    val detailTabs = tabs("i: Info", "l: Lyrics", "s: Similar")
        .selected(state.detail.detailTab.ordinal)
        .highlightColor(PRIMARY_COLOR)
        .divider(" │ ")

    val tabContent: StyledElement<*> = when (state.detail.detailTab) {
        DetailTab.INFO -> renderTrackMetadata(track, state, marqueeText)
        DetailTab.LYRICS -> renderLyricsTab(state, lyricsArea)
        DetailTab.SIMILAR -> renderSimilarTab(state, similarArea)
    }

    val layeredContent = if (state.detail.detailTab != DetailTab.INFO) {
        tabContent.fill()
    } else {
        column(
            renderArtwork(state),
            tabContent.fill()
        )
    }

    val bottomHelp = when (state.detail.detailTab) {
        DetailTab.LYRICS -> {
            val isNowPlaying = state.player.nowPlaying?.id == track.id
            val hasSync =
                (if (isNowPlaying && state.player.syncedLyrics.isNotEmpty()) state.player.syncedLyrics else state.detail.syncedLyrics).isNotEmpty()
            if (hasSync) {
                if (isNowPlaying) " [↑/↓] Scroll  [a] Sync  [Esc] Back "
                else " [↑/↓] Scroll  [Esc] Back "
            } else {
                " [i/l/s] Tabs  [Esc] Back "
            }
        }

        DetailTab.SIMILAR -> " [↑/↓] Select  [Enter] Play  [Esc] Back "
        else -> " [i/l/s] Tabs  [Esc] Back "
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

fun buildEntityDetailPanel(
    state: MeloState,
    entityDescriptionArea: MarkupTextAreaElement,
    onKeyEvent: (KeyEvent) -> EventResult
): Element {
    val entity = state.detail.selectedEntity ?: return spacer()

    val headerElements = mutableListOf<Element>()
    when (entity) {
        is SearchResult.Album -> {
            headerElements.add(text(" Album ").fg(PRIMARY_COLOR))
            headerElements.add(text(""))
            headerElements.add(text(entity.title).fg(TEXT_PRIMARY))
            headerElements.add(text(entity.author).fg(TEXT_SECONDARY))
            if (entity.year != null) headerElements.add(text(entity.year.toString()).dim())
            if (!entity.songs.isNullOrEmpty()) headerElements.add(text("${entity.songs!!.size} tracks").dim())
            if (!entity.otherVersions.isNullOrEmpty()) {
                val versionElements = mutableListOf<Element>()
                versionElements.add(text(""))
                versionElements.add(text("Other versions:").fg(TEXT_SECONDARY))
                entity.otherVersions!!.forEach {
                    versionElements.add(text("• ${it.title}").dim().overflow(Overflow.WRAP_WORD))
                }
                headerElements.add(column(*versionElements.toTypedArray()))
            }
        }

        is SearchResult.Artist -> {
            headerElements.add(text(" Artist ").fg(PRIMARY_COLOR))
            headerElements.add(text(""))
            headerElements.add(text(entity.name).fg(TEXT_PRIMARY).overflow(Overflow.WRAP_WORD))
            if (entity.subscriberCountText != null) headerElements.add(text(entity.subscriberCountText!!).dim())
            if (entity.monthlyListenerCount != null) headerElements.add(text(entity.monthlyListenerCount!!).dim())
            if (state.detail.entityGenres.isNotEmpty()) {
                headerElements.add(
                    column(
                        text(""),
                        text(state.detail.entityGenres.joinToString(", ")).fg(TEXT_SECONDARY)
                            .overflow(Overflow.WRAP_WORD)
                    )
                )
            }
        }

        is SearchResult.Playlist -> {
            headerElements.add(text(" Playlist ").fg(PRIMARY_COLOR))
            headerElements.add(text(""))
            headerElements.add(text(entity.title).fg(TEXT_PRIMARY).overflow(Overflow.WRAP_WORD))
            headerElements.add(text(entity.author).fg(TEXT_SECONDARY).overflow(Overflow.WRAP_WORD))
            if (!entity.songs.isNullOrEmpty()) {
                headerElements.add(text("${entity.songs!!.size} tracks").dim())
            } else if (entity.trackCount != null) {
                headerElements.add(text("${entity.trackCount} tracks").dim())
            }
        }

        else -> return spacer()
    }

    val headerColumn = column(*headerElements.toTypedArray())
        .margin(Margin.horizontal(2))
        .flex(Flex.START)

    val descriptionRaw = when (entity) {
        is SearchResult.Album -> entity.description
        is SearchResult.Artist -> entity.description
        is SearchResult.Playlist -> entity.description
    }

    val description = wrapText(descriptionRaw)

    val artworkLines =
        if (state.detail.artworkData != null && !state.player.isQueueVisible) 18 else 5
    val headerLines = headerElements.size
    val topHeight = artworkLines + 1 + headerLines + (if (description.isNotEmpty()) 1 else 0)

    val topPart = column(
        renderArtwork(state),
        text("").length(1),
        headerColumn,
        if (description.isNotEmpty()) text("").length(1) else text("")
    ).flex(Flex.START).length(topHeight)

    val contentPart = if (description.isNotEmpty()) {
        column(
            topPart,
            entityDescriptionArea.markup(description).fill()
        ).fill()
    } else {
        column(topPart, spacer()).fill()
    }

    return panel(contentPart)
        .title(" Details ")
        .bottomTitle(" [Esc] Back ")
        .id("desc-area")
        .focusable()
        .rounded()
        .borderColor(BORDER_DEFAULT)
        .focusedBorderColor(BORDER_FOCUSED)
        .onKeyEvent(onKeyEvent)
        .fill()
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
        panel(text(" [ No Artwork ] ").dim().centered()).rounded().length(5)
    }

private fun renderLyricsTab(
    state: MeloState,
    lyricsArea: MarkupTextAreaElement,
): StyledElement<*> {
    val track = state.detail.selectedTrack
    val isNowPlaying = track != null && state.player.nowPlaying?.id == track.id
    val isLoading = state.detail.isLoadingLyrics ||
            (isNowPlaying && state.player.isLoadingSyncedLyrics && state.player.syncedLyrics.isEmpty())

    if (isLoading) {
        return column(
            spacer(),
            text("  Loading lyrics...").dim().centered(),
            spacer()
        )
    }

    val syncedLines = if (isNowPlaying && state.player.syncedLyrics.isNotEmpty()) {
        state.player.syncedLyrics
    } else {
        state.detail.syncedLyrics
    }

    if (syncedLines.isNotEmpty()) {
        val windowSize = 14
        val half = windowSize / 2

        val (visibleLines, relativeActiveIndex) = if (isNowPlaying) {
            val activeIndex =
                LrcParser.currentLineIndex(syncedLines, state.player.nowPlayingPositionMs)
            val centerIndex = if (state.detail.isAutoScrollLyrics) {
                activeIndex
            } else {
                (activeIndex + state.detail.lyricsScrollOffset).coerceIn(0, syncedLines.lastIndex)
            }
            val start = (centerIndex - half).coerceAtLeast(0)
            val end = (start + windowSize).coerceAtMost(syncedLines.size)
            Pair(syncedLines.subList(start, end), activeIndex - start)
        } else {
            val centerIndex = state.detail.lyricsScrollOffset.coerceIn(0, syncedLines.lastIndex)
            val start = (centerIndex - half).coerceAtLeast(0)
            val end = (start + windowSize).coerceAtMost(syncedLines.size)
            Pair(syncedLines.subList(start, end), -1)
        }

        val headerBadge = if (isNowPlaying) {
            if (state.detail.isAutoScrollLyrics) {
                text("● SYNCED").bold().fg(PRIMARY_COLOR).centered()
            } else {
                text("↕ MANUAL SCROLL (press 'a' to sync)").dim().centered()
            }
        } else {
            text("♫ Lyrics (${syncedLines.size} lines)").dim().centered()
        }

        val lineElements = visibleLines.mapIndexed { i, lrcLine ->
            val isCurrent = (i == relativeActiveIndex)
            val displayText = lrcLine.text.ifBlank { "♪ ♫ ♪" }
            val linePrefix = if (isCurrent) "▶ " else "  "
            val t = text("$linePrefix$displayText")
            when {
                isCurrent -> t.bold().fg(PRIMARY_COLOR).centered()
                relativeActiveIndex >= 0 && i < relativeActiveIndex -> t.fg(TEXT_DIM).centered()
                else -> t.fg(TEXT_SECONDARY).centered()
            }
        }

        return column(
            text("").length(1),
            headerBadge,
            text("").length(1),
            *lineElements.toTypedArray(),
            spacer()
        )
    }

    return when {
        state.detail.lyrics != null -> lyricsArea.markup(state.detail.lyrics).fill()
        else -> column(
            spacer(),
            text("  Press Enter or 'l' to load lyrics").fg(TEXT_SECONDARY).centered(),
            spacer()
        )
    }
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
        val titleColor =
            if (isSelected) PRIMARY_COLOR else if (isPlayable) TEXT_PRIMARY else TEXT_DIM
        row(
            text(similar.title).fg(titleColor).apply { if (isSelected) bold() }.ellipsisMiddle()
                .fill(),
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

private fun wrapText(text: String?): String {
    if (text.isNullOrEmpty()) return ""
    return text.split("\n").joinToString("\n") { paragraph ->
        val words = paragraph.split(Regex("\\s+"))
        val sb = java.lang.StringBuilder()
        var currentLineLength = 0
        for (word in words) {
            if (word.isEmpty()) continue
            val plainWordLength = word.replace(Regex("\\[/?.*?]"), "").length
            if (currentLineLength + plainWordLength > 60 && currentLineLength > 0) {
                sb.append("\n")
                currentLineLength = 0
            } else if (currentLineLength > 0) {
                sb.append(" ")
                currentLineLength += 1
            }
            sb.append(word)
            currentLineLength += plainWordLength
        }
        sb.toString()
    }
}