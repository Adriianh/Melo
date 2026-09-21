package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.isFavoriteTrack
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import com.github.adriianh.core.domain.model.Track
import dev.tamboui.layout.Margin
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.panel
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.Element

internal fun buildTrackPreview(track: Track?, state: MeloState, title: String): Element {
    if (track == null) {
        return panel(
            column(
                spacer(),
                text("Select a track to view preview").fg(TEXT_DIM).centered(),
                spacer()
            )
        ).title(" $title ").rounded().borderColor(BORDER_DEFAULT).fill()
    }

    val isPlaying = track.id == state.player.nowPlaying?.id
    val isFav = state.isFavoriteTrack(track)
    val nowPlayingText = if (isPlaying) " $ICON_NOTE Now Playing" else ""

    return panel(
        column(
            row(
                text("[Track]").bold().fg(PRIMARY_COLOR),
                if (isFav) text(" $ICON_HEART Favorited").fg(PRIMARY_COLOR) else text(""),
                if (isPlaying) text(nowPlayingText).fg(PRIMARY_COLOR) else text("")
            ).length(1),
            text(""),
            text(track.title).bold().fg(TEXT_PRIMARY).ellipsisMiddle(),
            text("by ${track.artist}").fg(TEXT_SECONDARY).ellipsis(),
            if (track.album.isNotBlank()) text("Album: ${track.album}").dim()
                .ellipsis() else text(""),
            if (track.durationMs > 0L) text("Duration: ${formatDuration(track.durationMs)}").dim() else text(
                ""
            ),
            spacer(),
            text("────────────────────────────────").fg(BORDER_DEFAULT),
            text("[Enter] Play track").fg(PRIMARY_COLOR),
            text("[Q] Add to queue").fg(TEXT_DIM),
            text("[F] Toggle favorite").fg(TEXT_DIM),
            text("[O] Track options").fg(TEXT_DIM),
        ).margin(Margin.symmetric(1, 1))
    ).title(" $title ").rounded().borderColor(BORDER_DEFAULT).fill()
}