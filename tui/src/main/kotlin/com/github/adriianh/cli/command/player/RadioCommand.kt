package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.PlayActionHandler
import com.github.adriianh.cli.command.player.util.ItemPicker
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.service.DaemonManager
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.varabyte.kotter.foundation.text.textLine
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess
import com.varabyte.kotter.foundation.text.yellow as kotterYellow

class RadioCommand :
    CliktCommand(
        name = "radio",
    ),
    KoinComponent {
    private val query by argument(name = "query", help = "Seed track or artist name for the radio station")
    private val interactive by option(
        "-i",
        "--interactive",
        help = "Interactively select seed track from search results",
    ).flag(default = false)
    private val foreground by option(
        "-f",
        "--foreground",
        help = "Run in foreground with progress bar instead of detaching to daemon",
    ).flag(default = false)

    private val terminal = Terminal()

    override fun help(context: Context): String = "Start an endless radio station based on a seed track or artist."

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo radio \"Comfortably Numb\"\n" +
            "  melo radio \"Daft Punk\" --interactive\n" +
            "  melo radio \"Starboy\" --foreground"

    override fun run() {
        startKoin { modules(appModule) }

        try {
            val searchTracks: SearchTracksUseCase by inject()
            val getRadio: GetRadioUseCase by inject()
            val getStream: GetStreamUseCase by inject()

            runBlocking {
                terminal.println(gray("Searching for seed track '$query'..."))
                val tracks = searchTracks(query)
                val seedTrack = selectSeedTrack(tracks)

                if (seedTrack == null) {
                    terminal.println("No seed track found or selection cancelled for '$query'.")
                    return@runBlocking
                }

                val videoId =
                    seedTrack.sourceId
                        ?: (if (seedTrack.id.startsWith("piped:")) seedTrack.id.removePrefix("piped:") else null)
                        ?: getStream.resolveSourceId(seedTrack)
                        ?: seedTrack.id
                val radioTracks = getRadio(videoId)

                if (radioTracks.isEmpty()) {
                    terminal.println("Could not generate radio for '${seedTrack.title}'.")
                    return@runBlocking
                }

                dispatchRadioPlayback(seedTrack, radioTracks, getStream)
            }
        } finally {
            stopKoin()
            exitProcess(0)
        }
    }

    private fun selectSeedTrack(tracks: List<Track>): Track? =
        when {
            tracks.isEmpty() -> null
            !interactive || tracks.size == 1 -> tracks.first()
            else -> {
                val limited = tracks.take(15)
                ItemPicker.pickItem(limited, "Select Seed Track") { _, item, isSelected ->
                    if (isSelected) {
                        kotterYellow { textLine("> ${item.title} by ${item.artist}") }
                    } else {
                        textLine("  ${item.title} by ${item.artist}")
                    }
                }
            }
        }

    private suspend fun dispatchRadioPlayback(
        seedTrack: Track,
        radioTracks: List<Track>,
        getStream: GetStreamUseCase,
    ) {
        if (foreground) {
            PlayActionHandler.playMultiple(
                contextName = "Radio: ${seedTrack.title}",
                tracks = radioTracks,
                getStream = getStream,
                terminal = terminal,
            )
            return
        }

        if (DaemonManager.ensureDaemonRunning(terminal)) {
            DaemonManager.playTracks(radioTracks)
            terminal.println(
                green("▶ Started radio: ") + seedTrack.title + gray(" by ") + seedTrack.artist +
                    gray(" (${radioTracks.size} tracks)"),
            )
            terminal.println(
                gray("Playing in background. Use 'melo status' to inspect, 'melo next' to skip."),
            )
            return
        }

        terminal.println(yellow("Warning: Could not start daemon. Falling back to foreground playback."))
        PlayActionHandler.playMultiple(
            contextName = "Radio: ${seedTrack.title}",
            tracks = radioTracks,
            getStream = getStream,
            terminal = terminal,
        )
    }
}
