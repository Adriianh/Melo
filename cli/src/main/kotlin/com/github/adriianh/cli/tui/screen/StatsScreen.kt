package com.github.adriianh.cli.tui.screen

import com.github.adriianh.cli.tui.*

import com.github.adriianh.cli.tui.MeloState
import com.github.adriianh.cli.tui.MeloTheme
import com.github.adriianh.cli.tui.MeloTheme.BORDER_DEFAULT
import com.github.adriianh.cli.tui.MeloTheme.BORDER_FOCUSED
import com.github.adriianh.cli.tui.MeloTheme.PRIMARY_COLOR
import com.github.adriianh.cli.tui.MeloTheme.TEXT_DIM
import com.github.adriianh.cli.tui.MeloTheme.TEXT_PRIMARY
import com.github.adriianh.cli.tui.MeloTheme.TEXT_SECONDARY
import com.github.adriianh.cli.tui.StatsTimeUnit
import com.github.adriianh.cli.tui.graphics.ClearGraphicsElement
import com.github.adriianh.core.domain.model.StatsPeriod
import dev.tamboui.layout.Constraint
import dev.tamboui.toolkit.Toolkit.*
import dev.tamboui.toolkit.element.Element
import dev.tamboui.tui.event.KeyEvent

private const val MINUTES_IN_HOUR = 60
private const val MINUTES_IN_DAY = 1440

private fun formatSummaryTime(totalMs: Long): String {
    val totalSecs = totalMs / 1_000
    val totalMins = totalSecs / 60
    return when {
        totalMins >= MINUTES_IN_DAY ->
            "${totalMins / MINUTES_IN_DAY}d ${(totalMins % MINUTES_IN_DAY) / MINUTES_IN_HOUR}h"
        totalMins >= MINUTES_IN_HOUR ->
            "${totalMins / MINUTES_IN_HOUR}h ${totalMins % MINUTES_IN_HOUR}m"
        totalMins >= 1 -> "${totalMins}m ${totalSecs % 60}s"
        else -> "${totalSecs}s"
    }
}

private fun formatByUnit(totalMs: Long, unit: StatsTimeUnit): String = when (unit) {
    StatsTimeUnit.SECONDS -> "${totalMs / 1_000}s"
    StatsTimeUnit.MINUTES -> "${totalMs / 60_000}m"
    StatsTimeUnit.HOURS   -> "${"%.1f".format(totalMs / 3_600_000.0)}h"
}

