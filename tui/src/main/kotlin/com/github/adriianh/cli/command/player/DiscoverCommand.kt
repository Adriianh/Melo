package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.PlayActionHandler
import com.github.adriianh.cli.command.player.util.ItemPicker
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.interactor.DiscoveryInteractors
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.search.GetEntityDetailsUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.rendering.TextColors.cyan
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

class DiscoverCommand : NoOpCliktCommand(
    name = "discover"
) {
    override fun help(context: Context): String = "Discover new music (home, trending, explore)"

    init {
        subcommands(Home(), Trending(), Explore())
    }

    private class Home : CliktCommand(name = "home"), KoinComponent {
        private val terminal = Terminal()
        override fun help(context: Context): String = "Show home recommendations"
        override fun run() {
            startKoin { modules(appModule) }
            try {
                val discovery: DiscoveryInteractors by inject()
                val getStream: GetStreamUseCase by inject()
                val getEntityDetails: GetEntityDetailsUseCase by inject()

                runBlocking {
                    terminal.println(gray("Fetching home recommendations..."))
                    val sections = discovery.getHome().sections
                    if (sections.isEmpty()) {
                        terminal.println("No recommendations found.")
                        return@runBlocking
                    }

                    val selectedSection =
                        ItemPicker.pickItem(sections, "Select Section") { _, item, isSelected ->
                            if (isSelected) {
                                kotterYellow { textLine("> ${item.title}") }
                            } else {
                                textLine("  ${item.title}")
                            }
                        } ?: return@runBlocking

                    val selectedItem = ItemPicker.pickItem(
                        selectedSection.items,
                        selectedSection.title
                    ) { _, item, isSelected ->
                        val text = when (item) {
                            is SearchResult.Song -> "[Song] ${item.track.title} - ${item.track.artist}"
                            is SearchResult.Album -> "[Album] ${item.title} - ${item.author}"
                            is SearchResult.Playlist -> "[Playlist] ${item.title} - ${item.author}"
                            is SearchResult.Artist -> "[Artist] ${item.name}"
                        }
                        if (isSelected) {
                            kotterYellow { textLine("> $text") }
                        } else {
                            textLine("  $text")
                        }
                    } ?: return@runBlocking

                    handleSelection(selectedItem, getStream, getEntityDetails, terminal)
                }
            } finally {
                stopKoin()
                exitProcess(0)
            }
        }
    }

    private class Trending : CliktCommand(name = "trending"), KoinComponent {
        private val terminal = Terminal()
        override fun help(context: Context): String = "Show trending tracks"
        override fun run() {
            startKoin { modules(appModule) }
            try {
                val discovery: DiscoveryInteractors by inject()
                val getStream: GetStreamUseCase by inject()

                runBlocking {
                    terminal.println(gray("Fetching trending tracks..."))
                    val tracks = discovery.getTrending()
                    if (tracks.isEmpty()) {
                        terminal.println("No trending tracks found.")
                        return@runBlocking
                    }

                    val selectedTrack =
                        ItemPicker.pickItem(tracks, "Trending Tracks") { _, item, isSelected ->
                            if (isSelected) {
                                kotterYellow { textLine("> ${item.title} - ${item.artist}") }
                            } else {
                                textLine("  ${item.title} - ${item.artist}")
                            }
                        } ?: return@runBlocking

                    PlayActionHandler.playTrack(selectedTrack, getStream, terminal)
                }
            } finally {
                stopKoin()
                exitProcess(0)
            }
        }
    }

    private class Explore : CliktCommand(name = "explore"), KoinComponent {
        private val terminal = Terminal()
        override fun help(context: Context): String = "Explore new releases"
        override fun run() {
            startKoin { modules(appModule) }
            try {
                val discovery: DiscoveryInteractors by inject()
                val getStream: GetStreamUseCase by inject()
                val getEntityDetails: GetEntityDetailsUseCase by inject()

                runBlocking {
                    terminal.println(gray("Fetching new releases..."))
                    val sections = discovery.getExplore()
                    if (sections.isEmpty()) {
                        terminal.println("No new releases found.")
                        return@runBlocking
                    }

                    val selectedSection =
                        ItemPicker.pickItem(sections, "Explore") { _, item, isSelected ->
                            if (isSelected) {
                                kotterYellow { textLine("> ${item.title}") }
                            } else {
                                textLine("  ${item.title}")
                            }
                        } ?: return@runBlocking

                    val selectedItem = ItemPicker.pickItem(
                        selectedSection.items,
                        selectedSection.title
                    ) { _, item, isSelected ->
                        val text = when (item) {
                            is SearchResult.Album -> "${item.title} - ${item.author}"
                            else -> item.toString()
                        }
                        if (isSelected) {
                            kotterYellow { textLine("> $text") }
                        } else {
                            textLine("  $text")
                        }
                    } ?: return@runBlocking

                    handleSelection(selectedItem, getStream, getEntityDetails, terminal)
                }
            } finally {
                stopKoin()
                exitProcess(0)
            }
        }
    }

    companion object {
        private suspend fun handleSelection(
            item: SearchResult,
            getStream: GetStreamUseCase,
            getEntityDetails: GetEntityDetailsUseCase,
            terminal: Terminal
        ) {
            when (item) {
                is SearchResult.Song -> PlayActionHandler.playTrack(item.track, getStream, terminal)
                is SearchResult.Album -> {
                    val detailed = getEntityDetails(item) as? SearchResult.Album
                    detailed?.songs?.let {
                        PlayActionHandler.playMultiple(detailed.title, it, getStream, terminal)
                    }
                }

                is SearchResult.Playlist -> {
                    val detailed = getEntityDetails(item) as? SearchResult.Playlist
                    detailed?.songs?.let {
                        PlayActionHandler.playMultiple(detailed.title, it, getStream, terminal)
                    }
                }

                is SearchResult.Artist -> {
                    terminal.println(cyan("Artist details not yet implemented in CLI discovery."))
                }
            }
        }
    }
}