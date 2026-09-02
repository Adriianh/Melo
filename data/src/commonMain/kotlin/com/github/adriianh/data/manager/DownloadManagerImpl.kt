package com.github.adriianh.data.manager

import com.github.adriianh.core.domain.manager.DownloadManager
import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.DownloadType
import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import com.github.adriianh.core.domain.usecase.settings.GetSettingsUseCase
import com.github.adriianh.core.platform.PlatformFileSystem
import com.github.adriianh.core.util.MeloDispatchers
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class DownloadManagerImpl(
    private val httpClient: HttpClient,
    private val getStreamUseCase: GetStreamUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val offlineRepository: OfflineRepository,
    private val configDirPath: String,
    private val dispatcher: CoroutineDispatcher = MeloDispatchers.IO
) : DownloadManager {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _activeDownloads = MutableStateFlow<Map<String, Float>>(emptyMap())
    override val activeDownloads: StateFlow<Map<String, Float>> = _activeDownloads.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()

    override suspend fun downloadTrack(track: Track, customPath: String?): Boolean {
        return withContext(dispatcher) {
            val existing = offlineRepository.getOfflineTrack(track.id)
            val existingPath = existing?.localFilePath
            if (existing?.downloadStatus == DownloadStatus.COMPLETED &&
                existingPath != null &&
                PlatformFileSystem.fileExists(existingPath)
            ) {
                return@withContext true
            }

            val settings = getSettingsUseCase.getSnapshot()
            val formatExtension = settings.downloadFormat.displayName.lowercase()
            val safeFileName = "${track.artist} - ${track.title}"
                .replace(Regex("[\\\\/:*?\"<>|]"), "_") + ".$formatExtension"

            val targetFolder = customPath
                ?: settings.downloadPath
                ?: "$configDirPath/downloads"

            PlatformFileSystem.makeDirs(targetFolder)
            val targetFilePath = "$targetFolder/$safeFileName"

            val offlineTrack = OfflineTrack(
                track = track,
                localFilePath = targetFilePath,
                downloadStatus = DownloadStatus.DOWNLOADING,
                downloadType = DownloadType.MANUAL
            )
            offlineRepository.saveOfflineTrack(offlineTrack)
            _activeDownloads.update { it + (track.id to 0f) }

            try {
                val streamUrl = getStreamUseCase(track)
                if (streamUrl == null) {
                    offlineRepository.saveOfflineTrack(
                        offlineTrack.copy(downloadStatus = DownloadStatus.FAILED)
                    )
                    _activeDownloads.update { it - track.id }
                    return@withContext false
                }

                val response = httpClient.get(streamUrl) {
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            val progress =
                                (bytesSentTotal.toFloat() / contentLength.toFloat()).coerceIn(
                                    0f,
                                    1f
                                )
                            _activeDownloads.update { it + (track.id to progress) }
                        }
                    }
                }

                val bytes = response.bodyAsBytes()
                PlatformFileSystem.writeBytes(targetFilePath, bytes)

                val completedTrack = offlineTrack.copy(
                    downloadStatus = DownloadStatus.COMPLETED,
                    downloadedAt = Clock.System.now().toEpochMilliseconds(),
                    fileSize = bytes.size.toLong()
                )
                offlineRepository.saveOfflineTrack(completedTrack)
                _activeDownloads.update { it - track.id }
                true
            } catch (e: Exception) {
                offlineRepository.saveOfflineTrack(
                    offlineTrack.copy(downloadStatus = DownloadStatus.FAILED)
                )
                _activeDownloads.update { it - track.id }
                PlatformFileSystem.deleteFile(targetFilePath)
                false
            }
        }
    }

    override suspend fun downloadTracks(tracks: List<Track>, customPath: String?) {
        scope.launch {
            for (track in tracks) {
                downloadTrack(track, customPath)
            }
        }
    }

    override suspend fun cancelDownload(trackId: String) {
        downloadJobs[trackId]?.cancel()
        downloadJobs.remove(trackId)
        _activeDownloads.update { it - trackId }
        val offlineTrack = offlineRepository.getOfflineTrack(trackId)
        val path = offlineTrack?.localFilePath
        if (offlineTrack != null && offlineTrack.downloadStatus == DownloadStatus.DOWNLOADING) {
            offlineRepository.removeOfflineTrack(trackId)
            if (path != null) {
                PlatformFileSystem.deleteFile(path)
            }
        }
    }

    override suspend fun deleteDownload(trackId: String) {
        cancelDownload(trackId)
        offlineRepository.removeOfflineTrack(trackId)
    }

    override suspend fun isDownloaded(trackId: String): Boolean {
        val track = offlineRepository.getOfflineTrack(trackId) ?: return false
        val path = track.localFilePath
        return track.downloadStatus == DownloadStatus.COMPLETED &&
                path != null &&
                PlatformFileSystem.fileExists(path)
    }
}