package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.PlayActionHandler
import com.github.adriianh.cli.command.player.util.ItemPicker
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.usecase.playback.GetRecentTracksUseCase
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors.gray
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

class HistoryCommand : CliktCommand(
    name = "history"
), KoinComponent {
    override fun help(context: Context): String = "Show recently played tracks"
    private val limit by option("-l", "--limit", help = "Number of tracks to show").int()
        .default(20)

    private val terminal = Terminal()

    override fun run() {
        startKoin { modules(appModule) }
        try {
            val getRecent: GetRecentTracksUseCase by inject()
            val getStream: GetStreamUseCase by inject()

            runBlocking {
                terminal.println(gray("Fetching recently played tracks..."))
                val entries = getRecent(limit).first()
                if (entries.isEmpty()) {
                    terminal.println("History is empty.")
                    return@runBlocking
                }

                val selectedEntry =
                    ItemPicker.pickItem(entries, "Playback History") { _, item, isSelected ->
                        if (isSelected) {
                            kotterYellow { textLine("> ${item.track.title} - ${item.track.artist}") }
                        } else {
                            textLine("  ${item.track.title} - ${item.track.artist}")
                        }
                    } ?: return@runBlocking

                PlayActionHandler.playTrack(selectedEntry.track, getStream, terminal)
            }
        } finally {
            stopKoin()
            exitProcess(0)
        }
    }
}