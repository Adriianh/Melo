package com.github.adriianh.cli.tui.handler

import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.cli.tui.StatsTimeUnit
import com.github.adriianh.core.domain.model.StatsPeriod
import dev.tamboui.toolkit.event.EventResult
import dev.tamboui.tui.event.KeyCode
import dev.tamboui.tui.event.KeyEvent
import kotlinx.coroutines.launch

internal fun MeloScreen.loadStats(period: StatsPeriod? = null) {
    val current = (state.screen as? ScreenState.Stats) ?: cachedStatsScreen
    val currentPeriod = period ?: current.statsPeriod
    if (current.statsLoading && period == null) return
    scope.launch {
        appRunner()?.runOnRenderThread {
            updateScreen<ScreenState.Stats> {
                it.copy(
                    statsLoading = true,
                    statsPeriod = currentPeriod
                )
            }
        }
        try {
            val tracks = getTopTracks(currentPeriod)
            val artists = getTopArtists(currentPeriod)
            val listening = getListeningStats(currentPeriod)
            appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Stats> {
                    it.copy(
                        statsTopTracks = tracks,
                        statsTopArtists = artists,
                        statsListening = listening,
                        statsLoading = false,
                    )
                }
            }
        } catch (_: Exception) {
            appRunner()?.runOnRenderThread {
                updateScreen<ScreenState.Stats> { it.copy(statsLoading = false) }
            }
        }
    }
}

internal fun MeloScreen.handleStatsKey(event: KeyEvent): EventResult {
    val s = state.screen as? ScreenState.Stats ?: return handleGlobalShortcuts(event)
    val periods = StatsPeriod.entries
    val units = StatsTimeUnit.entries
    val current = s.statsPeriod
    when {
        event.code() == KeyCode.TAB || event.isCharIgnoreCase('l') -> {
            val next = periods[(current.ordinal + 1) % periods.size]
            loadStats(next)
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('h') -> {
            val prev = periods[(current.ordinal - 1 + periods.size) % periods.size]
            loadStats(prev)
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('u') -> {
            val next = units[(s.statsTimeUnit.ordinal + 1) % units.size]
            updateScreen<ScreenState.Stats> { it.copy(statsTimeUnit = next) }
            return EventResult.HANDLED
        }

        event.isCharIgnoreCase('r') -> {
            loadStats(current)
            return EventResult.HANDLED
        }
    }
    return handleGlobalShortcuts(event)
}