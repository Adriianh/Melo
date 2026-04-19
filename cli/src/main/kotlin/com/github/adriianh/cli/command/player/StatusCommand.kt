package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.usecase.search.GetLyricsUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.table.horizontalLayout
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.ProgressBar
import com.github.ajalt.mordant.widgets.Text
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

    private val terminal = Terminal()

    override fun help(context: Context): String =
        "Show current playing status from MPRIS (Linux) and optionally its lyrics"

    override fun run() {
        startKoin { modules(appModule) }

        try {
            val getLyrics: GetLyricsUseCase by inject()

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

            if (lyrics) {
                runBlocking {
                    val lyricsText = getLyrics(artist, title)
                    if (format == "json") {
                        val escapedLyrics =
                            lyricsText?.replace("\"", "\\\"")?.replace("\n", "\\n") ?: ""
                        echo(
                            """
                            {
                                "title": "$title",
                                "artist": "$artist",
                                "album": "$album",
                                "position_ms": ${positionUs / 1000},
                                "duration_ms": ${lengthUs / 1000},
                                "lyrics": "$escapedLyrics"
                            }
                            """.trimIndent()
                                .replace(Regex(",\\s*\"lyrics\": \"\""), ",\"lyrics\": null")
                        )
                    } else {
                        if (!lyricsText.isNullOrBlank()) {
                            terminal.println("Lyrics for " + cyan(title) + " by " + cyan(artist) + ":\n\n$lyricsText")
                        } else {
                            terminal.println(gray("No lyrics found for $title by $artist."))
                        }
                    }
                }
            } else {
                if (format == "json") {
                    echo(
                        """
                        {
                            "title": "$title",
                            "artist": "$artist",
                            "album": "$album",
                            "position_ms": ${positionUs / 1000},
                            "duration_ms": ${lengthUs / 1000}
                        }
                        """.trimIndent()
                    )
                } else {
                    terminal.println("Playing: " + cyan(title) + " - " + gray("$artist ($album)"))
                    if (lengthUs > 0) {
                        terminal.println(horizontalLayout {
                            cell(ProgressBar(total = lengthUs, completed = positionUs, width = 30))
                            cell(Text(gray(" $posStr / $lenStr")))
                        })
                    }
                }
            }
        } catch (e: Exception) {
            terminal.println(gray("Failed to get status via playerctl: ${e.message}"))
        } finally {
            stopKoin()
        }
    }
}
