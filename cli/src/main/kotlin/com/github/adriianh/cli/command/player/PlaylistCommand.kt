package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.handler.PlayActionHandler
import com.github.adriianh.cli.command.player.util.ItemPicker
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.interactor.LibraryInteractors
import com.github.adriianh.core.domain.interactor.OfflineInteractors
import com.github.adriianh.core.domain.model.Playlist
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.varabyte.kotter.foundation.text.textLine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File
import com.varabyte.kotter.foundation.text.yellow as kotterYellow

class PlaylistCommand : CliktCommand(name = "playlist") {
    override fun help(context: Context): String = "Manage local playlists"

    init {
        subcommands(
            PlaylistCreateCommand(),
            PlaylistListCommand(),
            PlaylistAddCommand(),
            PlaylistPlayCommand(),
            PlaylistExportCommand()
        )
    }

    override fun run() {}
}

abstract class LibraryCommand(name: String) : CliktCommand(name = name), KoinComponent {
    protected val terminal = Terminal()

    protected fun <T> selectInteractive(
        results: List<T>,
        title: String,
        titleSelector: (T) -> String
    ): T? {
        if (results.isEmpty()) return null
        if (results.size == 1) return results.first()

        val limited = results.take(15)
        val selected = ItemPicker.pickItem(limited, title) { _, item, isSelected ->
            if (isSelected) {
                kotterYellow { textLine("> " + titleSelector(item)) }
            } else {
                textLine("  " + titleSelector(item))
            }
        }

        if (selected == null) terminal.println(gray("Selection cancelled."))
        return selected
    }

    protected fun runWithKoin(block: suspend () -> Unit) {
        if (GlobalContext.getOrNull() == null) {
            startKoin { modules(appModule) }
        }
        try {
            runBlocking { block() }
        } finally {
            stopKoin()
        }
    }

    protected fun findPlaylist(playlists: List<Playlist>, nameOrId: String): Playlist? {
        val id = nameOrId.toLongOrNull()
        if (id != null) {
            playlists.find { it.id == id }?.let { return it }
        }

        val matches = playlists.filter { it.name.contains(nameOrId, ignoreCase = true) }
        if (matches.isEmpty()) {
            terminal.println(yellow("Playlist '$nameOrId' not found."))
            return null
        }

        return selectInteractive(
            matches,
            "Select Playlist"
        ) { "${it.name} (${it.trackCount} tracks)" }
    }
}

class PlaylistCreateCommand : LibraryCommand("create") {
    private val name by argument(help = "Playlist name")
    override fun help(context: Context): String = "Create a new local playlist"

    override fun run() = runWithKoin {
        val library: LibraryInteractors by inject()
        val id = library.createPlaylist(name)
        terminal.println(green("Playlist '$name' created with ID: $id"))
    }
}

class PlaylistListCommand : LibraryCommand("list") {
    override fun help(context: Context): String = "List all local playlists"

    override fun run() = runWithKoin {
        val library: LibraryInteractors by inject()
        val playlists = library.getPlaylists().first()
        if (playlists.isEmpty()) {
            terminal.println(yellow("No playlists found."))
            return@runWithKoin
        }

        terminal.println(cyan("Local Playlists:"))
        playlists.forEach { p ->
            terminal.println("${gray("#${p.id}")} ${p.name} (${p.trackCount} tracks)")
        }
    }
}

class PlaylistAddCommand : LibraryCommand("add") {
    private val playlistName by argument(help = "Playlist name or ID")
    private val query by argument(help = "Track search query")
    private val interactive by option(
        "-i",
        "--interactive",
        help = "Interactively select track"
    ).flag(default = false)

    override fun help(context: Context): String = "Add a track to a playlist"

    override fun run() = runWithKoin {
        val library: LibraryInteractors by inject()
        val searchTracks: SearchTracksUseCase by inject()

        val playlists = library.getPlaylists().first()
        val playlist = findPlaylist(playlists, playlistName) ?: return@runWithKoin

        terminal.println(gray("Searching for '$query'..."))
        val tracks = searchTracks(query)
        val track = if (interactive) {
            selectInteractive(tracks, "Select Track") { "${it.title} by ${it.artist}" }
        } else {
            tracks.firstOrNull()
        }

        if (track == null) {
            terminal.println(yellow("No track found for '$query'."))
            return@runWithKoin
        }

        library.addTrackToPlaylist(playlist.id, track)
        terminal.println(green("Added '${track.title}' to playlist '${playlist.name}'"))
    }
}

class PlaylistPlayCommand : LibraryCommand("play") {
    private val playlistName by argument(help = "Playlist name or ID")

    override fun help(context: Context): String = "Play a local playlist"

    override fun run() = runWithKoin {
        val library: LibraryInteractors by inject()
        val getStream: GetStreamUseCase by inject()

        val playlists = library.getPlaylists().first()
        val playlist = findPlaylist(playlists, playlistName) ?: return@runWithKoin

        val tracks = library.getPlaylistTracks(playlist.id).first()
        if (tracks.isEmpty()) {
            terminal.println(yellow("Playlist '${playlist.name}' is empty."))
            return@runWithKoin
        }

        PlayActionHandler.playMultiple(
            contextName = playlist.name,
            tracks = tracks,
            getStream = getStream,
            terminal = terminal
        )
    }
}

class PlaylistExportCommand : LibraryCommand("export") {
    private val playlistName by argument(help = "Playlist name or ID")

    override fun help(context: Context): String = "Export a playlist to .m3u"

    override fun run() = runWithKoin {
        val library: LibraryInteractors by inject()
        val offline: OfflineInteractors by inject()
        val playlists = library.getPlaylists().first()
        val playlist = findPlaylist(playlists, playlistName) ?: return@runWithKoin

        val tracks = library.getPlaylistTracks(playlist.id).first()
        if (tracks.isEmpty()) {
            terminal.println(yellow("Playlist '${playlist.name}' is empty."))
            return@runWithKoin
        }

        val fileName = "${playlist.name.replace(" ", "_")}.m3u"
        val file = File(fileName)
        file.writeText("#EXTM3U\n")

        val offlineTracksSnapshot = offline.getOfflineTracks.getSnapshot()

        tracks.forEach { t ->
            val offlineTrack = offlineTracksSnapshot.find { it.track.id == t.id }
            val path = offlineTrack?.localFilePath ?: "${t.title}.mp3"
            file.appendText("#EXTINF:${t.durationMs / 1000},${t.artist} - ${t.title}\n")
            file.appendText("$path\n")
        }

        terminal.println(green("Playlist exported to ${file.absolutePath}"))
    }
}
