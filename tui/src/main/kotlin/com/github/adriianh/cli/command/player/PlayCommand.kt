package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.PlayActionHandler
import com.github.adriianh.cli.command.player.util.ItemPicker
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.service.DaemonManager
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
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.varabyte.kotter.foundation.text.textLine
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess
import com.varabyte.kotter.foundation.text.yellow as kotterYellow

class PlayCommand :
    CliktCommand(
        name = "play",
    ),
    KoinComponent {
    private val query by argument(name = "query", help = "Name of track, album, or playlist to play")
    private val type by option(
        "-t",
        "--type",
        help = "Playback type: track, album, or playlist (default: track)",
    ).default("track")
    private val interactive by option(
        "-i",
        "--interactive",
        help = "Interactively select from search results",
    ).flag(default = false)
    private val foreground by option(
        "-f",
        "--foreground",
        help = "Run in foreground with progress bar instead of detaching to daemon",
    ).flag(default = false)

    private val terminal = Terminal()

    override fun help(context: Context): String = "Play a track, album, or playlist from streaming or local library."

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo play \"Bohemian Rhapsody\"\n" +
            "  melo play \"Abbey Road\" --type album\n" +
            "  melo play \"Rock Classics\" --type playlist -i\n" +
            "  melo play \"Comfortably Numb\" --foreground"

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
                        val album =
                            selectInteractive(
                                albums,
                                "Select Album",
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
                        dispatchPlayback(detailedAlbum.title, tracks, getStream)
                    }

                    "playlist" -> {
                        val playlists = searchPlaylists(query)
                        val playlist =
                            selectInteractive(
                                playlists,
                                "Select Playlist",
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
                        dispatchPlayback(detailedPlaylist.title, tracks, getStream)
                    }

                    else -> {
                        val tracks = searchTracks(query)
                        val track =
                            selectInteractive(
                                tracks,
                                "Select Track",
                            ) { "${it.title} by ${it.artist}" }

                        if (track == null) {
                            terminal.println("No track results found or selection cancelled for '$query'.")
                            return@runBlocking
                        }

                        dispatchPlayback(track.title, listOf(track), getStream)
                    }
                }
            }
        } finally {
            stopKoin()
            exitProcess(0)
        }
    }

    private suspend fun dispatchPlayback(
        contextName: String,
        tracks: List<Track>,
        getStream: GetStreamUseCase,
    ) {
        if (foreground) {
            playForeground(contextName, tracks, getStream)
            return
        }

        if (DaemonManager.ensureDaemonRunning(terminal)) {
            val success =
                if (tracks.size == 1) {
                    DaemonManager.playTrack(tracks.first())
                } else {
                    DaemonManager.playTracks(tracks)
                }
            if (success) {
                if (tracks.size == 1) {
                    val track = tracks.first()
                    terminal.println(green("▶ Playing: ") + "${track.title} by ${track.artist}")
                    terminal.println(
                        gray("Playing in background. Use 'melo status' to inspect, 'melo pause' to pause."),
                    )
                } else {
                    terminal.println(green("▶ Playing: ") + "$contextName (${tracks.size} tracks)")
                    terminal.println(
                        gray("Playing in background. Use 'melo status' to inspect, 'melo next' to skip."),
                    )
                }
                return
            }
        }

        terminal.println(yellow("Warning: Could not start daemon. Falling back to foreground playback."))
        playForeground(contextName, tracks, getStream)
    }

    private suspend fun playForeground(
        contextName: String,
        tracks: List<Track>,
        getStream: GetStreamUseCase,
    ) {
        if (tracks.size == 1) {
            PlayActionHandler.playTrack(tracks.first(), getStream, terminal)
        } else {
            PlayActionHandler.playMultiple(
                contextName = contextName,
                tracks = tracks,
                getStream = getStream,
                terminal = terminal,
            )
        }
    }

    private fun <T> selectInteractive(
        results: List<T>,
        title: String,
        titleSelector: (T) -> String,
    ): T? {
        if (results.isEmpty()) return null
        if (!interactive || results.size == 1) return results.first()

        val limited = results.take(15)
        val selected =
            ItemPicker.pickItem(limited, title) { _, item, isSelected ->
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
