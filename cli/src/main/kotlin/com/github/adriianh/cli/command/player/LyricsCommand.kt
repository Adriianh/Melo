package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.util.VerticalProgressBarMaker
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.usecase.search.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.GetSyncedLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.SearchTracksUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.animation.progress.animateOnThread
import com.github.ajalt.mordant.animation.progress.execute
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.InputStreamReader

class LyricsCommand : CliktCommand(
    name = "lyrics"
), KoinComponent {
    private val query by argument(
        name = "query",
        help = "Track name to search lyrics for"
    ).optional()
    private val current by option(
        "-c", "--current",
        help = "Fetch lyrics for the track currently playing in Melo via MPRIS"
    ).flag(default = false)

    private val synced by option(
        "-s", "--synced",
        help = "Fetch synchronized lyrics (LRC format)"
    ).flag(default = false)

    private val terminal = Terminal()

    override fun help(context: Context): String =
        "Fetch and display lyrics for a given track or the currently playing track via MPRIS"

    override fun run() {
        startKoin { modules(appModule) }

        try {
            val getLyrics: GetLyricsUseCase by inject()
            val getSyncedLyrics: GetSyncedLyricsUseCase by inject()
            val searchTracks: SearchTracksUseCase by inject()
            runBlocking {
                if (current) {
                    while (true) {
                        val process = ProcessBuilder(
                            "sh", "-c",
                            "playerctl -p $(playerctl -l | grep '^melo' | head -n 1) metadata --format \"{{title}}|||{{artist}}|||{{mpris:length}}\""
                        )
                            .redirectErrorStream(true)
                            .start()

                        val output = InputStreamReader(process.inputStream).readText().trim()
                        process.waitFor()

                        if (process.exitValue() != 0 || output.isBlank() || output == "No players found") {
                            terminal.println(gray("No Melo player is currently running."))
                            return@runBlocking
                        }

                        val parts = output.split("|||")
                        val title = parts.getOrNull(0)?.trim() ?: "Unknown"
                        val artist = parts.getOrNull(1)?.trim() ?: "Unknown"
                        val lengthRaw = parts.getOrNull(2)?.trim() ?: "0"
                        val lengthUs = lengthRaw.toLongOrNull() ?: 0L

                        terminal.println(cyan("Fetching lyrics for currently playing: $title by $artist..."))

                        val lyricsText =
                            if (synced) getSyncedLyrics(artist, title) else getLyrics(artist, title)
                        if (!lyricsText.isNullOrBlank()) {
                            val lrcLines = mutableListOf<Pair<Long, String>>()
                            if (synced && lyricsText.contains(Regex("""\[\d{2}:\d{2}\.\d{2,3}]"""))) {
                                val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
                                lyricsText.lines().forEach { line ->
                                    val match = regex.find(line.trim())
                                    if (match != null) {
                                        val m = match.groupValues[1].toLong()
                                        val s = match.groupValues[2].toLong()
                                        val msStr = match.groupValues[3]
                                        val ms =
                                            if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                                        val timeUs = (m * 60000000L) + (s * 1000000L) + (ms * 1000L)
                                        lrcLines.add(timeUs to match.groupValues[4].trim())
                                    }
                                }
                            }

                            if (lrcLines.isNotEmpty() && lengthUs > 0) {
                                var currentLyric = "..."
                                val activeProgressTask = progressBarLayout {
                                    text(cyan("♪")); text {
                                    cyan(currentLyric)
                                }
                                    text("Time"); text {
                                    val currentSec = completed / 1000000L
                                    val tlSec = (total ?: 0L) / 1000000L
                                    val currentStr =
                                        String.format("%02d:%02d", currentSec / 60, currentSec % 60)
                                    val tlStr = String.format("%02d:%02d", tlSec / 60, tlSec % 60)
                                    gray("$currentStr / $tlStr")
                                }
                                    text("Progress"); progressBar()
                                }.animateOnThread(
                                    terminal,
                                    total = lengthUs,
                                    maker = VerticalProgressBarMaker
                                )

                                val job = CoroutineScope(Dispatchers.IO).launch {
                                    activeProgressTask.execute()
                                }

                                while (true) {
                                    try {
                                        val curTrackProcess = ProcessBuilder(
                                            "sh", "-c",
                                            "playerctl -p $(playerctl -l | grep '^melo' | head -n 1) metadata --format \"{{title}}|||{{artist}}\""
                                        ).start()
                                        val curTrackStr =
                                            InputStreamReader(curTrackProcess.inputStream).readText()
                                                .trim()
                                        curTrackProcess.waitFor()

                                        if (curTrackStr != "$title|||$artist" && curTrackStr.isNotBlank() && curTrackStr != "No players found") {
                                            break
                                        }

                                        val stateProcess = ProcessBuilder(
                                            "sh", "-c",
                                            "playerctl -p $(playerctl -l | grep '^melo' | head -n 1) status"
                                        ).start()
                                        val stateStr =
                                            InputStreamReader(stateProcess.inputStream).readText()
                                                .trim()
                                        stateProcess.waitFor()
                                        if (stateStr != "Playing" && stateStr != "Paused") return@runBlocking

                                        val posProcess = ProcessBuilder(
                                            "sh", "-c",
                                            "playerctl -p $(playerctl -l | grep '^melo' | head -n 1) position"
                                        ).start()
                                        val posOutput =
                                            InputStreamReader(posProcess.inputStream).readText()
                                                .trim()
                                        posProcess.waitFor()

                                        val posSecResult = posOutput.toDoubleOrNull()
                                        if (posSecResult != null) {
                                            val currentPosUs = (posSecResult * 1000000.0).toLong()
                                            val activeLine =
                                                lrcLines.lastOrNull { it.first <= currentPosUs }
                                            currentLyric = activeLine?.second ?: "..."
                                            activeProgressTask.update { completed = currentPosUs }
                                        } else {
                                            break
                                        }
                                    } catch (_: Exception) {
                                        break
                                    }
                                    delay(500)
                                }
                                job.cancel()
                                activeProgressTask.clear()
                            } else {
                                terminal.println("\n$lyricsText\n")
                                if (synced) {
                                    while (true) {
                                        val curTrackProcess = ProcessBuilder(
                                            "sh", "-c",
                                            "playerctl -p $(playerctl -l | grep '^melo' | head -n 1) metadata --format \"{{title}}|||{{artist}}\""
                                        ).start()
                                        val curTrackStr =
                                            InputStreamReader(curTrackProcess.inputStream).readText()
                                                .trim()
                                        curTrackProcess.waitFor()
                                        if (curTrackStr != "$title|||$artist") break
                                        delay(2000)
                                    }
                                }
                            }
                        } else {
                            terminal.println(gray("No lyrics found for currently playing track."))
                            if (synced) {
                                while (true) {
                                    val curTrackProcess = ProcessBuilder(
                                        "sh", "-c",
                                        "playerctl -p $(playerctl -l | grep '^melo' | head -n 1) metadata --format \"{{title}}|||{{artist}}\""
                                    ).start()
                                    val curTrackStr =
                                        InputStreamReader(curTrackProcess.inputStream).readText()
                                            .trim()
                                    curTrackProcess.waitFor()
                                    if (curTrackStr != "$title|||$artist") break
                                    delay(2000)
                                }
                            }
                        }
                        if (!synced) break
                    }
                } else if (!query.isNullOrBlank()) {
                    terminal.println(gray("Searching for track '$query'..."))

                    val track = searchTracks(query!!).firstOrNull()
                    if (track == null) {
                        terminal.println(gray("No track found for query: $query"))
                        return@runBlocking
                    }

                    terminal.println(cyan("Fetching lyrics for ${track.title} by ${track.artist}..."))
                    val lyricsText = if (synced) getSyncedLyrics(
                        track.artist,
                        track.title
                    ) else getLyrics(track.artist, track.title)
                    if (!lyricsText.isNullOrBlank()) {
                        terminal.println(lyricsText)
                    } else {
                        terminal.println(gray("No lyrics found for ${track.title} by ${track.artist}."))
                    }
                } else {
                    terminal.println(gray("You must provide either a query or use the --current flag."))
                }
            }
        } catch (e: Exception) {
            terminal.println(gray("Error fetching lyrics: ${e.message}"))
        } finally {
            stopKoin()
        }
    }
}
