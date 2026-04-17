package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
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
                        outputPlainAlbums(results)
                    }

                    "artist" -> {
                        val results = searchArtists(query).take(limit)
                        if (results.isEmpty()) {
                            echo(Messages.get("search.no_results", "query" to query))
                            return@runBlocking
                        }
                        outputPlainArtists(results)
                    }

                    "playlist" -> {
                        val results = searchPlaylists(query).take(limit)
                        if (results.isEmpty()) {
                            echo(Messages.get("search.no_results", "query" to query))
                            return@runBlocking
                        }
                        outputPlainPlaylists(results)
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

                tracks.forEachIndexed { index, track ->
                    val isSelected = index == selectedIndex
                    val duration = formatDuration(track.durationMs)
                    val genres =
                        if (track.genres.isNotEmpty()) " [${track.genres.joinToString(", ")}]" else ""

                    if (isSelected) {
                        text("> ")
                        yellow { text("${index + 1}. ${track.title} — ${track.artist}") }
                        textLine(" (${track.album}) $duration$genres")
                    } else {
                        text("  ")
                        textLine("${index + 1}. ${track.title} — ${track.artist} (${track.album}) $duration$genres")
                    }
                }
                textLine()
                textLine("Use UP/DOWN to pick, ENTER to select, ESC to exit")
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

    private fun outputPlainAlbums(albums: List<SearchResult.Album>) {
        var selectedAlbum: SearchResult.Album?

        session {
            var selectedIndex by liveVarOf(0)
            var accepted by liveVarOf(false)

            section {
                cyan { textLine("Search Results for '$query' ($type): ${albums.size}") }
                textLine()

                albums.forEachIndexed { index, album ->
                    val isSelected = index == selectedIndex
                    val yearStr = if (album.year != null) " [${album.year}]" else ""

                    if (isSelected) {
                        text("> ")
                        yellow { text("${index + 1}. ${album.title} — ${album.author}") }
                        textLine(yearStr)
                    } else {
                        text("  ")
                        textLine("${index + 1}. ${album.title} — ${album.author}$yearStr")
                    }
                }
                textLine()
                textLine("Use UP/DOWN to pick, ENTER to select, ESC to exit")
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
                    cyan { textLine("You selected Album: ${selectedAlbum.title} — ${selectedAlbum.author}") }
                }.run()
            }
        }
    }

    private fun outputPlainArtists(artists: List<SearchResult.Artist>) {
        var selectedArtist: SearchResult.Artist?

        session {
            var selectedIndex by liveVarOf(0)
            var accepted by liveVarOf(false)

            section {
                cyan { textLine("Search Results for '$query' ($type): ${artists.size}") }
                textLine()

                artists.forEachIndexed { index, artist ->
                    val isSelected = index == selectedIndex
                    val subscribers = artist.subscriberCountText?.let { " ($it)" } ?: ""

                    if (isSelected) {
                        text("> ")
                        yellow { text("${index + 1}. ${artist.name}") }
                        textLine(subscribers)
                    } else {
                        text("  ")
                        textLine("${index + 1}. ${artist.name}$subscribers")
                    }
                }
                textLine()
                textLine("Use UP/DOWN to pick, ENTER to select, ESC to exit")
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
                    cyan { textLine("You selected Artist: ${selectedArtist.name}") }
                }.run()
            }
        }
    }

    private fun outputPlainPlaylists(playlists: List<SearchResult.Playlist>) {
        var selectedPlaylist: SearchResult.Playlist?

        session {
            var selectedIndex by liveVarOf(0)
            var accepted by liveVarOf(false)

            section {
                cyan { textLine("Search Results for '$query' ($type): ${playlists.size}") }
                textLine()

                playlists.forEachIndexed { index, playlist ->
                    val isSelected = index == selectedIndex
                    val tCount = playlist.trackCount?.let { " [$it tracks]" } ?: ""

                    if (isSelected) {
                        text("> ")
                        yellow { text("${index + 1}. ${playlist.title} — ${playlist.author}") }
                        textLine(tCount)
                    } else {
                        text("  ")
                        textLine("${index + 1}. ${playlist.title} — ${playlist.author}$tCount")
                    }
                }
                textLine()
                textLine("Use UP/DOWN to pick, ENTER to select, ESC to exit")
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
                    cyan { textLine("You selected Playlist: ${selectedPlaylist.title} — ${selectedPlaylist.author}") }
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