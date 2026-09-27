package com.github.adriianh.cli.command.player

import com.github.adriianh.cli.command.player.util.VerticalProgressBarMaker
import com.github.adriianh.cli.di.appModule
import com.github.adriianh.cli.tui.player.ipc.LocalIpcClient
import com.github.adriianh.core.domain.player.PlaybackStatusDto
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
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.InputStreamReader
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

private data class TrackStatusSnapshot(
    val title: String,
    val artist: String,
    val album: String,
    val positionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val volume: Int,
)

class StatusCommand :
    CliktCommand(
        name = "status",
    ),
    KoinComponent {
    private val format by option(
        names = arrayOf("-f", "--format"),
        help = "Output format: plain or json (default: plain)",
    ).choice("json", "plain", ignoreCase = true).default("plain")
    private val lyrics by option(
        "-l",
        "--lyrics",
        help = "Fetch and display lyrics for the currently playing track",
    ).flag(default = false)

    private val synced by option(
        "-s",
        "--synced",
        help = "Fetch synchronized lyrics (LRC format) when using --lyrics",
    ).flag(default = false)

    private val live by option(
        "--live",
        help = "Continuously update progress bar until interrupted",
    ).flag("--no-live", default = true)

    private val terminal = Terminal()
    private val json = Json { ignoreUnknownKeys = true }

    override fun help(context: Context): String = "Display playback status, track metadata, progress, and lyrics."

    override fun helpEpilog(context: Context): String =
        "Examples:\n" +
            "  melo status\n" +
            "  melo status --format json\n" +
            "  melo status --lyrics --synced\n" +
            "  melo status --no-live"

    override fun run() {
        startKoin { modules(appModule) }
        try {
            val getLyrics: GetLyricsUseCase by inject()
            val getSyncedLyrics: GetSyncedLyricsUseCase by inject()

            runBlocking {
                while (true) {
                    val status = queryStatus()
                    if (status == null) {
                        if (format == "json") {
                            echo("{}")
                        } else {
                            terminal.println(gray("No Melo player is currently running."))
                        }
                        return@runBlocking
                    }

                    val lyricsText =
                        when {
                            !lyrics -> null
                            synced -> getSyncedLyrics(status.artist, status.title)
                            else -> getLyrics(status.artist, status.title)
                        }

                    if (format == "json") {
                        renderJsonStatus(status, lyricsText)
                        return@runBlocking
                    }

                    terminal.println(
                        "\nPlaying: ${cyan(status.title)} - ${gray("${status.artist} (${status.album})")}",
                    )
                    val lrcLines =
                        if (synced && lyricsText != null) parseSyncedLyrics(lyricsText) else emptyList()
                    if (lyricsText != null && lrcLines.isEmpty()) {
                        terminal.println(
                            "\nLyrics for ${cyan(status.title)} by ${cyan(status.artist)}:\n\n$lyricsText\n",
                        )
                    }

                    if (status.durationMs > 0) {
                        val isInteractive = terminal.terminalInfo.interactive || live
                        if (isInteractive) {
                            runLiveProgress(status, lrcLines)
                        } else {
                            renderStaticProgress(status)
                        }
                    }

                    if (!live) break
                    delay(1000.milliseconds)
                }
            }
        } finally {
            stopKoin()
        }
    }

    private fun queryStatus(): TrackStatusSnapshot? {
        val fromIpc =
            runCatching {
                val ipcRes = LocalIpcClient.sendCommand("STATUS")
                if (ipcRes.startsWith("OK ")) {
                    val dto =
                        json.decodeFromString<PlaybackStatusDto>(ipcRes.removePrefix("OK ").trim())
                    dto.track?.let { track ->
                        TrackStatusSnapshot(
                            title = track.title,
                            artist = track.artist,
                            album = track.album,
                            positionMs = dto.positionMs,
                            durationMs = if (dto.durationMs > 0) dto.durationMs else track.durationMs,
                            isPlaying = dto.isPlaying,
                            volume = dto.volume,
                        )
                    }
                } else {
                    null
                }
            }.getOrNull()
        return fromIpc ?: queryPlayerctlStatus()
    }

    private fun queryPlayerctlStatus(): TrackStatusSnapshot? {
        if (System.getProperty("os.name").lowercase().contains("win")) return null
        return runCatching {
            val process =
                ProcessBuilder(
                    "sh",
                    "-c",
                    "playerctl -p $(playerctl -l | grep '^melo' | head -n 1) metadata " +
                        "--format \"{{title}}|||{{artist}}|||{{album}}|||{{position}}|||{{mpris:length}}\"",
                ).redirectErrorStream(true).start()
            val output = InputStreamReader(process.inputStream).readText().trim()
            process.waitFor()
            if (process.exitValue() == 0 && output.isNotBlank() && output != "No players found") {
                val parts = output.split("|||")
                val posUs = parts.getOrNull(3)?.trim()?.toLongOrNull() ?: 0L
                val lenUs = parts.getOrNull(4)?.trim()?.toLongOrNull() ?: 0L
                TrackStatusSnapshot(
                    title = parts.getOrNull(0)?.trim() ?: "Unknown",
                    artist = parts.getOrNull(1)?.trim() ?: "Unknown",
                    album = parts.getOrNull(2)?.trim() ?: "Unknown",
                    positionMs = posUs / 1000L,
                    durationMs = lenUs / 1000L,
                    isPlaying = true,
                    volume = 75,
                )
            } else {
                null
            }
        }.getOrNull()
    }

    private fun parseSyncedLyrics(lyricsText: String): List<Pair<Long, String>> {
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
        return lyricsText.lines().mapNotNull { line ->
            val match = regex.find(line.trim()) ?: return@mapNotNull null
            val m = match.groupValues[1].toLong()
            val s = match.groupValues[2].toLong()
            val msStr = match.groupValues[3]
            val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
            val timeMs = (m * 60_000L) + (s * 1000L) + ms
            timeMs to match.groupValues[4].trim()
        }
    }

    private fun renderJsonStatus(
        status: TrackStatusSnapshot,
        lyricsText: String?,
    ) {
        val escapedLyrics =
            lyricsText
                ?.replace("\"", "\\\"")
                ?.replace("\n", "\\n")
        val jsonLyricsPart =
            if (escapedLyrics != null) ",\n    \"lyrics\": \"$escapedLyrics\"" else ""
        echo(
            """
            {
                "title": "${status.title}",
                "artist": "${status.artist}",
                "album": "${status.album}",
                "position_ms": ${status.positionMs},
                "duration_ms": ${status.durationMs},
                "is_playing": ${status.isPlaying},
                "volume": ${status.volume}$jsonLyricsPart
            }
            """.trimIndent(),
        )
    }

    private suspend fun runLiveProgress(
        initialStatus: TrackStatusSnapshot,
        lrcLines: List<Pair<Long, String>>,
    ) {
        var currentLyric = if (lrcLines.isNotEmpty()) "..." else ""
        val activeProgressTask =
            progressBarLayout {
                if (lrcLines.isNotEmpty()) {
                    text(cyan("♪"))
                    text { cyan(currentLyric) }
                }
                text("Time")
                text {
                    val currentSec = completed / 1000L
                    val tlSec = (total ?: 0L) / 1000L
                    val currentStr =
                        String.format(Locale.ROOT, "%02d:%02d", currentSec / 60, currentSec % 60)
                    val tlStr = String.format(Locale.ROOT, "%02d:%02d", tlSec / 60, tlSec % 60)
                    gray("$currentStr / $tlStr")
                }
                text("Progress")
                progressBar()
                text("")
                text(gray("Press Ctrl+C to stop."))
            }.animateOnThread(
                terminal,
                total = initialStatus.durationMs,
                maker = VerticalProgressBarMaker,
            )

        val job = CoroutineScope(Dispatchers.IO).launch { activeProgressTask.execute() }
        try {
            while (true) {
                val status = queryStatus() ?: break
                if (status.title != initialStatus.title || status.artist != initialStatus.artist) break
                if (lrcLines.isNotEmpty()) {
                    val activeLine = lrcLines.lastOrNull { it.first <= status.positionMs }
                    currentLyric = activeLine?.second ?: "..."
                }
                activeProgressTask.update { completed = status.positionMs }
                delay(500.milliseconds)
            }
        } finally {
            job.cancel()
            activeProgressTask.clear()
        }
    }

    private fun renderStaticProgress(status: TrackStatusSnapshot) {
        val posSec = status.positionMs / 1000L
        val lenSec = status.durationMs / 1000L
        val posStr = String.format(Locale.ROOT, "%02d:%02d", posSec / 60, posSec % 60)
        val lenStr = String.format(Locale.ROOT, "%02d:%02d", lenSec / 60, lenSec % 60)
        terminal.println(
            horizontalLayout {
                cell(
                    ProgressBar(
                        total = status.durationMs,
                        completed = status.positionMs,
                        width = 30,
                    ),
                )
                cell(Text(gray(" $posStr / $lenStr")))
            },
        )
    }
}
