package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.usecase.search.GetEntityDetailsUseCase
import com.github.adriianh.core.domain.usecase.search.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.search.SearchAlbumsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchArtistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchPlaylistsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
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
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.cyan
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.white
import com.varabyte.kotter.foundation.text.yellow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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

    private val prettyJson = Json { prettyPrint = true }

    override fun help(context: Context): String = Messages.get("help.search_command")

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

            runBlocking {
                when (type.lowercase()) {
                    "track" -> {
                        val results = searchTracks(query).take(limit)
                        if (results.isEmpty()) {
                            echo(Messages.get("search.no_results", "query" to query))
                            return@runBlocking
                        }
                        when (format) {
                            "json" -> outputJsonTracks(results, getSimilarTracks, getLyrics)
                            else -> outputPlainTracks(results, getSimilarTracks, getLyrics)
                        }
                    }

                    "album" -> {
                        val results = searchAlbums(query).take(limit)
                        if (results.isEmpty()) {
                            echo(Messages.get("search.no_results", "query" to query))
                            return@runBlocking
                        }
                        outputPlainAlbums(results, getEntityDetails)
                    }

                    "artist" -> {
                        val results = searchArtists(query).take(limit)
                        if (results.isEmpty()) {
                            echo(Messages.get("search.no_results", "query" to query))
                            return@runBlocking
                        }
                        outputPlainArtists(results, getEntityDetails)
                    }

                    "playlist" -> {
                        val results = searchPlaylists(query).take(limit)
                        if (results.isEmpty()) {
                            echo(Messages.get("search.no_results", "query" to query))
                            return@runBlocking
                        }
                        outputPlainPlaylists(results, getEntityDetails)
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
    ) {
        var selectedTrack: Track? = null

        session {
            var selectedIndex by liveVarOf(0)
            var accepted by liveVarOf(false)
            val maxVisible = 15

            section {
                cyan {
                    textLine(
                        Messages.get(
                            "search.results_header",
                            "query" to query,
                            "count" to tracks.size.toString()
                        )
                    )
                }
                textLine()

                val windowStart =
                    maxOf(0, minOf(selectedIndex - maxVisible / 2, tracks.size - maxVisible))
                val visibleTracks = tracks.drop(windowStart).take(maxVisible)

                if (windowStart > 0) {
                    white { textLine("  ↑ ... ") }
                }

                visibleTracks.forEachIndexed { index, track ->
                    val actualIndex = windowStart + index
                    val isSelected = actualIndex == selectedIndex
                    val duration = formatDuration(track.durationMs)
                    val genres =
                        if (track.genres.isNotEmpty()) " [${track.genres.joinToString(", ")}]" else ""

                    if (isSelected) {
                        text("> ")
                        yellow { text("${actualIndex + 1}. ${track.title} — ${track.artist}") }
                        textLine(" (${track.album}) $duration$genres")
                    } else {
                        text("  ")
                        textLine("${actualIndex + 1}. ${track.title} — ${track.artist} (${track.album}) $duration$genres")
                    }
                }

                if (windowStart + maxVisible < tracks.size) {
                    white { textLine("  ↓ ... ") }
                }

                textLine()
                textLine("Use UP/DOWN to scroll & pick, ENTER to select, ESC to exit")
            }.runUntilSignal {
                onKeyPressed {
                    when (key) {
                        Keys.UP -> selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        Keys.DOWN -> selectedIndex =
                            (selectedIndex + 1).coerceAtMost(tracks.size - 1)

                        Keys.ENTER -> {
                            accepted = true
                            signal()
                        }

                        Keys.ESC -> signal()
                    }
                }
            }

            if (accepted) {
                selectedTrack = tracks[selectedIndex]
                section {
                    textLine()
                    cyan { textLine("You selected: ${selectedTrack.title} — ${selectedTrack.artist}") }
                    textLine("Hint: Next we will implement Play/Download here!")
                }.run()
            }
        }

        if (selectedTrack == null) return

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
        getEntityDetails: GetEntityDetailsUseCase
    ) {
        var selectedAlbum: SearchResult.Album? = null

        session {
            var selectedIndex by liveVarOf(0)
            var accepted by liveVarOf(false)
            val maxVisible = 15

            section {
                cyan { textLine("Search Results for '$query' ($type): ${albums.size}") }
                textLine()

                val windowStart =
                    maxOf(0, minOf(selectedIndex - maxVisible / 2, albums.size - maxVisible))
                val visibleAlbums = albums.drop(windowStart).take(maxVisible)

                if (windowStart > 0) {
                    white { textLine("  ↑ ... ") }
                }

                visibleAlbums.forEachIndexed { index, album ->
                    val actualIndex = windowStart + index
                    val isSelected = actualIndex == selectedIndex
                    val yearStr = if (album.year != null) " [${album.year}]" else ""

                    if (isSelected) {
                        text("> ")
                        yellow { text("${actualIndex + 1}. ${album.title} — ${album.author}") }
                        textLine(yearStr)
                    } else {
                        text("  ")
                        textLine("${actualIndex + 1}. ${album.title} — ${album.author}$yearStr")
                    }
                }

                if (windowStart + maxVisible < albums.size) {
                    white { textLine("  ↓ ... ") }
                }

                textLine()
                textLine("Use UP/DOWN to scroll & pick, ENTER to select, ESC to exit")
            }.runUntilSignal {
                onKeyPressed {
                    when (key) {
                        Keys.UP -> selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        Keys.DOWN -> selectedIndex =
                            (selectedIndex + 1).coerceAtMost(albums.size - 1)

                        Keys.ENTER -> {
                            accepted = true
                            signal()
                        }

                        Keys.ESC -> signal()
                    }
                }
            }

            if (accepted) {
                selectedAlbum = albums[selectedIndex]
                section {
                    textLine()
                    cyan { textLine("Loading tracks for ${selectedAlbum.title}...") }
                }.run()
            }
        }

        if (selectedAlbum != null) {
            val detailedAlbum = getEntityDetails(selectedAlbum) as? SearchResult.Album
            val songs = detailedAlbum?.songs ?: emptyList()
            showTrackListPicker(songs, "Tracks in ${detailedAlbum?.title}")
        }
    }

    private suspend fun outputPlainArtists(
        artists: List<SearchResult.Artist>,
        getEntityDetails: GetEntityDetailsUseCase
    ) {
        var selectedArtist: SearchResult.Artist? = null

        session {
            var selectedIndex by liveVarOf(0)
            var accepted by liveVarOf(false)
            val maxVisible = 15

            section {
                cyan { textLine("Search Results for '$query' ($type): ${artists.size}") }
                textLine()

                val windowStart =
                    maxOf(0, minOf(selectedIndex - maxVisible / 2, artists.size - maxVisible))
                val visibleArtists = artists.drop(windowStart).take(maxVisible)

                if (windowStart > 0) {
                    white { textLine("  ↑ ... ") }
                }

                visibleArtists.forEachIndexed { index, artist ->
                    val actualIndex = windowStart + index
                    val isSelected = actualIndex == selectedIndex
                    val subscribers = artist.subscriberCountText?.let { " ($it)" } ?: ""

                    if (isSelected) {
                        text("> ")
                        yellow { text("${actualIndex + 1}. ${artist.name}") }
                        textLine(subscribers)
                    } else {
                        text("  ")
                        textLine("${actualIndex + 1}. ${artist.name}$subscribers")
                    }
                }

                if (windowStart + maxVisible < artists.size) {
                    white { textLine("  ↓ ... ") }
                }

                textLine()
                textLine("Use UP/DOWN to scroll & pick, ENTER to select, ESC to exit")
            }.runUntilSignal {
                onKeyPressed {
                    when (key) {
                        Keys.UP -> selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        Keys.DOWN -> selectedIndex =
                            (selectedIndex + 1).coerceAtMost(artists.size - 1)

                        Keys.ENTER -> {
                            accepted = true
                            signal()
                        }

                        Keys.ESC -> signal()
                    }
                }
            }

            if (accepted) {
                selectedArtist = artists[selectedIndex]
                section {
                    textLine()
                    cyan { textLine("Loading details for ${selectedArtist.name}...") }
                }.run()
            }
        }

        if (selectedArtist != null) {
            val detailedArtist = getEntityDetails(selectedArtist) as? SearchResult.Artist
            val allItems = detailedArtist?.sections?.flatMap { it.items } ?: emptyList()

            if (allItems.isEmpty()) {
                echo("No extra information found for ${detailedArtist?.name}")
                return
            }

            showEntityListPicker(allItems, "Details for ${detailedArtist?.name}", getEntityDetails)
        }
    }

    private suspend fun showEntityListPicker(
        items: List<SearchResult>,
        title: String,
        getEntityDetails: GetEntityDetailsUseCase
    ) {
        var selectedItem: SearchResult? = null

        session {
            var selectedIndex by liveVarOf(0)
            var accepted by liveVarOf(false)
            val maxVisible = 15

            section {
                cyan { textLine("== $title ==") }
                textLine()

                val windowStart =
                    maxOf(0, minOf(selectedIndex - maxVisible / 2, items.size - maxVisible))
                val visibleItems = items.drop(windowStart).take(maxVisible)

                if (windowStart > 0) {
                    white { textLine("  ↑ ... ") }
                }

                visibleItems.forEachIndexed { index, item ->
                    val actualIndex = windowStart + index
                    val isSelected = actualIndex == selectedIndex

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
                        text("> ")
                        yellow { text("$typeStr $nameStr") }
                        textLine()
                    } else {
                        text("  ")
                        textLine("$typeStr $nameStr")
                    }
                }

                if (windowStart + maxVisible < items.size) {
                    white { textLine("  ↓ ... ") }
                }

                textLine()
                textLine("Use UP/DOWN to scroll & pick, ENTER to select, ESC to exit")
            }.runUntilSignal {
                onKeyPressed {
                    when (key) {
                        Keys.UP -> selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        Keys.DOWN -> selectedIndex =
                            (selectedIndex + 1).coerceAtMost(items.size - 1)

                        Keys.ENTER -> {
                            accepted = true
                            signal()
                        }

                        Keys.ESC -> signal()
                    }
                }
            }

            if (accepted) {
                selectedItem = items[selectedIndex]
                section {
                    textLine()
                    cyan { textLine("Loading selection...") }
                }.run()
            }
        }

        when (val item = selectedItem) {
            is SearchResult.Song -> {
                echo(magenta("You selected Song: ${item.track.title}"))
                echo("Hint: Next we will implement Play/Download here!")
            }

            is SearchResult.Album -> {
                val detailedAlbum = getEntityDetails(item) as? SearchResult.Album
                val songs = detailedAlbum?.songs ?: emptyList()
                showTrackListPicker(songs, "Tracks in ${detailedAlbum?.title}")
            }

            is SearchResult.Playlist -> {
                val detailedPlaylist = getEntityDetails(item) as? SearchResult.Playlist
                val songs = detailedPlaylist?.songs ?: emptyList()
                showTrackListPicker(songs, "Tracks in ${detailedPlaylist?.title}")
            }

            is SearchResult.Artist -> {
                val detailedArtist = getEntityDetails(item) as? SearchResult.Artist
                val allItems = detailedArtist?.sections?.flatMap { it.items } ?: emptyList()
                if (allItems.isNotEmpty()) {
                    showEntityListPicker(
                        allItems,
                        "Details for ${detailedArtist?.name}",
                        getEntityDetails
                    )
                } else {
                    echo("No details found for artist ${detailedArtist?.name}")
                }
            }

            else -> {}
        }
    }

    private suspend fun outputPlainPlaylists(
        playlists: List<SearchResult.Playlist>,
        getEntityDetails: GetEntityDetailsUseCase
    ) {
        var selectedPlaylist: SearchResult.Playlist? = null

        session {
            var selectedIndex by liveVarOf(0)
            var accepted by liveVarOf(false)
            val maxVisible = 15

            section {
                cyan { textLine("Search Results for '$query' ($type): ${playlists.size}") }
                textLine()

                val windowStart =
                    maxOf(0, minOf(selectedIndex - maxVisible / 2, playlists.size - maxVisible))
                val visiblePlaylists = playlists.drop(windowStart).take(maxVisible)

                if (windowStart > 0) {
                    white { textLine("  ↑ ... ") }
                }

                visiblePlaylists.forEachIndexed { index, playlist ->
                    val actualIndex = windowStart + index
                    val isSelected = actualIndex == selectedIndex
                    val tCount = playlist.trackCount?.let { " [$it tracks]" } ?: ""

                    if (isSelected) {
                        text("> ")
                        yellow { text("${actualIndex + 1}. ${playlist.title} — ${playlist.author}") }
                        textLine(tCount)
                    } else {
                        text("  ")
                        textLine("${actualIndex + 1}. ${playlist.title} — ${playlist.author}$tCount")
                    }
                }

                if (windowStart + maxVisible < playlists.size) {
                    white { textLine("  ↓ ... ") }
                }

                textLine()
                textLine("Use UP/DOWN to scroll & pick, ENTER to select, ESC to exit")
            }.runUntilSignal {
                onKeyPressed {
                    when (key) {
                        Keys.UP -> selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        Keys.DOWN -> selectedIndex =
                            (selectedIndex + 1).coerceAtMost(playlists.size - 1)

                        Keys.ENTER -> {
                            accepted = true
                            signal()
                        }

                        Keys.ESC -> signal()
                    }
                }
            }

            if (accepted) {
                selectedPlaylist = playlists[selectedIndex]
                section {
                    textLine()
                    cyan { textLine("Loading tracks for ${selectedPlaylist.title}...") }
                }.run()
            }
        }

        if (selectedPlaylist != null) {
            val detailedPlaylist = getEntityDetails(selectedPlaylist) as? SearchResult.Playlist
            val songs = detailedPlaylist?.songs ?: emptyList()
            showTrackListPicker(songs, "Tracks in ${detailedPlaylist?.title}")
        }
    }

    private fun showTrackListPicker(tracks: List<Track>, title: String) {
        if (tracks.isEmpty()) {
            echo("No tracks found.")
            return
        }

        var selectedTrack: Track?

        session {
            var selectedIndex by liveVarOf(0)
            var accepted by liveVarOf(false)
            val maxVisible = 15

            section {
                cyan { textLine("== $title ==") }
                textLine()

                val windowStart =
                    maxOf(0, minOf(selectedIndex - maxVisible / 2, tracks.size - maxVisible))
                val visibleTracks = tracks.drop(windowStart).take(maxVisible)

                if (windowStart > 0) {
                    white { textLine("  ↑ ... ") }
                }

                visibleTracks.forEachIndexed { index, track ->
                    val actualIndex = windowStart + index
                    val isSelected = actualIndex == selectedIndex
                    val duration = formatDuration(track.durationMs)

                    if (isSelected) {
                        text("> ")
                        yellow { text("${actualIndex + 1}. ${track.title}") }
                        textLine(" $duration")
                    } else {
                        text("  ")
                        textLine("${actualIndex + 1}. ${track.title} $duration")
                    }
                }

                if (windowStart + maxVisible < tracks.size) {
                    white { textLine("  ↓ ... ") }
                }

                textLine()
                textLine("Use UP/DOWN to scroll & pick, ENTER to select, ESC to exit")
            }.runUntilSignal {
                onKeyPressed {
                    when (key) {
                        Keys.UP -> selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        Keys.DOWN -> selectedIndex =
                            (selectedIndex + 1).coerceAtMost(tracks.size - 1)

                        Keys.ENTER -> {
                            accepted = true
                            signal()
                        }

                        Keys.ESC -> signal()
                    }
                }
            }

            if (accepted) {
                selectedTrack = tracks[selectedIndex]
                section {
                    textLine()
                    cyan { textLine("You selected: ${selectedTrack.title} — ${selectedTrack.artist}") }
                    textLine("Hint: Next we will implement Play/Download here!")
                }.run()
            }
        }
    }

    private suspend fun outputJsonTracks(
        tracks: List<Track>,
        getSimilarTracks: GetSimilarTracksUseCase,
        getLyrics: GetLyricsUseCase,
    ) {
        val first = tracks.first()

        val similarArray = if (similar) {
            val similarTracks = getSimilarTracks(first.artist, first.title)
            buildJsonArray {
                similarTracks.take(5).forEach { s ->
                    add(buildJsonObject {
                        put("title", s.title)
                        put("artist", s.artist)
                        put("match", s.match)
                    })
                }
            }
        } else null

        val lyricsText = if (lyrics) getLyrics(first.artist, first.title) else null

        val root = buildJsonObject {
            put("query", query)
            put("count", tracks.size)
            put("results", buildJsonArray {
                tracks.forEach { track ->
                    add(buildJsonObject {
                        put("id", track.id)
                        put("title", track.title)
                        put("artist", track.artist)
                        put("album", track.album)
                        put("durationMs", track.durationMs)
                        put("genres", track.genres.joinToString(", "))
                        put("artworkUrl", track.artworkUrl ?: "")
                    })
                }
            })
            if (similarArray != null) put("similar", similarArray)
            if (lyricsText != null) put("lyrics", lyricsText)
        }

        echo(prettyJson.encodeToString(root))
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}

