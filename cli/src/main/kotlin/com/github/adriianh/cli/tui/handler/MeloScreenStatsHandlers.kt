package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.StatsTimeUnit
import com.github.adriianh.core.domain.model.StatsPeriod
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch

internal fun MeloScreen.loadStats(period: StatsPeriod = state.statsPeriod) {
    scope.launch {
        appRunner()?.runOnRenderThread { state = state.copy(statsLoading = true, statsPeriod = period) }
        val tracks = getTopTracks(period)
        val artists = getTopArtists(period)
        val listening = getListeningStats(period)
        appRunner()?.runOnRenderThread {
            state = state.copy(
                statsTopTracks = tracks,
                statsTopArtists = artists,
                statsListening = listening,
                statsLoading = false,
            )
        }
    }
}

internal fun MeloScreen.handleStatsKey(event: KeyEvent): EventResult {
    val periods = StatsPeriod.entries
    val units = StatsTimeUnit.entries
    val current = state.statsPeriod
    when {
        event.code() == KeyCode.TAB ||
        event.code() == KeyCode.CHAR && event.character() == 'l' -> {
            val next = periods[(current.ordinal + 1) % periods.size]
            loadStats(next)
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'h' -> {
            val prev = periods[(current.ordinal - 1 + periods.size) % periods.size]
            loadStats(prev)
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'u' -> {
            val next = units[(state.statsTimeUnit.ordinal + 1) % units.size]
            state = state.copy(statsTimeUnit = next)
            return EventResult.HANDLED
        }
        event.code() == KeyCode.CHAR && event.character() == 'r' -> {
            loadStats(current)
            return EventResult.HANDLED
        }
    }
    return EventResult.UNHANDLED
}