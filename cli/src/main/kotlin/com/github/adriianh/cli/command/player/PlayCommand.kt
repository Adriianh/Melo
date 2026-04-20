package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.PlayActionHandler
import com.github.adriianh.cli.command.player.util.ItemPicker
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.search.GetEntityDetailsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchAlbumsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.terminal.Terminal
import com.varabyte.kotter.foundation.text.textLine
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import com.varabyte.kotter.foundation.text.yellow as kotterYellow

class PlayCommand : CliktCommand(
    name = "play"
), KoinComponent {
    private val query by argument(name = "query", help = "Track name to play")
    private val type by option(
        "-t",
        "--type",
        help = "Type of playback (track, album, playlist)"
    ).default("track")
    private val interactive by option(
        "-i",
        "--interactive",
        help = "Interactively select from search results"
    ).flag(default = false)

    private val terminal = Terminal()

    override fun help(context: Context): String =
        "Play the best matching track, album, or playlist for the given query directly"

    override fun run() {
        startKoin { modules(appModule) }

        try {
            val searchTracks: SearchTracksUseCase by inject()
            val searchAlbums: SearchAlbumsUseCase by inject()
            val searchPlaylists: SearchPlaylistsUseCase by inject()
            val getEntityDetails: GetEntityDetailsUseCase by inject()
            val getStream: GetStreamUseCase by inject()

            runBlocking {
                terminal.println(gray("Searching for '$query' as $type..."))
                when (type.lowercase()) {
                    "album" -> {
                        val albums = searchAlbums(query)
                        val album = selectInteractive(
                            albums,
                            "Select Album"
                        ) { "${it.title} by ${it.author}" }
                        if (album == null) {
                            terminal.println("No album found or selection cancelled for '$query'.")
                            return@runBlocking
                        }
                        val detailedAlbum = getEntityDetails(album) as SearchResult.Album
                        val tracks = detailedAlbum.songs
                        if (tracks.isNullOrEmpty()) {
                            terminal.println("No tracks found in album '${album.title}'.")
                            return@runBlocking
                        }
                        if (delegateToDaemon(tracks)) {
                            terminal.println(green("Added album tracks to active session: ") + detailedAlbum.title)
                        } else {
                            PlayActionHandler.playMultiple(
                                contextName = detailedAlbum.title,
                                tracks = tracks,
                                getStream = getStream,
                                terminal = terminal
                            )
                        }
                    }

                    "playlist" -> {
                        val playlists = searchPlaylists(query)
                        val playlist = selectInteractive(
                            playlists,
                            "Select Playlist"
                        ) { "${it.title} by ${it.author}" }
                        if (playlist == null) {
                            terminal.println("No playlist found or selection cancelled for '$query'.")
                            return@runBlocking
                        }
                        val detailedPlaylist = getEntityDetails(playlist) as SearchResult.Playlist
                        val tracks = detailedPlaylist.songs
                        if (tracks.isNullOrEmpty()) {
                            terminal.println("No tracks found in playlist '${playlist.title}'.")
                            return@runBlocking
                        }
                        if (delegateToDaemon(tracks)) {
                            terminal.println(green("Added playlist tracks to active session: ") + detailedPlaylist.title)
                        } else {
                            PlayActionHandler.playMultiple(
                                contextName = detailedPlaylist.title,
                                tracks = tracks,
                                getStream = getStream,
                                terminal = terminal
                            )
                        }
                    }

                    else -> {
                        val tracks = searchTracks(query)
                        val track = selectInteractive(
                            tracks,
                            "Select Track"
                        ) { "${it.title} by ${it.artist}" }

                        if (track == null) {
                            terminal.println("No track results found or selection cancelled for '$query'.")
                            return@runBlocking
                        }

                        if (delegateToDaemon(listOf(track))) {
                            terminal.println(green("Added to active session: ") + "${track.title} by ${track.artist}")
                        } else {
                            PlayActionHandler.playTrack(track, getStream, terminal)
                        }
                    }
                }
            }
        } finally {
            stopKoin()
        }
    }

    private fun delegateToDaemon(tracks: List<Track>): Boolean {
        val firstRes = LocalIpcClient.sendCommand("QUEUE_LIST")
        if (firstRes.startsWith("ERROR")) return false

        val json = Json { ignoreUnknownKeys = true }
        tracks.forEach { track ->
            val payload = json.encodeToString(track)
            LocalIpcClient.sendCommand("QUEUE_ADD", payload)
        }
        return true
    }

    private fun <T> selectInteractive(
        results: List<T>,
        title: String,
        titleSelector: (T) -> String
    ): T? {
        if (results.isEmpty()) return null
        if (!interactive || results.size == 1) return results.first()

        val limited = results.take(15)
        val selected = ItemPicker.pickItem(limited, title) { _, item, isSelected ->
            if (isSelected) {
                kotterYellow { textLine("> " + titleSelector(item)) }
            } else {
                textLine("  " + titleSelector(item))
            }
        }

        if (selected == null) {
            terminal.println(gray("Selection cancelled."))
        }
        return selected
    }
}