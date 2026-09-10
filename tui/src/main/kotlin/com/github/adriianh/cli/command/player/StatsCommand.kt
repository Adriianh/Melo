package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.interactor.StatsInteractors
import com.github.adriianh.core.domain.model.StatsPeriod
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class StatsCommand : CliktCommand(name = "stats"), KoinComponent {
    private val terminal = Terminal()
    private val period by option(
        "-p",
        "--period",
        help = "Stats period (WEEK, MONTH, YEAR, ALL_TIME)"
    )
        .enum<StatsPeriod>()
        .default(StatsPeriod.WEEK)

    override fun help(context: Context): String = "View your listening statistics"

    override fun run() {
        if (GlobalContext.getOrNull() == null) {
            startKoin { modules(appModule) }
        }
        try {
            runBlocking {
                val stats: StatsInteractors by inject()

                terminal.println(cyan("=== Listening Stats ($period) ==="))

                val topTracks = stats.getTopTracks(period, limit = 5)
                if (topTracks.isNotEmpty()) {
                    terminal.println(green("\nTop Tracks:"))
                    topTracks.forEachIndexed { i, t ->
                        terminal.println("${gray("${i + 1}.")} ${t.track.title} - ${t.track.artist} (${t.playCount} plays)")
                    }
                }

                val topArtists = stats.getTopArtists(period, limit = 5)
                if (topArtists.isNotEmpty()) {
                    terminal.println(green("\nTop Artists:"))
                    topArtists.forEachIndexed { i, a ->
                        terminal.println("${gray("${i + 1}.")} ${a.artist} (${a.playCount} plays)")
                    }
                }

                val general = stats.getListeningStats(period)
                terminal.println(green("\nSummary:"))
                terminal.println("Total Tracks Played: ${yellow(general.totalPlays.toString())}")
                terminal.println("Unique Artists: ${yellow(general.uniqueArtists.toString())}")

                val totalMin = general.totalMs / 1000 / 60
                terminal.println("Total Listening Time: ${yellow("${totalMin} min")}")
            }
        } finally {
            stopKoin()
        }
    }
}
