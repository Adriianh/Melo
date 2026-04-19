package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.util.VerticalProgressBarMaker
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.usecase.search.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.GetSyncedLyricsUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.animation.progress.animateOnThread
import com.github.ajalt.mordant.animation.progress.execute
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.table.horizontalLayout
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.ProgressBar
import com.github.ajalt.mordant.widgets.Text
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

class StatusCommand : CliktCommand(
    name = "status"
), KoinComponent {
    private val format by option(
        names = arrayOf("-f", "--format"),
        help = "Output format: json or plain (default: plain)"
    ).choice("json", "plain", ignoreCase = true).default("plain")
    private val lyrics by option(
        "-l", "--lyrics",
        help = "Fetch and display the lyrics for the currently playing track"
    ).flag(default = false)

    private val synced by option(
        "-s", "--synced",
        help = "Fetch synchronized lyrics (LRC format) when using --lyrics"
    ).flag(default = false)

    private val live by option(
        "--live",
        help = "Live updating progress bar (runs continuously until stopped)"
    ).flag(default = true)

    private val terminal = Terminal()

    override fun help(context: Context): String =
        "Show current playing status from MPRIS (Linux) and optionally its lyrics"

    override fun run() {
        startKoin { modules(appModule) }

        try {
            val getLyrics: GetLyricsUseCase by inject()
            val getSyncedLyrics: GetSyncedLyricsUseCase by inject()

            val process = ProcessBuilder(
                "sh", "-c",
                "playerctl -p $(playerctl -l | grep '^melo' | head -n 1) metadata --format \"{{title}}|||{{artist}}|||{{album}}|||{{position}}|||{{mpris:length}}\""
            )
                .redirectErrorStream(true)
                .start()

            val output = InputStreamReader(process.inputStream).readText().trim()
            process.waitFor()

            if (process.exitValue() != 0 || output.isBlank() || output == "No players found") {
                if (format == "json") {
                    echo("{}")
                } else {
                    terminal.println(gray("No Melo player is currently running."))
                }
                return
            }

            val parts = output.split("|||")
            val title = parts.getOrNull(0)?.trim() ?: "Unknown"
            val artist = parts.getOrNull(1)?.trim() ?: "Unknown"
            val album = parts.getOrNull(2)?.trim() ?: "Unknown"
            val positionRaw = parts.getOrNull(3)?.trim() ?: "0"
            val lengthRaw = parts.getOrNull(4)?.trim() ?: "0"

            val positionUs = positionRaw.toLongOrNull() ?: 0L
            val lengthUs = lengthRaw.toLongOrNull() ?: 0L
            val posSec = positionUs / 1000000L
            val lenSec = lengthUs / 1000000L

            val posStr = String.format("%02d:%02d", posSec / 60, posSec % 60)
            val lenStr = String.format("%02d:%02d", lenSec / 60, lenSec % 60)

            var lyricsText: String? = null
            if (lyrics) {
                runBlocking {
                    lyricsText =
                        if (synced) getSyncedLyrics(artist, title) else getLyrics(artist, title)
                }
            }

            if (format == "json") {
                val escapedLyrics = lyricsText?.replace("\"", "\\\"")?.replace("\n", "\\n")
                val jsonLyricsPart =
                    if (escapedLyrics != null) ",\n    \"lyrics\": \"$escapedLyrics\"" else ""
                echo(
                    """
                    {
                        "title": "$title",
                        "artist": "$artist",
                        "album": "$album",
                        "position_ms": ${positionUs / 1000},
                        "duration_ms": ${lengthUs / 1000}$jsonLyricsPart
                    }
                    """.trimIndent()
                )
                return
            }

            terminal.println("Playing: " + cyan(title) + " - " + gray("$artist ($album)"))

            val lrcLines = mutableListOf<Pair<Long, String>>()
            if (lyricsText != null) {
                if (synced && lyricsText.contains(Regex("""\[\d{2}:\d{2}\.\d{2,3}]"""))) {
                    val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
                    lyricsText.lines().forEach { line ->
                        val match = regex.find(line.trim())
                        if (match != null) {
                            val m = match.groupValues[1].toLong()
                            val s = match.groupValues[2].toLong()
                            val msStr = match.groupValues[3]
                            val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                            val timeUs = (m * 60000000L) + (s * 1000000L) + (ms * 1000L)
                            lrcLines.add(timeUs to match.groupValues[4].trim())
                        }
                    }
                } else {
                    terminal.println("\nLyrics for " + cyan(title) + " by " + cyan(artist) + ":\n\n$lyricsText\n")
                }
            }

            if (lengthUs > 0) {
                val isInteractive = terminal.terminalInfo.interactive || live
                if (isInteractive) {
                    var currentLyric = if (lrcLines.isNotEmpty()) "..." else ""
                    val activeProgressTask = progressBarLayout {
                        if (lrcLines.isNotEmpty()) {
                            text(cyan("♪")); text {
                                cyan(currentLyric)
                            }
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
                    }.animateOnThread(terminal, total = lengthUs, maker = VerticalProgressBarMaker)

                    val job = CoroutineScope(Dispatchers.IO).launch {
                        activeProgressTask.execute()
                    }

                    runBlocking {
                        while (true) {
                            try {
                                val stateProcess = ProcessBuilder(
                                    "sh", "-c",
                                    "playerctl -p $(playerctl -l | grep '^melo' | head -n 1) status"
                                ).start()
                                val stateStr =
                                    InputStreamReader(stateProcess.inputStream).readText().trim()
                                stateProcess.waitFor()
                                if (stateStr != "Playing" && stateStr != "Paused") break

                                val posProcess = ProcessBuilder(
                                    "sh", "-c",
                                    "playerctl -p $(playerctl -l | grep '^melo' | head -n 1) position"
                                ).start()
                                val posOutput =
                                    InputStreamReader(posProcess.inputStream).readText().trim()
                                posProcess.waitFor()

                                val posSecResult = posOutput.toDoubleOrNull()
                                if (posSecResult != null) {
                                    val currentPosUs = (posSecResult * 1000000.0).toLong()
                                    if (lrcLines.isNotEmpty()) {
                                        val activeLine =
                                            lrcLines.lastOrNull { it.first <= currentPosUs }
                                        currentLyric = activeLine?.second ?: "..."
                                    }
                                    activeProgressTask.update { completed = currentPosUs }
                                    if (currentPosUs >= lengthUs && stateStr != "Paused") break
                                } else {
                                    break
                                }
                            } catch (_: Exception) {
                                break
                            }
                            delay(500)
                        }
                    }
                    job.cancel()
                    activeProgressTask.clear()
                } else {
                    terminal.println(horizontalLayout {
                        cell(ProgressBar(total = lengthUs, completed = positionUs, width = 30))
                        cell(Text(gray(" $posStr / $lenStr")))
                    })
                }
            }
        } catch (e: Exception) {
            terminal.println(gray("Failed to get status via playerctl: ${e.message}"))
        } finally {
            stopKoin()
        }
    }
}
