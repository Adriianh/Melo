package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.usecase.search.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.InputStreamReader

class LyricsCommand : CliktCommand(
    name = "lyrics"
), KoinComponent {
    private val query by argument(
        name = "query",
        help = "Track name to search lyrics for"
    ).optional()
    private val current by option(
        "-c", "--current",
        help = "Fetch lyrics for the track currently playing in Melo via MPRIS"
    ).flag(default = false)

    private val terminal = Terminal()

    override fun help(context: Context): String =
        "Fetch and display lyrics for a given track or the currently playing track via MPRIS"

    override fun run() {
        startKoin { modules(appModule) }

        try {
            val getLyrics: GetLyricsUseCase by inject()
            val searchTracks: SearchTracksUseCase by inject()
            runBlocking {
                if (current) {
                    val process = ProcessBuilder(
                        "playerctl",
                        "-p",
                        "melo,melo*",
                        "metadata",
                        "--format",
                        "{{title}}|||{{artist}}"
                    )
                        .redirectErrorStream(true)
                        .start()

                    val output = InputStreamReader(process.inputStream).readText().trim()
                    process.waitFor()

                    if (process.exitValue() != 0 || output.isBlank() || output == "No players found") {
                        terminal.println(gray("No Melo player is currently running."))
                        return@runBlocking
                    }

                    val parts = output.split("|||")
                    val title = parts.getOrNull(0)?.trim() ?: "Unknown"
                    val artist = parts.getOrNull(1)?.trim() ?: "Unknown"
                    terminal.println(cyan("Fetching lyrics for currently playing: $title by $artist..."))

                    val lyricsText = getLyrics(artist, title)
                    if (!lyricsText.isNullOrBlank()) {
                        terminal.println(lyricsText)
                    } else {
                        terminal.println(gray("No lyrics found for currently playing track."))
                    }
                } else if (!query.isNullOrBlank()) {
                    terminal.println(gray("Searching for track '$query'..."))

                    val track = searchTracks(query!!).firstOrNull()
                    if (track == null) {
                        terminal.println(gray("No track found for query: $query"))
                        return@runBlocking
                    }

                    terminal.println(cyan("Fetching lyrics for ${track.title} by ${track.artist}..."))
                    val lyricsText = getLyrics(track.artist, track.title)
                    if (!lyricsText.isNullOrBlank()) {
                        terminal.println(lyricsText)
                    } else {
                        terminal.println(gray("No lyrics found for ${track.title} by ${track.artist}."))
                    }
                } else {
                    terminal.println(gray("You must provide either a query or use the --current flag."))
                }
            }
        } catch (e: Exception) {
            terminal.println(gray("Error fetching lyrics: ${e.message}"))
        } finally {
            stopKoin()
        }
    }
}
