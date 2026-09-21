package com.github.adriianh.cli.tui.component

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme.ICON_HEART
import com.github.adriianh.cli.tui.MeloTheme.ICON_LOADING
import com.github.adriianh.cli.tui.MeloTheme.ICON_NOTE
import com.github.adriianh.cli.tui.MeloTheme.ICON_PLAY
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.isFavoriteTrack
import com.github.adriianh.cli.tui.isPlayable
import com.github.adriianh.cli.tui.util.TextFormatUtil.formatDuration
import dev.tamboui.toolkit.Toolkit.column
import dev.tamboui.toolkit.Toolkit.row
import dev.tamboui.toolkit.Toolkit.spacer
import dev.tamboui.toolkit.Toolkit.text
import dev.tamboui.toolkit.element.StyledElement
import dev.tamboui.toolkit.elements.ListElement

internal fun renderSimilarTab(
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