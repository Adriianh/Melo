package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.PlayActionHandler
import com.github.adriianh.cli.command.player.util.ItemPicker
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.search.GetRadioUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.terminal.Terminal
import com.varabyte.kotter.foundation.text.textLine
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess
import com.varabyte.kotter.foundation.text.yellow as kotterYellow

class RadioCommand : CliktCommand(
    name = "radio"
), KoinComponent {
    private val query by argument(name = "query", help = "Seed track name for radio")
    private val interactive by option(
        "-i",
        "--interactive",
        help = "Interactively select seed track from search results"
    ).flag(default = false)

    private val terminal = Terminal()

    override fun help(context: Context): String =
        "Start an infinite radio session based on a seed track"

    override fun run() {
        startKoin { modules(appModule) }

        try {
            val searchTracks: SearchTracksUseCase by inject()
            val getRadio: GetRadioUseCase by inject()
            val getStream: GetStreamUseCase by inject()

            runBlocking {
                terminal.println(gray("Searching for seed track '$query'..."))
                val tracks = searchTracks(query)
                val seedTrack = if (tracks.isEmpty()) {
                    null
                } else if (!interactive || tracks.size == 1) {
                    tracks.first()
                } else {
                    val limited = tracks.take(15)
                    ItemPicker.pickItem(limited, "Select Seed Track") { _, item, isSelected ->
                        if (isSelected) {
                            kotterYellow { textLine("> ${item.title} by ${item.artist}") }
                        } else {
                            textLine("  ${item.title} by ${item.artist}")
                        }
                    }
                }

                if (seedTrack == null) {
                    terminal.println("No seed track found or selection cancelled for '$query'.")
                    return@runBlocking
                }

                terminal.println(gray("Starting radio for '${seedTrack.title}'..."))
                val radioTracks =
                    getRadio(seedTrack.sourceId ?: seedTrack.id.removePrefix("piped:"))

                if (radioTracks.isEmpty()) {
                    terminal.println("Could not generate radio for '${seedTrack.title}'.")
                    return@runBlocking
                }

                PlayActionHandler.playMultiple(
                    contextName = "Radio: ${seedTrack.title}",
                    tracks = radioTracks,
                    getStream = getStream,
                    terminal = terminal
                )
            }
        } finally {
            stopKoin()
            exitProcess(0)
        }
    }
}