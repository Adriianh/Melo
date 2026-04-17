package com.github.adriianh.cli.command.player

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.usecase.offline.DownloadTrackUseCase
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.animation.progress.animateOnThread
import com.github.ajalt.mordant.animation.progress.execute
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.magenta
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.progress.completed
import com.github.ajalt.mordant.widgets.progress.percentage
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.github.ajalt.mordant.widgets.progress.speed
import com.github.ajalt.mordant.widgets.progress.text
import com.github.ajalt.mordant.widgets.progress.timeRemaining
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.yellow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object SearchActionHandler {
    suspend fun handleTrackAction(
        track: Track,
        getStream: GetStreamUseCase,
        getSettings: GetSettingsUseCase,
        downloadTrack: DownloadTrackUseCase,
        terminal: Terminal = Terminal()
    ) {
        val actions = listOf("Play with ffplay", "Download", "Cancel")
        val selectedAction = SearchPickers.pickItem(
            actions,
            "What would you like to do with '${track.title}'?"
        ) { _, action, isSelected ->
            if (isSelected) {
                yellow { textLine("> $action") }
            } else {
                textLine("  $action")
            }
        }
        when (selectedAction) {
            "Play with ffplay" -> playTrack(track, getStream, terminal)
            "Download" -> downloadTrack(track, getStream, getSettings, downloadTrack, terminal)
            else -> terminal.println("Action cancelled.")
        }
    }

    private suspend fun playTrack(track: Track, getStream: GetStreamUseCase, terminal: Terminal) {
        terminal.println(cyan("Fetching stream URL for ${track.title}..."))
        val url = getStream(track)
        if (url != null) {
            terminal.println(magenta("Playing with ffplay..."))
            try {
                val process = withContext(Dispatchers.IO) {
                    Runtime.getRuntime().exec(arrayOf("ffplay", "-nodisp", "-autoexit", url))
                }
                val durationSec = track.durationMs / 1000
                var elapsedSec = 0L
                val progress = progressBarLayout {
                    text("${track.title} - ${track.artist}")
                    percentage()
                    progressBar()
                    text { SearchOutputFormatter.formatDuration(elapsedSec * 1000) }
                    text("/")
                    text(SearchOutputFormatter.formatDuration(track.durationMs))
                }.animateOnThread(
                    terminal,
                    total = if (durationSec > 0) durationSec else null
                )
                val progressJob = CoroutineScope(Dispatchers.Default).launch {
                    progress.execute()
                    while (process.isAlive) {
                        delay(1000)
                        elapsedSec++
                        progress.advance(1)
                    }
                }
                withContext(Dispatchers.IO) {
                    process.waitFor()
                }
                progressJob.cancel()
            } catch (e: Exception) {
                terminal.println(gray("Failed to launch ffplay. Is it installed? Error: ${e.message}"))
            }
        } else {
            terminal.println(gray("Failed to resolve stream for ${track.title}."))
        }
    }

    private suspend fun downloadTrack(
        track: Track,
        getStream: GetStreamUseCase,
        getSettings: GetSettingsUseCase,
        downloadTrackUseCase: DownloadTrackUseCase,
        terminal: Terminal
    ) {
        terminal.println(cyan("Fetching direct stream for downloading ${track.title}..."))
        val url = getStream(track)
        if (url != null) {
            val settings = getSettings.getSnapshot()
            val fallbackPath = File(System.getProperty("user.home"), "Downloads/Melo")
            val downloadFolder = settings.downloadPath?.let { File(it) } ?: fallbackPath
            if (!downloadFolder.exists()) downloadFolder.mkdirs()
            val safeFileName = "${track.artist} - ${track.title}".replace(
                Regex("[\\\\/:*?\"<>|]"),
                "_"
            ) + ".${settings.downloadFormat.displayName}"
            val file = File(downloadFolder, safeFileName)
            val offlineTrack = OfflineTrack(
                track = track,
                localFilePath = file.absolutePath,
                downloadStatus = DownloadStatus.DOWNLOADING,
                downloadType = DownloadType.MANUAL
            )
            downloadTrackUseCase(offlineTrack)
            terminal.println(magenta("Downloading into ${file.absolutePath} ..."))
            withContext(Dispatchers.IO) {
                try {
                    val connection = URL(url).openConnection()
                    val totalBytes = connection.contentLengthLong
                    val progress = progressBarLayout {
                        text(safeFileName)
                        percentage()
                        progressBar()
                        completed()
                        speed()
                        timeRemaining()
                    }.animateOnThread(
                        terminal,
                        total = if (totalBytes > 0) totalBytes else null
                    )
                    val progressJob = CoroutineScope(Dispatchers.Default).launch {
                        progress.execute()
                    }
                    connection.getInputStream().use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                progress.advance(bytesRead.toLong())
                            }
                        }
                    }
                    progressJob.cancel()
                    downloadTrackUseCase(
                        offlineTrack.copy(
                            downloadStatus = DownloadStatus.COMPLETED,
                            downloadedAt = System.currentTimeMillis(),
                            fileSize = file.length()
                        )
                    )
                    terminal.println(yellow("Download complete!"))
                } catch (e: Exception) {
                    downloadTrackUseCase(offlineTrack.copy(downloadStatus = DownloadStatus.FAILED))
                    terminal.println(gray("Failed to download. Error: ${e.message}"))
                    if (file.exists()) file.delete()
                }
            }
        } else {
            terminal.println(gray("Failed to resolve stream for ${track.title}."))
        }
    }
}
