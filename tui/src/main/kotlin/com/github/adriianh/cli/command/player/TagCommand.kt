package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.interactor.OfflineInteractors
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess

class TagCommand :
    CliktCommand(
        name = "tag",
    ),
    KoinComponent {
    private val id by argument(help = "Track ID to inspect or edit (e.g. local:/path/to/song.mp3)")

    private val title by option("--title", help = "New title tag")
    private val artist by option("--artist", help = "New artist tag")
    private val album by option("--album", help = "New album tag")

    private val terminal = Terminal()

    override fun help(context: Context): String = "View or edit ID3 tags and metadata of local audio tracks."

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo tag \"local:/path/to/song.mp3\"\n" +
            "  melo tag \"local:/path/to/song.mp3\" --title \"Song Title\" --artist \"Artist Name\"\n" +
            "  melo tag \"local:/path/to/song.mp3\" --album \"Album Name\""

    override fun run() {
        startKoin { modules(appModule) }
        try {
            val offlineInteractors: OfflineInteractors by inject()
            runBlocking {
                val track = offlineInteractors.getOfflineTracks().first().find { it.track.id == id }

                if (track == null) {
                    terminal.println(red("Track with ID '$id' not found in your library."))
                    return@runBlocking
                }

                if (title == null && artist == null && album == null) {
                    terminal.println(cyan("Metadata for ${track.track.title}:"))
                    terminal.println("  ID: ${track.track.id}")
                    terminal.println("  Title: ${track.track.title}")
                    terminal.println("  Artist: ${track.track.artist}")
                    terminal.println("  Album: ${track.track.album}")
                    terminal.println("  Path: ${track.localFilePath ?: "Not downloaded"}")
                } else {
                    if (track.localFilePath == null) {
                        terminal.println(red("Track is not downloaded. Metadata can only be edited for local files."))
                        return@runBlocking
                    }

                    terminal.println(cyan("Updating metadata..."))
                    offlineInteractors.updateTrackMetadata(id, title, artist, album)
                    terminal.println(green("Metadata updated successfully."))
                }
            }
        } finally {
            stopKoin()
            exitProcess(0)
        }
    }
}
