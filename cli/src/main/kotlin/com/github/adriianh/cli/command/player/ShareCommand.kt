package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ShareCommand : CliktCommand(
    name = "share"
), KoinComponent {
    private val terminal = Terminal()
    private val query by argument().optional()
    private val searchTracks: SearchTracksUseCase by inject()

    override fun help(context: Context): String =
        "Share the current track or search for a track link"

    override fun run() = runBlocking {
        if (query == null) {
            val result = LocalIpcClient.sendCommand("GET_CURRENT_TRACK")
            if (result.startsWith("ERROR")) {
                terminal.println(yellow("No track is currently playing in the daemon."))
                terminal.println(cyan("Hint: Provide a search query to share a specific track, e.g., melo share 'Never Gonna Give You Up'"))
            } else {
                val track = Json.decodeFromString<Track>(result)
                printTrackLink(track)
            }
        } else {
            terminal.println(cyan("Searching for track link..."))
            val tracks = searchTracks(query!!)
            if (tracks.isEmpty()) {
                terminal.println(yellow("No tracks found for '$query'"))
            } else {
                printTrackLink(tracks.first())
            }
        }
    }

    private fun printTrackLink(track: Track) {
        val trackId = track.id.split(":").last()
        val link = "https://music.youtube.com/watch?v=${trackId}"
        terminal.println(green("Track Found: ") + track.title + " by " + track.artist)
        terminal.println(cyan("Link: ") + link)
    }
}