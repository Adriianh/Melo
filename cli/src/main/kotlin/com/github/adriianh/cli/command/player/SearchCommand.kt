package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.config.Messages
import com.github.adriianh.cli.config.configDir
import com.github.adriianh.cli.config.resolveEnv
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.usecase.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.GetSimilarTracksUseCase
import com.github.adriianh.core.domain.usecase.SearchTracksUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
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

    val format by option(names = arrayOf("-f", "--format"), help = "Output format: json or plain (default: plain)")
        .choice("json", "plain", ignoreCase = true)
        .default("plain")

    val limit by option(names = arrayOf("-l", "--limit"), help = "Maximum number of results to show (default: 10)")
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
            val getSimilarTracks: GetSimilarTracksUseCase by inject()
            val getLyrics: GetLyricsUseCase by inject()

            runBlocking {
                val results = searchTracks(query).take(limit)

                if (results.isEmpty()) {
                    echo(Messages.get("search.no_results", "query" to query))
                    return@runBlocking
                }

                when (format) {
                    "json" -> outputJson(results, getSimilarTracks, getLyrics)
                    else   -> outputPlain(results, getSimilarTracks, getLyrics)
                }
            }
        } finally {
            stopKoin()
        }
    }

    private suspend fun outputPlain(
        tracks: List<Track>,
        getSimilarTracks: GetSimilarTracksUseCase,
        getLyrics: GetLyricsUseCase,
    ) {
        echo(Messages.get("search.results_header", "query" to query, "count" to tracks.size.toString()))
        echo("")

        tracks.forEachIndexed { index, track ->
            val duration = formatDuration(track.durationMs)
            val genres = if (track.genres.isNotEmpty()) " [${track.genres.joinToString(", ")}]" else ""
            echo("${index + 1}. ${track.title}")
            echo("   Artist : ${track.artist}")
            echo("   Album  : ${track.album}")
            echo("   Duration: $duration$genres")
        }

        val first = tracks.first()

        if (similar) {
            echo("")
            echo(Messages.get("search.similar_header", "title" to first.title, "artist" to first.artist))
            val similarTracks = getSimilarTracks(first.artist, first.title)
            if (similarTracks.isEmpty()) {
                echo("  ${Messages.get("search.no_similar")}")
            } else {
                similarTracks.take(5).forEach { s ->
                    val matchPct = "%.0f%%".format(s.match * 100)
                    echo("  • ${s.title} — ${s.artist} ($matchPct match)")
                }
            }
        }

        if (lyrics) {
            echo("")
            echo(Messages.get("search.lyrics_header", "title" to first.title, "artist" to first.artist))
            val lyricsText = getLyrics(first.artist, first.title)
            if (lyricsText.isNullOrBlank()) {
                echo("  ${Messages.get("search.no_lyrics")}")
            } else {
                echo(lyricsText)
            }
        }
    }

    private suspend fun outputJson(
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