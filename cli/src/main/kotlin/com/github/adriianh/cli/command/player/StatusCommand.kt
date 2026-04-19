package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.di.appModule
import com.github.adriianh.core.domain.usecase.search.GetLyricsUseCase
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
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

    override fun help(context: Context): String =
        "Show current playing status from MPRIS (Linux) and optionally its lyrics"

    override fun run() {
        startKoin { modules(appModule) }

        try {
            val getLyrics: GetLyricsUseCase by inject()

            val process = ProcessBuilder(
                "playerctl",
                "-p",
                "melo,melo*",
                "metadata",
                "--format",
                "{{title}}|||{{artist}}|||{{album}}"
            )
                .redirectErrorStream(true)
                .start()

            val output = InputStreamReader(process.inputStream).readText().trim()
            process.waitFor()

            if (process.exitValue() != 0 || output.isBlank() || output == "No players found") {
                if (format == "json") {
                    echo("{}")
                } else {
                    echo("No Melo player is currently running.")
                }
                return
            }

            val parts = output.split("|||")
            val title = parts.getOrNull(0)?.trim() ?: "Unknown"
            val artist = parts.getOrNull(1)?.trim() ?: "Unknown"
            val album = parts.getOrNull(2)?.trim() ?: "Unknown"

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
                                "lyrics": "$escapedLyrics"
                            }
                            """.trimIndent()
                                .replace(Regex(",\\s*\"lyrics\": \"\""), ",\"lyrics\": null")
                        )
                    } else {
                        if (!lyricsText.isNullOrBlank()) {
                            echo("Lyrics for $title by $artist:\n\n$lyricsText")
                        } else {
                            echo("No lyrics found for $title by $artist.")
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
                            "album": "$album"
                        }
                        """.trimIndent()
                    )
                } else {
                    echo("Playing: $title - $artist ($album)")
                }
            }
        } catch (e: Exception) {
            echo("Failed to get status via playerctl: ${e.message}", err = true)
        } finally {
            stopKoin()
        }
    }
}
