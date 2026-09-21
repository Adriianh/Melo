package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.ICON_OFFLINE
import com.github.adriianh.cli.tui.MeloTheme.ICON_PAUSE
import com.github.adriianh.cli.tui.MeloTheme.ICON_PLAY
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.SECONDARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.isFavoriteTrack
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.image.Image
import dev.tamboui.image.ImageScaling
import dev.tamboui.layout.Margin
import dev.tamboui.text.Line
import dev.tamboui.text.Span
import dev.tamboui.text.Text
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.richText
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.Toolkit.widget
import dev.tamboui.toolkit.element.Element
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.widgets.block.Block
import dev.tamboui.widgets.block.BorderType
import dev.tamboui.widgets.block.Borders

internal fun renderTrackMetadata(
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

internal fun renderArtwork(
    state: MeloState,
    terminalHeight: Int = 30,
    isNowPlaying: Boolean = false,
): StyledElement<*> {
    val artworkHeight = if (terminalHeight <= 28) 12 else 15
    val artworkData =
        state.detail.artworkData ?: if (isNowPlaying) state.player.nowPlayingArtwork else null

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