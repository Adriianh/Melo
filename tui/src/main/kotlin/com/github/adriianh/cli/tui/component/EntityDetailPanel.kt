package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_BLUE
import com.github.adriianh.cli.tui.MeloTheme.ACCENT_RED
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.isFavoriteEntity
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.model.search.entityId
import dev.tamboui.layout.Flex
import dev.tamboui.layout.Margin
import dev.tamboui.style.Overflow
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.elements.MarkupTextAreaElement
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyEvent

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