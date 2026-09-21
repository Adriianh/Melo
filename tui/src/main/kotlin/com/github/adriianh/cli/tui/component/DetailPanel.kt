package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.DetailTab
import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_BLUE
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_RED
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_LOADING
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.ICON_OFFLINE
import com.github.adriianh.cli.tui.MeloTheme.ICON_PAUSE
import com.github.adriianh.cli.tui.MeloTheme.ICON_PLAY
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.SECONDARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.isFavoriteEntity
import com.github.adriianh.cli.tui.isFavoriteTrack
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.LrcParser
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.LyricsTranslationMode
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.model.search.entityId
import dev.tamboui.image.Image
import dev.tamboui.image.ImageScaling
import dev.tamboui.layout.Flex
import dev.tamboui.layout.Margin
import dev.tamboui.style.Overflow
import dev.tamboui.text.Line
import dev.tamboui.text.Span
import dev.tamboui.text.Text
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.richText
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
            if (hasSync) {
                if (isNowPlaying) {
                    if (state.detail.isAutoScrollLyrics) " [↑/↓] Scroll  [t] Mode  [Esc] Back "
                    else " [↑/↓] Move  [Enter] Seek  [a] Sync  [t] Mode  [Esc] Back "
                } else {
                    " [↑/↓] Move  [Enter] Play  [t] Mode  [Esc] Back "
                }
            } else {
                " [i/l/s] Tabs  [t] Mode  [Esc] Back "
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

fun buildEntityDetailPanel(
    state: MeloState,
    entityDescriptionArea: MarkupTextAreaElement,
    onKeyEvent: (KeyEvent) -> EventResult
): Element {
    val entity = state.detail.selectedEntity ?: return spacer()

    val isSaved = state.isFavoriteEntity(entity.entityId)
    val headerElements = mutableListOf<Element>()
    when (entity) {
        is SearchResult.Album -> {
            val favBadge = if (isSaved) text(" [♥ Saved] ").fg(ACCENT_RED)
                .bold() else text(" [♡ Save (F)] ").fg(TEXT_DIM)
            headerElements.add(row(text(" [ALBUM] ").fg(PRIMARY_COLOR).bold(), text(" "), favBadge))
            headerElements.add(text(""))
            headerElements.add(
                text(entity.title).bold().fg(TEXT_PRIMARY).overflow(Overflow.WRAP_WORD)
            )
            headerElements.add(text(entity.author).fg(TEXT_SECONDARY).overflow(Overflow.WRAP_WORD))
            val albumStats = mutableListOf<String>()
            if (entity.year != null) albumStats.add(entity.year.toString())
            val trackCount = entity.songs?.size ?: 0
            if (trackCount > 0) albumStats.add("$trackCount track${if (trackCount != 1) "s" else ""}")
            val totalDuration = entity.songs?.sumOf { it.durationMs } ?: 0L
            if (totalDuration > 0L) albumStats.add(formatDuration(totalDuration))
            if (albumStats.isNotEmpty()) {
                headerElements.add(text(albumStats.joinToString(" • ")).dim())
            }
            if (!entity.otherVersions.isNullOrEmpty()) {
                headerElements.add(text(""))
                headerElements.add(text("Other versions:").fg(TEXT_SECONDARY))
                entity.otherVersions!!.forEach {
                    headerElements.add(text("• ${it.title}").dim().overflow(Overflow.WRAP_WORD))
                }
            }
        }

        is SearchResult.Artist -> {
            val favBadge = if (isSaved) text(" [♥ Following] ").fg(ACCENT_RED)
                .bold() else text(" [♡ Follow (F)] ").fg(TEXT_DIM)
            headerElements.add(
                row(
                    text(" [ARTIST] ").fg(PRIMARY_COLOR).bold(),
                    text(" "),
                    favBadge
                )
            )
            headerElements.add(text(""))
            headerElements.add(
                text(entity.name).bold().fg(TEXT_PRIMARY).overflow(Overflow.WRAP_WORD)
            )
            val stats = mutableListOf<String>()
            if (!entity.monthlyListenerCount.isNullOrBlank()) stats.add("${entity.monthlyListenerCount} listeners")
            if (!entity.subscriberCountText.isNullOrBlank()) stats.add("${entity.subscriberCountText} subscribers")
            if (stats.isNotEmpty()) {
                headerElements.add(
                    text(stats.joinToString(" • ")).dim().overflow(Overflow.WRAP_WORD)
                )
            }
            if (state.detail.entityGenres.isNotEmpty()) {
                headerElements.add(text(""))
                headerElements.add(
                    text(state.detail.entityGenres.joinToString(", ")).fg(TEXT_SECONDARY)
                        .overflow(Overflow.WRAP_WORD)
                )
            }
        }

        is SearchResult.Playlist -> {
            val favBadge = if (isSaved) text(" [♥ Saved] ").fg(ACCENT_RED)
                .bold() else text(" [♡ Save (F)] ").fg(TEXT_DIM)
            headerElements.add(
                row(
                    text(" [PLAYLIST] ").fg(ACCENT_BLUE).bold(),
                    text(" "),
                    favBadge
                )
            )
            headerElements.add(text(""))
            headerElements.add(
                text(entity.title).bold().fg(TEXT_PRIMARY).overflow(Overflow.WRAP_WORD)
            )
            headerElements.add(text(entity.author).fg(TEXT_SECONDARY).overflow(Overflow.WRAP_WORD))
            val count = entity.songs?.size ?: entity.trackCount ?: 0
            val playlistStats = mutableListOf<String>()
            if (count > 0) playlistStats.add("$count track${if (count != 1) "s" else ""}")
            val totalDuration = entity.songs?.sumOf { it.durationMs } ?: 0L
            if (totalDuration > 0L) playlistStats.add(formatDuration(totalDuration))
            if (playlistStats.isNotEmpty()) {
                headerElements.add(text(playlistStats.joinToString(" • ")).dim())
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
    isNowPlaying: Boolean = state.player.nowPlaying?.id == track.id,
): StyledElement<*> {
    val albumText = track.album.ifBlank { "Single" }
    val durationText = if (track.durationMs > 0) formatDuration(track.durationMs) else "--:--"
    val isFav = state.isFavoriteTrack(track)
    val isOff = state.collections.offlineTracks.any {
        it.track.id == track.id && it.downloadStatus == DownloadStatus.COMPLETED
    }
    val queuePos = state.player.queue.indexOfFirst { it.id == track.id }
    val queueText = when {
        isNowPlaying -> "Now playing"
        queuePos >= 0 -> "#${queuePos + 1} in queue"
        else -> "Not in queue"
    }

    val elements = mutableListOf<Element>()
    val titleArtistSpans = mutableListOf<Span>(
        Span.raw(track.title).bold().fg(TEXT_PRIMARY)
    )
    if (track.artist.isNotBlank()) {
        titleArtistSpans.add(Span.raw(" ● ").dim())
        titleArtistSpans.add(Span.raw(track.artist).fg(PRIMARY_COLOR))
    }
    val titleArtistLine = Line.from(titleArtistSpans)
    val titleLen =
        track.title.length + (if (track.artist.isNotBlank()) track.artist.length + 3 else 0)
    val titleLines = if (titleLen > 40) 2 else 1
    elements.add(richText(Text.from(titleArtistLine)).wrapWord().length(titleLines))

    if (isNowPlaying) {
        val isPlaying = state.player.isPlaying
        val playIcon = if (isPlaying) ICON_PLAY else ICON_PAUSE
        val posStr = formatDuration(state.player.nowPlayingPositionMs)
        val durStr = if (track.durationMs > 0) formatDuration(track.durationMs) else "--:--"
        val pct =
            if (track.durationMs > 0) (state.player.nowPlayingPositionMs.toDouble() / track.durationMs).coerceIn(
                0.0,
                1.0
            ) else 0.0
        val barWidth = 18
        val filled = (pct * barWidth).toInt().coerceIn(0, barWidth)
        val bar = "━".repeat(filled) + (if (filled < barWidth) "╸" else "") + "─".repeat(
            (barWidth - filled - 1).coerceAtLeast(0)
        )

        elements.add(text("").length(1))
        elements.add(
            row(
                text("$playIcon NOW PLAYING").bold().fg(PRIMARY_COLOR),
                text("  $posStr / $durStr").dim(),
            ).length(1)
        )
        elements.add(text(bar).fg(PRIMARY_COLOR).length(1))
    } else {
        elements.add(text("").length(1))
        elements.add(
            row(
                text("Queue     ").dim().length(10),
                text(queueText).fg(if (queuePos >= 0) PRIMARY_COLOR else TEXT_DIM).fill(),
            ).length(1)
        )
    }

    elements.add(text("").length(1))
    elements.add(
        row(
            text("Album     ").dim().length(10),
            text(albumText).fg(TEXT_PRIMARY).ellipsis().fill(),
        ).length(1)
    )
    elements.add(
        row(
            text("Length    ").dim().length(10),
            text(durationText).fg(TEXT_PRIMARY).fill(),
        ).length(1)
    )
    if (track.genres.isNotEmpty()) {
        elements.add(
            row(
                text("Genres    ").dim().length(10),
                text(track.genres.joinToString(", ")).fg(TEXT_SECONDARY).ellipsis().fill(),
            ).length(1)
        )
    }
    elements.add(
        row(
            text("Status    ").dim().length(10),
            text(if (isFav) "$ICON_HEART Liked" else "♡ Not liked").fg(if (isFav) PRIMARY_COLOR else TEXT_DIM),
            text("  •  ").dim(),
            text(if (isOff) "$ICON_OFFLINE Downloaded" else "Streaming").fg(if (isOff) SECONDARY_COLOR else TEXT_DIM),
        ).length(1)
    )
    elements.add(spacer())

    return column(*elements.toTypedArray()).margin(Margin.horizontal(1)).fill()
}

private fun renderArtwork(
    state: MeloState,
    terminalHeight: Int = 30,
    isNowPlaying: Boolean = false,
): StyledElement<*> {
    val artworkHeight = if (terminalHeight <= 28) 12 else 15
    val artworkData = state.detail.artworkData ?: if (isNowPlaying) state.player.nowPlayingArtwork else null

    return if (artworkData != null && !state.player.isQueueVisible) {
        widget(
            Image.builder()
                .data(artworkData)
                .scaling(ImageScaling.FIT)
                .block(
                    Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .build()
                )
                .build()
        ).length(artworkHeight)
    } else {
        panel(
            column(
                spacer(),
                text(ICON_NOTE).fg(PRIMARY_COLOR).centered(),
                text("No Artwork Available").dim().centered(),
                spacer(),
            )
        ).rounded().borderColor(BORDER_DEFAULT).length(artworkHeight)
    }
}

private fun renderLyricsTab(
    state: MeloState,
    lyricsArea: MarkupTextAreaElement,
    terminalHeight: Int = 30,
): StyledElement<*> {
    val track = state.detail.selectedTrack ?: state.player.nowPlaying
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

    val mode =
        if (isNowPlaying) state.player.lyricsTranslationMode else state.detail.lyricsTranslationMode
    val isTranslating =
        if (isNowPlaying) state.player.isTranslatingLyrics else state.detail.isTranslatingLyrics
    val isBilingual = mode == LyricsTranslationMode.BILINGUAL

    val syncedLines = if (isNowPlaying && state.player.syncedLyrics.isNotEmpty()) {
        state.player.syncedLyrics
    } else {
        state.detail.syncedLyrics
    }

    if (syncedLines.isNotEmpty()) {
        val activeIndex = if (isNowPlaying) {
            LrcParser.currentLineIndex(syncedLines, state.player.nowPlayingPositionMs)
        } else -1

        val centerIndex = if (isNowPlaying) {
            if (state.detail.isAutoScrollLyrics) {
                if (activeIndex >= 0) activeIndex else 0
            } else {
                state.detail.lyricsScrollOffset.coerceIn(0, syncedLines.lastIndex)
            }
        } else {
            state.detail.lyricsScrollOffset.coerceIn(0, syncedLines.lastIndex)
        }

        val rawWindow = (terminalHeight - 12).coerceIn(14, 50)
        val windowSize = if (isBilingual) (rawWindow / 2).coerceAtLeast(6) else rawWindow
        val half = windowSize / 2
        val start = (centerIndex - half).coerceAtLeast(0)
        val end = (start + windowSize).coerceAtMost(syncedLines.size)
        val visibleLines = syncedLines.subList(start, end)
        val visibleActiveIndex = if (activeIndex >= 0) activeIndex - start else -1
        val visibleCenterIndex = centerIndex - start

        val headerBadge = when {
            isTranslating -> text("● Translating lyrics...").dim().centered()
            isNowPlaying -> {
                if (state.detail.isAutoScrollLyrics) {
                    when (mode) {
                        LyricsTranslationMode.BILINGUAL -> text("● SYNCED [Bilingual]").bold()
                            .fg(PRIMARY_COLOR).centered()

                        LyricsTranslationMode.TRANSLATION_ONLY -> text("● SYNCED [Translated]").bold()
                            .fg(PRIMARY_COLOR).centered()

                        LyricsTranslationMode.ORIGINAL -> text("● SYNCED").bold().fg(PRIMARY_COLOR)
                            .centered()
                    }
                } else {
                    text("↕ MANUAL SCROLL [Enter to seek, 'a' to sync]").dim().centered()
                }
            }

            else -> {
                val modeSuffix = when (mode) {
                    LyricsTranslationMode.BILINGUAL -> " [Bilingual]"
                    LyricsTranslationMode.TRANSLATION_ONLY -> " [Translated]"
                    LyricsTranslationMode.ORIGINAL -> ""
                }
                text("♫ Lyrics (${syncedLines.size} lines)$modeSuffix").dim().centered()
            }
        }

        val lineElements = mutableListOf<Element>()
        visibleLines.forEachIndexed { i, lrcLine ->
            val isCurrentPlaying = (i == visibleActiveIndex)
            val isManualFocused = (!state.detail.isAutoScrollLyrics && i == visibleCenterIndex)
            val isHighlighted = isCurrentPlaying || isManualFocused

            when (mode) {
                LyricsTranslationMode.ORIGINAL -> {
                    val displayText = lrcLine.text.ifBlank { "♪ ♫ ♪" }
                    val t = text(displayText).overflow(Overflow.WRAP_WORD)
                    val styled = when {
                        isHighlighted -> t.bold().fg(PRIMARY_COLOR).centered()
                        visibleActiveIndex >= 0 && i < visibleActiveIndex -> t.fg(TEXT_DIM)
                            .centered()

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
                        visibleActiveIndex >= 0 && i < visibleActiveIndex -> t.fg(TEXT_DIM)
                            .centered()

                        else -> t.fg(TEXT_SECONDARY).centered()
                    }
                    lineElements.add(styled)
                }

                LyricsTranslationMode.BILINGUAL -> {
                    val displayText = lrcLine.text.ifBlank { "♪ ♫ ♪" }
                    val t = text(displayText).overflow(Overflow.WRAP_WORD)
                    val styledOrig = when {
                        isHighlighted -> t.bold().fg(PRIMARY_COLOR).centered()
                        visibleActiveIndex >= 0 && i < visibleActiveIndex -> t.fg(TEXT_DIM)
                            .centered()

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

        return column(
            headerBadge.length(1),
            spacer(),
            *lineElements.toTypedArray(),
            spacer(),
            text("").length(1),
        ).fill()
    }

    return when {
        state.detail.lyrics != null -> {
            val textToDisplay = when (mode) {
                LyricsTranslationMode.ORIGINAL -> state.detail.lyrics
                LyricsTranslationMode.TRANSLATION_ONLY -> state.detail.plainLyricsTranslation
                    ?: state.detail.lyrics

                LyricsTranslationMode.BILINGUAL -> {
                    if (state.detail.plainLyricsTranslation != null) {
                        val origLines = state.detail.lyrics.lines()
                        val transLines = state.detail.plainLyricsTranslation.lines()
                        origLines.mapIndexed { idx, orig ->
                            val trans = transLines.getOrNull(idx)
                            if (!trans.isNullOrBlank()) "$orig\n  ↳ $trans" else orig
                        }.joinToString("\n")
                    } else {
                        state.detail.lyrics
                    }
                }
            }
            if (isTranslating) {
                column(
                    text("● Translating lyrics...").dim().centered().length(1),
                    lyricsArea.markup(textToDisplay).fill()
                ).fill()
            } else {
                lyricsArea.markup(textToDisplay).fill()
            }
        }
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
            text("  $ICON_LOADING Loading recommendations...").dim().centered(),
            spacer()
        )
    }
    if (state.detail.similarTracks.isEmpty()) {
        return column(
            spacer(),
            text("  No recommendations found").fg(TEXT_SECONDARY).centered(),
            spacer()
        )
    }

    val sourceTrack = state.detail.selectedTrack ?: state.player.nowPlaying
    val headerSubtitle = if (sourceTrack != null) {
        "Based on \"${sourceTrack.title}\""
    } else {
        "Similar tracks"
    }

    val items = state.detail.similarTracks.mapIndexed { index, similar ->
        val isSelected = index == state.detail.similarCursor
        val isPlaying = state.player.nowPlaying?.id == similar.id
        val isFav = state.isFavoriteTrack(similar)
        val isPlayable = state.isPlayable(similar)
        val titleColor = when {
            isSelected -> PRIMARY_COLOR
            isPlaying -> PRIMARY_COLOR
            isPlayable -> TEXT_PRIMARY
            else -> TEXT_DIM
        }
        val prefix = when {
            isPlaying -> "$ICON_NOTE "
            isSelected -> "$ICON_PLAY "
            else -> "  "
        }
        val duration = if (similar.durationMs > 0L) formatDuration(similar.durationMs) else ""

        row(
            text(prefix).fg(PRIMARY_COLOR).length(2),
            text("${index + 1}").dim().length(3),
            text(similar.title).fg(titleColor).apply { if (isSelected) bold(); ellipsisMiddle() }
                .fill(),
            text(similar.artist).fg(TEXT_SECONDARY).ellipsis().percent(25),
            text(if (isFav) ICON_HEART else " ").fg(PRIMARY_COLOR).length(2),
            text(duration).fg(TEXT_DIM).length(6),
        )
    }.toMutableList()

    if (state.detail.isLoadingMoreSimilar) {
        items.add(
            row(
                text("  ").length(2),
                text("Loading more recommendations...").fg(TEXT_DIM).centered().fill(),
                text("").length(6),
            )
        )
    }

    similarArea.elements(*items.toTypedArray())
    similarArea.selected(state.detail.similarCursor)

    return column(
        text("").length(1),
        text("♫ RECOMMENDATIONS").bold().fg(PRIMARY_COLOR).centered(),
        text(headerSubtitle).dim().apply { ellipsis() }.centered(),
        text("").length(1),
        row(
            text("  ").length(2),
            text("#").dim().length(3),
            text("Title").dim().fill(),
            text("Artist").dim().percent(25),
            text(ICON_HEART).dim().length(2),
            text("Time").dim().length(6),
        ),
        similarArea.fill(),
    ).fill()
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