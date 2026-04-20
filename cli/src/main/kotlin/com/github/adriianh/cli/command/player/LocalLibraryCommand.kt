package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.PlayActionHandler
import com.github.adriianh.cli.command.player.util.ItemPicker
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.interactor.OfflineInteractors
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.terminal.Terminal
import com.varabyte.kotter.foundation.text.textLine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess
import com.varabyte.kotter.foundation.text.yellow as kotterYellow

class LocalLibraryCommand : NoOpCliktCommand(
    name = "library"
) {
    override fun help(context: Context): String = "Manage your local music library"

    init {
        subcommands(Scan(), List())
    }

    private class Scan : CliktCommand(name = "scan"), KoinComponent {
        private val paths by argument(help = "Directories to scan").multiple()
        private val terminal = Terminal()

        override fun help(context: Context): String = "Scan directories for music files"

        override fun run() {
            startKoin { modules(appModule) }
            try {
                val offlineInteractors: OfflineInteractors by inject()
                runBlocking {
                    if (paths.isEmpty()) {
                        terminal.println("Please provide at least one path to scan.")
                        return@runBlocking
                    }

                    terminal.println(cyan("Scanning ${paths.size} paths..."))
                    val tracks = offlineInteractors.scanLocalTracks(paths)

                    if (tracks.isEmpty()) {
                        terminal.println("No music files found in the provided paths.")
                    } else {
                        terminal.println(green("Found ${tracks.size} tracks."))
                        tracks.take(10).forEach {
                            terminal.println("  - ${it.title} by ${it.artist}")
                        }
                        if (tracks.size > 10) {
                            terminal.println(gray("  ... and ${tracks.size - 10} more"))
                        }
                    }
                }
            } finally {
                stopKoin()
                exitProcess(0)
            }
        }
    }

    private class List : CliktCommand(name = "list"), KoinComponent {
        private val terminal = Terminal()

        override fun help(context: Context): String = "List all offline tracks"

        override fun run() {
            startKoin { modules(appModule) }
            try {
                val offlineInteractors: OfflineInteractors by inject()
                val getStream: GetStreamUseCase by inject()

                runBlocking {
                    terminal.println(gray("Loading library..."))
                    val offlineTracks = offlineInteractors.getOfflineTracks().first()

                    if (offlineTracks.isEmpty()) {
                        terminal.println("Your library is empty. Use 'scan' or download tracks.")
                        return@runBlocking
                    }

                    val selected = ItemPicker.pickItem(
                        offlineTracks,
                        "Local Library (${offlineTracks.size} tracks)"
                    ) { _, item, isSelected ->
                        if (isSelected) {
                            kotterYellow { textLine("> ${item.track.title} - ${item.track.artist}") }
                        } else {
                            textLine("  ${item.track.title} - ${item.track.artist}")
                        }
                    } ?: return@runBlocking

                    PlayActionHandler.playTrack(selected.track, getStream, terminal)
                }
            } finally {
                stopKoin()
                exitProcess(0)
            }
        }
    }
}