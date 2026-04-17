package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.offline.DownloadTrackUseCase
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.search.GetEntityDetailsUseCase
import com.github.adriianh.core.domain.usecase.search.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.search.SearchAlbumsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchArtistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.magenta
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.system.exitProcess

class SearchCommand : CliktCommand(
    name = "search"
), KoinComponent {
    val query by argument(name = "query", help = "Track name to search for")
    val format by option(
        names = arrayOf("-f", "--format"),
        help = "Output format: json or plain (default: plain)"
    )
        .choice("json", "plain", ignoreCase = true)
        .default("plain")
    val type by option(
        names = arrayOf("-t", "--type"),
        help = "Type of resource to search: track, album, artist, playlist (default: track)"
    )
        .choice("track", "album", "artist", "playlist", ignoreCase = true)
        .default("track")
    val limit by option(
        names = arrayOf("-l", "--limit"),
        help = "Maximum number of results to show (default: 10)"
    )
        .int()
        .default(10)
    val similar by option("-s", "--similar", help = "Show similar tracks for the first result")
        .flag(default = false)
    val lyrics by option("--lyrics", help = "Show lyrics for the first result")
        .flag(default = false)

    override fun help(context: Context): String = Messages.get("help.search_command")
    private val terminal = Terminal()
    override fun run() {
        if (resolveEnv("LASTFM_API_KEY") == null) {
            echo(Messages.get("error.missing_lastfm_key", "configDir" to configDir), err = true)
            exitProcess(1)
        }
        startKoin { modules(appModule) }
        try {
            val searchTracks: SearchTracksUseCase by inject()
            val searchAlbums: SearchAlbumsUseCase by inject()
            val searchArtists: SearchArtistsUseCase by inject()
            val searchPlaylists: SearchPlaylistsUseCase by inject()
            val getEntityDetails: GetEntityDetailsUseCase by inject()
            val getSimilarTracks: GetSimilarTracksUseCase by inject()
            val getLyrics: GetLyricsUseCase by inject()
            val getStream: GetStreamUseCase by inject()
            val getSettings: GetSettingsUseCase by inject()
            val downloadTrack: DownloadTrackUseCase by inject()
            runBlocking {
                when (type.lowercase()) {
                    "track" -> {
                        val results = searchTracks(query).take(limit)
                        if (results.isEmpty()) {
                            echo(Messages.get("search.no_results", "query" to query))
                            return@runBlocking
                        }
                        when (format) {
                            "json" -> echo(
                                SearchOutputFormatter.outputJsonTracks(
                                    results,
                                    query,
                                    similar,
                                    lyrics,
                                    getSimilarTracks,
                                    getLyrics
                                )
                            )

                            else -> outputPlainTracks(
                                results,
                                getSimilarTracks,
                                getLyrics,
                                getStream,
                                getSettings,
                                downloadTrack
                            )
                        }
                    }

                    "album" -> {
                        val results = searchAlbums(query).take(limit)
                        if (results.isEmpty()) {
                            echo(Messages.get("search.no_results", "query" to query))
                            return@runBlocking
                        }
                        outputPlainAlbums(
                            results,
                            getEntityDetails,
                            getStream,
                            getSettings,
                            downloadTrack
                        )
                    }

                    "artist" -> {
                        val results = searchArtists(query).take(limit)
                        if (results.isEmpty()) {
                            echo(Messages.get("search.no_results", "query" to query))
                            return@runBlocking
                        }
                        outputPlainArtists(
                            results,
                            getEntityDetails,
                            getStream,
                            getSettings,
                            downloadTrack
                        )
                    }

                    "playlist" -> {
                        val results = searchPlaylists(query).take(limit)
                        if (results.isEmpty()) {
                            echo(Messages.get("search.no_results", "query" to query))
                            return@runBlocking
                        }
                        outputPlainPlaylists(
                            results,
                            getEntityDetails,
                            getStream,
                            getSettings,
                            downloadTrack
                        )
                    }
                }
            }
        } finally {
            stopKoin()
        }
    }

    private suspend fun outputPlainTracks(
        tracks: List<Track>,
        getSimilarTracks: GetSimilarTracksUseCase,
        getLyrics: GetLyricsUseCase,
        getStream: GetStreamUseCase,
        getSettings: GetSettingsUseCase,
        downloadTrack: DownloadTrackUseCase
    ) {
        val title = Messages.get(
            "search.results_header",
            "query" to query,
            "count" to tracks.size.toString()
        )
        val selectedTrack = SearchPickers.pickItem(tracks, title) { _, track, isSelected ->
            val duration = SearchOutputFormatter.formatDuration(track.durationMs)
            val genres =
                if (track.genres.isNotEmpty()) " [${track.genres.joinToString(", ")}]" else ""
            if (isSelected) {
                yellow { textLine("> ${track.title} — ${track.artist} (${track.album}) $duration$genres") }
            } else {
                textLine("  ${track.title} — ${track.artist} (${track.album}) $duration$genres")
            }
        }
        if (selectedTrack == null) return
        SearchActionHandler.handleTrackAction(
            selectedTrack,
            getStream,
            getSettings,
            downloadTrack,
            terminal
        )
        if (similar) {
            echo("")
            echo(
                magenta(
                    Messages.get(
                        "search.similar_header",
                        "title" to selectedTrack.title,
                        "artist" to selectedTrack.artist
                    )
                )
            )
            val similarTracks = getSimilarTracks(selectedTrack.artist, selectedTrack.title)
            if (similarTracks.isEmpty()) {
                echo(gray("  ${Messages.get("search.no_similar")}"))
            } else {
                similarTracks.take(5).forEach { s ->
                    val matchPct = "%.0f%%".format(s.match * 100)
                    echo("  • ${s.title} — ${yellow(s.artist)} ${gray("($matchPct match)")}")
                }
            }
        }
        if (lyrics) {
            echo("")
            echo(
                magenta(
                    Messages.get(
                        "search.lyrics_header",
                        "title" to selectedTrack.title,
                        "artist" to selectedTrack.artist
                    )
                )
            )
            val lyricsText = getLyrics(selectedTrack.artist, selectedTrack.title)
            if (lyricsText.isNullOrBlank()) {
                echo(gray("  ${Messages.get("search.no_lyrics")}"))
            } else {
                echo(lyricsText)
            }
        }
    }

    private suspend fun outputPlainAlbums(
        albums: List<SearchResult.Album>,
        getEntityDetails: GetEntityDetailsUseCase,
        getStream: GetStreamUseCase,
        getSettings: GetSettingsUseCase,
        downloadTrack: DownloadTrackUseCase
    ) {
        val title = "Search Results for '$query' ($type): ${albums.size}"
        val selectedAlbum = SearchPickers.pickItem(albums, title) { _, album, isSelected ->
            val yearStr = if (album.year != null) " [${album.year}]" else ""
            if (isSelected) {
                yellow { textLine("> ${album.title} — ${album.author}$yearStr") }
            } else {
                textLine("  ${album.title} — ${album.author}$yearStr")
            }
        }
        if (selectedAlbum != null) {
            val detailedAlbum = getEntityDetails(selectedAlbum) as? SearchResult.Album
            val songs = detailedAlbum?.songs ?: emptyList()
            showTrackListPicker(
                songs,
                "Tracks in ${detailedAlbum?.title}",
                getStream,
                getSettings,
                downloadTrack
            )
        }
    }

    private suspend fun outputPlainArtists(
        artists: List<SearchResult.Artist>,
        getEntityDetails: GetEntityDetailsUseCase,
        getStream: GetStreamUseCase,
        getSettings: GetSettingsUseCase,
        downloadTrack: DownloadTrackUseCase
    ) {
        val title = "Search Results for '$query' ($type): ${artists.size}"
        val selectedArtist = SearchPickers.pickItem(artists, title) { _, artist, isSelected ->
            val subscribers = artist.subscriberCountText?.let { " ($it)" } ?: ""
            if (isSelected) {
                yellow { textLine("> ${artist.name}$subscribers") }
            } else {
                textLine("  ${artist.name}$subscribers")
            }
        }
        if (selectedArtist != null) {
            val detailedArtist = getEntityDetails(selectedArtist) as? SearchResult.Artist
            val allItems = detailedArtist?.sections?.flatMap { it.items } ?: emptyList()
            if (allItems.isEmpty()) {
                echo("No extra information found for ${detailedArtist?.name}")
                return
            }
            showEntityListPicker(
                allItems,
                "Details for ${detailedArtist?.name}",
                getEntityDetails,
                getStream,
                getSettings,
                downloadTrack
            )
        }
    }

    private suspend fun outputPlainPlaylists(
        playlists: List<SearchResult.Playlist>,
        getEntityDetails: GetEntityDetailsUseCase,
        getStream: GetStreamUseCase,
        getSettings: GetSettingsUseCase,
        downloadTrack: DownloadTrackUseCase
    ) {
        val title = "Search Results for '$query' ($type): ${playlists.size}"
        val selectedPlaylist = SearchPickers.pickItem(playlists, title) { _, playlist, isSelected ->
            val tCount = playlist.trackCount?.let { " [$it tracks]" } ?: ""
            if (isSelected) {
                yellow { textLine("> ${playlist.title} — ${playlist.author}$tCount") }
            } else {
                textLine("  ${playlist.title} — ${playlist.author}$tCount")
            }
        }
        if (selectedPlaylist != null) {
            val detailedPlaylist = getEntityDetails(selectedPlaylist) as? SearchResult.Playlist
            val songs = detailedPlaylist?.songs ?: emptyList()
            showTrackListPicker(
                songs,
                "Tracks in ${detailedPlaylist?.title}",
                getStream,
                getSettings,
                downloadTrack
            )
        }
    }

    private suspend fun showEntityListPicker(
        items: List<SearchResult>,
        title: String,
        getEntityDetails: GetEntityDetailsUseCase,
        getStream: GetStreamUseCase,
        getSettings: GetSettingsUseCase,
        downloadTrack: DownloadTrackUseCase
    ) {
        val selectedItem = SearchPickers.pickItem(items, "== $title ==") { _, item, isSelected ->
            val typeStr = when (item) {
                is SearchResult.Song -> "[Song]"
                is SearchResult.Album -> "[Album]"
                is SearchResult.Playlist -> "[Playlist]"
                is SearchResult.Artist -> "[Artist]"
            }.padEnd(10)
            val nameStr = when (item) {
                is SearchResult.Song -> "${item.track.title} — ${item.track.artist}"
                is SearchResult.Album -> "${item.title} — ${item.author}"
                is SearchResult.Playlist -> "${item.title} — ${item.author}"
                is SearchResult.Artist -> item.name
            }
            if (isSelected) {
                yellow { textLine("> $typeStr $nameStr") }
            } else {
                textLine("  $typeStr $nameStr")
            }
        }
        when (selectedItem) {
            is SearchResult.Song -> {
                echo(magenta("You selected Song: ${selectedItem.track.title}"))
                SearchActionHandler.handleTrackAction(
                    selectedItem.track,
                    getStream,
                    getSettings,
                    downloadTrack,
                    terminal
                )
            }

            is SearchResult.Album -> {
                val detailedAlbum = getEntityDetails(selectedItem) as? SearchResult.Album
                showTrackListPicker(
                    detailedAlbum?.songs ?: emptyList(),
                    "Tracks in ${detailedAlbum?.title}",
                    getStream,
                    getSettings,
                    downloadTrack
                )
            }

            is SearchResult.Playlist -> {
                val detailedPlaylist = getEntityDetails(selectedItem) as? SearchResult.Playlist
                showTrackListPicker(
                    detailedPlaylist?.songs ?: emptyList(),
                    "Tracks in ${detailedPlaylist?.title}",
                    getStream,
                    getSettings,
                    downloadTrack
                )
            }

            is SearchResult.Artist -> {
                val detailedArtist = getEntityDetails(selectedItem) as? SearchResult.Artist
                val allItems = detailedArtist?.sections?.flatMap { it.items } ?: emptyList()
                if (allItems.isNotEmpty()) {
                    showEntityListPicker(
                        allItems,
                        "Details for ${detailedArtist?.name}",
                        getEntityDetails,
                        getStream,
                        getSettings,
                        downloadTrack
                    )
                } else {
                    echo("No details found for artist ${detailedArtist?.name}")
                }
            }

            else -> {}
        }
    }

    private suspend fun showTrackListPicker(
        tracks: List<Track>,
        title: String,
        getStream: GetStreamUseCase,
        getSettings: GetSettingsUseCase,
        downloadTrack: DownloadTrackUseCase
    ) {
        if (tracks.isEmpty()) {
            echo("No tracks found.")
            return
        }
        val selectedTrack = SearchPickers.pickItem(tracks, "== $title ==") { _, track, isSelected ->
            val duration = SearchOutputFormatter.formatDuration(track.durationMs)
            if (isSelected) {
                yellow { textLine("> ${track.title} $duration") }
            } else {
                textLine("  ${track.title} $duration")
            }
        }
        selectedTrack?.let {
            SearchActionHandler.handleTrackAction(
                it,
                getStream,
                getSettings,
                downloadTrack,
                terminal
            )
        }
    }
}