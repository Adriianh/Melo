package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.PlayActionHandler
import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.di.appModule
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
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess

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
        if (resolveEnv("LASTFM_API_KEY") == null) {
            echo(Messages.get("error.missing_lastfm_key", "configDir" to configDir), err = true)
            exitProcess(1)
        }

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
                        val album = selectInteractive(albums) { "${it.title} by ${it.author}" }
                        if (album == null) {
                            terminal.println("No album found for '$query'.")
                            return@runBlocking
                        }
                        val detailedAlbum = getEntityDetails(album) as SearchResult.Album
                        val tracks = detailedAlbum.songs
                        if (tracks.isNullOrEmpty()) {
                            terminal.println("No tracks found in album '${album.title}'.")
                            return@runBlocking
                        }
                        PlayActionHandler.playMultiple(
                            contextName = detailedAlbum.title,
                            tracks = tracks,
                            getStream = getStream,
                            terminal = terminal
                        )
                    }

                    "playlist" -> {
                        val playlists = searchPlaylists(query)
                        val playlist =
                            selectInteractive(playlists) { "${it.title} by ${it.author}" }
                        if (playlist == null) {
                            terminal.println("No playlist found for '$query'.")
                            return@runBlocking
                        }
                        val detailedPlaylist = getEntityDetails(playlist) as SearchResult.Playlist
                        val tracks = detailedPlaylist.songs
                        if (tracks.isNullOrEmpty()) {
                            terminal.println("No tracks found in playlist '${playlist.title}'.")
                            return@runBlocking
                        }
                        PlayActionHandler.playMultiple(
                            contextName = detailedPlaylist.title,
                            tracks = tracks,
                            getStream = getStream,
                            terminal = terminal
                        )
                    }

                    else -> {
                        val tracks = searchTracks(query)
                        val track = selectInteractive(tracks) { "${it.title} by ${it.artist}" }
                        if (track == null) {
                            terminal.println("No results found for '$query'.")
                            return@runBlocking
                        }
                        PlayActionHandler.playTrack(track, getStream, terminal)
                    }
                }
            }
        } finally {
            stopKoin()
        }
    }

    private fun <T> selectInteractive(results: List<T>, titleSelector: (T) -> String): T? {
        if (results.isEmpty()) return null
        if (!interactive || results.size == 1) return results.first()

        val limited = results.take(10)
        terminal.println(cyan("Please select an option:"))
        limited.forEachIndexed { index, item ->
            terminal.println(yellow("${index + 1}.") + " " + titleSelector(item))
        }
        terminal.print(cyan("Enter number (1-${limited.size}): "))

        val input = readlnOrNull()?.toIntOrNull()
        if (input != null && input in 1..limited.size) {
            return limited[input - 1]
        }

        terminal.println(gray("Invalid or empty input, defaulting to first option."))
        return limited.first()
    }
}
