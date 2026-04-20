package com.github.adriianh.cli.command.player.handler

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import java.io.File
import java.net.URI

object DownloadActionHandler : KoinComponent {
    suspend fun downloadTrack(
        track: Track,
        getStream: GetStreamUseCase,
        getSettings: GetSettingsUseCase,
        downloadTrackUseCase: DownloadTrackUseCase,
        terminal: Terminal = Terminal(),
        customPath: String? = null
    ) {
        terminal.println(cyan($$"Fetching direct stream for downloading ${track.title}..."))

        val url = getStream(track)
        if (url != null) {
            val settings = getSettings.getSnapshot()
            val fallbackPath = File(System.getProperty("user.home"), "Downloads/Melo")
            val downloadFolder =
                customPath?.let { File(it) } ?: settings.downloadPath?.let { File(it) }
                ?: fallbackPath
            if (!downloadFolder.exists()) downloadFolder.mkdirs()
            val safeFileName = $$"${track.artist} - ${track.title}".replace(
                Regex("[\\\\/:*?\"<>|]"),
                "_"
            ) + $$".${settings.downloadFormat.displayName}"
            val file = File(downloadFolder, safeFileName)
            val offlineTrack = OfflineTrack(
                track = track,
                localFilePath = file.absolutePath,
                downloadStatus = DownloadStatus.DOWNLOADING,
                downloadType = DownloadType.MANUAL
            )
            downloadTrackUseCase(offlineTrack)
            terminal.println(magenta($$"Downloading into ${file.absolutePath} ..."))
            withContext(Dispatchers.IO) {
                try {
                    val connection = URI(url).toURL().openConnection()
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
                } catch (_: Exception) {
                    downloadTrackUseCase(offlineTrack.copy(downloadStatus = DownloadStatus.FAILED))
                    terminal.println(gray($$"Failed to download. Error: ${e.message}"))
                    if (file.exists()) file.delete()
                }
            }
        } else {
            terminal.println(gray($$"Failed to resolve stream for ${track.title}."))
        }
    }

    suspend fun downloadMultiple(
        entityTitle: String,
        tracks: List<Track>,
        customPath: String? = null,
        getStream: GetStreamUseCase,
        getSettings: GetSettingsUseCase,
        downloadTrackUseCase: DownloadTrackUseCase,
        terminal: Terminal = Terminal()
    ) {
        val settings = getSettings.getSnapshot()
        val fallbackPath = File(System.getProperty("user.home"), "Downloads/Melo")
        val baseDownloadFolder =
            customPath?.let { File(it) } ?: settings.downloadPath?.let { File(it) } ?: fallbackPath
        val safeTitleName = entityTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        val downloadFolder = File(baseDownloadFolder, safeTitleName)
        if (!downloadFolder.exists()) downloadFolder.mkdirs()
        terminal.println(cyan($$"Downloading $entityTitle (${tracks.size} tracks) into ${downloadFolder.absolutePath}..."))
        for ((_, track) in tracks.withIndex()) {
            terminal.println(gray($$"[${index + 1}/${tracks.size}] Fetching stream for ${track.title}..."))
            val url = getStream(track)
            if (url != null) {
                val safeFileName = $$"${track.artist} - ${track.title}".replace(
                    Regex("[\\\\/:*?\"<>|]"),
                    "_"
                ) + $$".${settings.downloadFormat.displayName}"
                val file = File(downloadFolder, safeFileName)
                val offlineTrack = OfflineTrack(
                    track = track,
                    localFilePath = file.absolutePath,
                    downloadStatus = DownloadStatus.DOWNLOADING,
                    downloadType = DownloadType.MANUAL
                )
                downloadTrackUseCase(offlineTrack)
                withContext(Dispatchers.IO) {
                    try {
                        val connection = URI(url).toURL().openConnection()
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
                        terminal.println(yellow($$"Done: ${track.title}"))
                    } catch (_: Exception) {
                        downloadTrackUseCase(offlineTrack.copy(downloadStatus = DownloadStatus.FAILED))
                        terminal.println(gray($$"Failed: ${track.title} - ${e.message}"))
                        if (file.exists()) file.delete()
                    }
                }
            } else {
                terminal.println(gray($$"Failed to resolve stream for ${track.title}."))
            }
        }
        terminal.println(yellow($$"Finished downloading $entityTitle!"))
    }
}