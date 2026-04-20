package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.util.ItemPicker
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.terminal.Terminal
import com.varabyte.kotter.foundation.text.textLine
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess
import com.varabyte.kotter.foundation.text.yellow as kotterYellow

class QueueCommand : CliktCommand(name = "queue") {
    override fun help(context: Context): String = "Manage the playback queue"

    init {
        subcommands(
            QueueAddCommand(),
            QueueListCommand(),
            QueueRemoveCommand(),
            QueueClearCommand()
        )
    }

    override fun run() {}
}

class QueueAddCommand : CliktCommand(name = "add"), KoinComponent {
    private val query by argument(name = "query", help = "Track name to add")
    private val interactive by option(
        "-i",
        "--interactive",
        help = "Interactively select from search results"
    ).flag(default = false)
    private val terminal = Terminal()
    private val json = Json { ignoreUnknownKeys = true }

    override fun help(context: Context): String = "Add a track to the queue"

    override fun run() {
        startKoin { modules(appModule) }
        try {
            val searchTracks: SearchTracksUseCase by inject()
            runBlocking {
                terminal.println(gray("Searching for '$query'..."))
                val tracks = searchTracks(query)
                val track =
                    selectInteractive(tracks) { "${it.title} by ${it.artist}" }

                if (track == null) {
                    terminal.println("No track results found or selection cancelled for '$query'.")
                    return@runBlocking
                }

                val payload = json.encodeToString(Track.serializer(), track)
                val result = LocalIpcClient.sendCommand("QUEUE_ADD", payload)
                if (result.startsWith("ERROR")) terminal.println(result)
                else terminal.println("Successfully added to queue: ${track.title}")
            }
        } finally {
            stopKoin()
            exitProcess(0)
        }
    }

    private fun <T> selectInteractive(
        results: List<T>,
        titleSelector: (T) -> String
    ): T? {
        if (results.isEmpty()) return null
        if (!interactive || results.size == 1) return results.first()

        val limited = results.take(15)
        val selected = ItemPicker.pickItem(limited, "Select Track") { _, item, isSelected ->
            if (isSelected) {
                kotterYellow { textLine("> " + titleSelector(item)) }
            } else {
                textLine("  " + titleSelector(item))
            }
        }

        if (selected == null) terminal.println(gray("Selection cancelled."))
        return selected
    }
}

class QueueListCommand : CliktCommand(name = "list") {
    private val terminal = Terminal()
    private val json = Json { ignoreUnknownKeys = true }

    override fun help(context: Context): String = "List upcoming tracks in the queue"

    override fun run() {
        val result = LocalIpcClient.sendCommand("QUEUE_LIST")
        if (result.startsWith("ERROR")) {
            terminal.println(result)
        } else if (result.startsWith("OK ")) {
            val payload = result.removePrefix("OK ")
            try {
                val queue = json.decodeFromString(ListSerializer(Track.serializer()), payload)
                if (queue.isEmpty()) {
                    terminal.println("The queue is empty.")
                } else {
                    terminal.println("Upcoming Tracks in Queue:")
                    queue.forEachIndexed { i, t ->
                        terminal.println("${i}. ${t.title} by ${t.artist}")
                    }
                }
            } catch (e: Exception) {
                terminal.println("ERROR Failed to parse queue data: ${e.message}")
                terminal.println(gray("Raw payload: $payload"))
            }
        }
    }
}

class QueueRemoveCommand : CliktCommand(name = "remove") {
    private val index by argument(
        name = "index",
        help = "Index of the track to remove (0-based from 'queue list')"
    )
    private val terminal = Terminal()

    override fun help(context: Context): String = "Remove a track from the queue"

    override fun run() {
        val idx = index.toIntOrNull()
        if (idx == null) {
            terminal.println("Please provide a valid numeric index.")
            return
        }
        val result = LocalIpcClient.sendCommand("QUEUE_REMOVE", idx.toString())
        if (result.startsWith("ERROR")) terminal.println(result)
        else terminal.println("Track at index $idx removed.")
    }
}

class QueueClearCommand : CliktCommand(name = "clear") {
    private val terminal = Terminal()

    override fun help(context: Context): String = "Clear all upcoming tracks from the queue"

    override fun run() {
        val result = LocalIpcClient.sendCommand("QUEUE_CLEAR")
        if (result.startsWith("ERROR")) terminal.println(result)
        else terminal.println("Queue cleared.")
    }
}