fun renderStatsScreen(
    state: MeloState,
    onKeyEvent: (KeyEvent) -> dev.tamboui.toolkit.event.EventResult,
): Element {
    val listening = (state.screen as ScreenState.Stats).statsListening
    val periodTabs = buildPeriodTabs((state.screen as ScreenState.Stats).statsPeriod)
    val unitTabs = buildUnitTabs((state.screen as ScreenState.Stats).statsTimeUnit)

    val summaryPanel = if (listening == null) {
        panel(
            column(
                spacer(),
                text(if ((state.screen as ScreenState.Stats).statsLoading) "  Loading…" else "  No listening data yet.")
                    .fg(TEXT_SECONDARY).centered(),
                text("  Start playing music to see your stats.")
                    .fg(TEXT_DIM).centered(),
                spacer(),
            )
        )
    } else {
        panel(
            column(
                row(
                    column(
                        text("  Total Time").fg(TEXT_DIM),
                        text("  ${formatSummaryTime(listening.totalMs)}").fg(PRIMARY_COLOR).bold(),
                    ).fill(),
                    column(
                        text("  Plays").fg(TEXT_DIM),
                        text("  ${listening.totalPlays}").fg(TEXT_PRIMARY).bold(),
                    ).fill(),
                    column(
                        text("  Unique Tracks").fg(TEXT_DIM),
                        text("  ${listening.uniqueTracks}").fg(TEXT_PRIMARY).bold(),
                    ).fill(),
                    column(
                        text("  Unique Artists").fg(TEXT_DIM),
                        text("  ${listening.uniqueArtists}").fg(TEXT_PRIMARY).bold(),
                    ).fill(),
                ).fill(),
            ).fill()
        )
    }
        .title("${MeloTheme.ICON_STATS} Overview")
        .rounded()
        .borderColor(BORDER_DEFAULT)

    val topTracksPanel = if ((state.screen as ScreenState.Stats).statsTopTracks.isEmpty()) {
        panel(
            column(
                spacer(),
                text("  No data").fg(TEXT_DIM).centered(),
                spacer(),
            )
        )
    } else {
        val unitLabel = (state.screen as ScreenState.Stats).statsTimeUnit.label
        panel(
            column(
                row(
                    text("  #").fg(TEXT_DIM).length(4),
                    text("Track").fg(TEXT_DIM).fill(),
                    text("Artist").fg(TEXT_DIM).percent(28),
                    text("Plays").fg(TEXT_DIM).length(7),
                    text(unitLabel).fg(PRIMARY_COLOR).bold().length(10),
                ),
                *(state.screen as ScreenState.Stats).statsTopTracks.mapIndexed { i, stat ->
                    row(
                        text("  ${i + 1}").fg(TEXT_DIM).length(4),
                        text(stat.track.title).fg(TEXT_PRIMARY).ellipsisMiddle().fill(),
                        text(stat.track.artist).fg(TEXT_SECONDARY).ellipsis().percent(28),
                        text("${stat.playCount}×").fg(TEXT_SECONDARY).length(7),
                        text(formatByUnit(stat.totalMs, (state.screen as ScreenState.Stats).statsTimeUnit)).fg(PRIMARY_COLOR).length(10),
                    )
                }.toTypedArray()
            ).fill()
        )
    }
        .title("${MeloTheme.ICON_NOTE} Top Tracks")
        .rounded()
        .borderColor(BORDER_DEFAULT)

    val topArtistsPanel = if ((state.screen as ScreenState.Stats).statsTopArtists.isEmpty()) {
        panel(
            column(
                spacer(),
                text("  No data").fg(TEXT_DIM).centered(),
                spacer(),
            )
        )
    } else {
        val unitLabel = (state.screen as ScreenState.Stats).statsTimeUnit.label
        panel(
            column(
                row(
                    text("  #").fg(TEXT_DIM).length(4),
                    text("Artist").fg(TEXT_DIM).fill(),
                    text("Plays").fg(TEXT_DIM).length(7),
                    text(unitLabel).fg(PRIMARY_COLOR).bold().length(10),
                ),
                *(state.screen as ScreenState.Stats).statsTopArtists.mapIndexed { i, stat ->
                    row(
                        text("  ${i + 1}").fg(TEXT_DIM).length(4),
                        text(stat.artist).fg(TEXT_PRIMARY).ellipsis().fill(),
                        text("${stat.playCount}×").fg(TEXT_SECONDARY).length(7),
                        text(formatByUnit(stat.totalMs, (state.screen as ScreenState.Stats).statsTimeUnit)).fg(PRIMARY_COLOR).length(10),
                    )
                }.toTypedArray()
            ).fill()
        )
    }
        .title("${MeloTheme.ICON_HEART} Top Artists")
        .rounded()
        .borderColor(BORDER_DEFAULT)

    val hints = text(
        "  [Tab/l] next period   [h] prev period   [u] cycle time unit   [r] refresh"
    ).fg(TEXT_DIM)

    return stack(
        ClearGraphicsElement().fill(),
        panel(
            column(
                row(periodTabs, spacer(), unitTabs),
                text(""),
                summaryPanel.length(5),
                text(""),
                dock()
                    .left(topTracksPanel.fill(), Constraint.percentage(55))
                    .center(topArtistsPanel.fill()),
                text(""),
                hints,
            )
        )
            .title("${MeloTheme.ICON_STATS} Statistics")
            .rounded()
            .borderColor(BORDER_DEFAULT)
            .focusedBorderColor(BORDER_FOCUSED)
            .focusable()
            .id("stats-panel")
            .onKeyEvent(onKeyEvent)
            .fill()
    )
}

private fun buildPeriodTabs(current: StatsPeriod): Element {
    val tabs = StatsPeriod.entries.map { period ->
        val selected = period == current
        val label = "  ${period.labelShort}  "
        if (selected) text(label).fg(PRIMARY_COLOR).bold()
        else text(label).fg(TEXT_DIM)
    }
    return row(*tabs.toTypedArray())
}

private fun buildUnitTabs(current: StatsTimeUnit): Element {
    val tabs = StatsTimeUnit.entries.map { unit ->
        val selected = unit == current
        val label = "  ${unit.label}  "
        if (selected) text(label).fg(PRIMARY_COLOR).bold()
        else text(label).fg(TEXT_DIM)
    }
    return row(*tabs.toTypedArray())
}