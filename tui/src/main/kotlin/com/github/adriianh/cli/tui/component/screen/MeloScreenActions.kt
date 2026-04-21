package com.github.adriianh.cli.tui.component.screen

import com.github.adriianh.cli.config.shareDir
import com.github.adriianh.cli.tui.MeloScreen
import com.github.adriianh.cli.tui.ScreenState
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import java.io.File

internal fun MeloScreen.loadLocalTracksAction() {
    scope.launch {
        updateScreen<ScreenState.Library> { it.copy(isLoading = true) }
        val paths = settingsViewState.currentSettings.localLibraryPaths
        val tracks = offlineRepository.scanLocalTracks(paths)
        appRunner()?.runOnRenderThread {
            updateScreen<ScreenState.Library> {
                it.copy(localTracks = tracks, isLoading = false)
            }
        }
    }
}

internal fun MeloScreen.deleteDownloadedTrackAction(trackId: String) {
    scope.launch {
        deleteDownloadedTrack.invoke(trackId)
    }
}

internal fun MeloScreen.downloadTrackAction(track: Track, downloadType: DownloadType = DownloadType.PREFETCH) {
    scope.launch {
        downloadSemaphore.withPermit {
            try {
                val existing = offlineRepository.getOfflineTrack(track.id)
                val sourceId = track.sourceId ?: audioProvider.getSourceId(
                    artist = track.artist,
                    title = track.title,
                    durationMs = track.durationMs,
                ) ?: return@launch
                val downloadsDir = File(
                    when (downloadType) {
                        DownloadType.MANUAL -> settingsViewState.currentSettings.downloadPath
                            ?: File(shareDir, "downloads").absolutePath

                        DownloadType.PREFETCH -> settingsViewState.currentSettings.cachePath
                            ?: File(shareDir, "cache").absolutePath
                    }
                )
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                if (existing != null && (existing.downloadStatus == DownloadStatus.COMPLETED || existing.downloadStatus == DownloadStatus.DOWNLOADING)) {
                    // If it's completed as cache and user wants manual, just copy the file
                    if (existing.downloadStatus == DownloadStatus.COMPLETED &&
                        existing.downloadType == DownloadType.PREFETCH &&
                        downloadType == DownloadType.MANUAL &&
                        existing.localFilePath != null
                    ) {
                        val cachedFile = File(existing.localFilePath!!)
                        if (cachedFile.exists()) {
                            val newFile = File(downloadsDir, cachedFile.name)
                            cachedFile.copyTo(newFile, overwrite = true)

                            val upgradedTrack = existing.copy(
                                localFilePath = newFile.absolutePath,
                                downloadType = DownloadType.MANUAL,
                                downloadedAt = System.currentTimeMillis()
                            )
                            downloadTrack.invoke(upgradedTrack)
                            return@launch
                        }
                    }
                    return@launch
                }

                val offlineTrack = OfflineTrack(
                    track = track,
                    downloadStatus = DownloadStatus.DOWNLOADING,
                    downloadType = downloadType
                )
                downloadTrack.invoke(offlineTrack)

                val downloadedPath = audioProvider.downloadAudio(
                    source = sourceId,
                    destination = downloadsDir.absolutePath,
                    format = settingsViewState.currentSettings.downloadFormat.displayName,
                    quality = settingsViewState.currentSettings.downloadQuality.displayName,
                    embedMetadata = (downloadType == DownloadType.MANUAL)
                )
                if (downloadedPath != null) {
                    val file = File(downloadedPath)
                    val completedTrack = offlineTrack.copy(
                        localFilePath = file.absolutePath,
                        downloadStatus = DownloadStatus.COMPLETED,
                        fileSize = file.length(),
                        downloadedAt = System.currentTimeMillis()
                    )
                    downloadTrack.invoke(completedTrack)
                    autoCleanup.invoke(
                        maxAgeDays = settingsViewState.currentSettings.maxOfflineAgeDays,
                        maxSizeMb = settingsViewState.currentSettings.maxOfflineSizeMb
                    )
                } else {
                    downloadTrack.invoke(
                        offlineTrack.copy(downloadStatus = DownloadStatus.FAILED)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                downloadTrack.invoke(
                    OfflineTrack(
                        track = track,
                        downloadStatus = DownloadStatus.FAILED
                    )
                )
            }
        }
    }
}