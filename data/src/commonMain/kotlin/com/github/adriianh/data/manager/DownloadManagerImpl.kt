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
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
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
    private val dispatcher: CoroutineDispatcher = MeloDispatchers.IO,
) : DownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _activeDownloads = MutableStateFlow<Map<String, Float>>(emptyMap())
    override val activeDownloads: StateFlow<Map<String, Float>> = _activeDownloads.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()

    override suspend fun downloadTrack(
        track: Track,
        customPath: String?,
    ): Boolean {
        return withContext(dispatcher) {
            val existing = offlineRepository.getOfflineTrack(track.id)
            val existingPath = existing?.localFilePath
            val isCompleted =
                existing?.downloadStatus == DownloadStatus.COMPLETED &&
                    existingPath != null &&
                    PlatformFileSystem.fileExists(existingPath)

            val settings = getSettingsUseCase.getSnapshot()
            val formatExtension = settings.downloadFormat.displayName.lowercase()
            val safeFileName =
                "${track.artist} - ${track.title}"
                    .replace(Regex("[\\\\/:*?\"<>|]"), "_") + ".$formatExtension"

            val targetFolder =
                customPath
                    ?: settings.downloadPath
                    ?: "$configDirPath/downloads"

            PlatformFileSystem.makeDirs(targetFolder)
            val targetFilePath = "$targetFolder/$safeFileName"

            if (isCompleted) {
                if (existing.downloadType == DownloadType.MANUAL && existingPath == targetFilePath) {
                    return@withContext true
                }
                if (existingPath != targetFilePath &&
                    PlatformFileSystem.fileExists(
                        existingPath,
                    )
                ) {
                    PlatformFileSystem.copyFile(existingPath, targetFilePath)
                }
                val promotedTrack =
                    existing.copy(
                        localFilePath = targetFilePath,
                        downloadStatus = DownloadStatus.COMPLETED,
                        downloadType = DownloadType.MANUAL,
                        downloadedAt = Clock.System.now().toEpochMilliseconds(),
                        fileSize = existing.fileSize,
                    )
                offlineRepository.saveOfflineTrack(promotedTrack)
                try {
                    offlineRepository.updateTrackMetadata(
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                    )
                } catch (_: Exception) {
                }
                return@withContext true
            }

            val offlineTrack =
                OfflineTrack(
                    track = track,
                    localFilePath = targetFilePath,
                    downloadStatus = DownloadStatus.DOWNLOADING,
                    downloadType = DownloadType.MANUAL,
                )
            offlineRepository.saveOfflineTrack(offlineTrack)
            _activeDownloads.update { it + (track.id to 0f) }

            val tempFilePath = "$targetFilePath.tmp"

            try {
                val streamUrl = getStreamUseCase(track)
                if (streamUrl == null) {
                    offlineRepository.saveOfflineTrack(
                        offlineTrack.copy(
                            downloadStatus = DownloadStatus.FAILED,
                            localFilePath = null,
                        ),
                    )
                    _activeDownloads.update { it - track.id }
                    return@withContext false
                }

                val totalBytes =
                    streamToFile(streamUrl, tempFilePath) { written, total ->
                        if (total != null && total > 0) {
                            val progress = (written.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                            _activeDownloads.update { it + (track.id to progress) }
                        }
                    }

                if (totalBytes <= MIN_VALID_AUDIO_BYTES) {
                    throw IllegalStateException("Downloaded audio file is too small ($totalBytes bytes)")
                }

                PlatformFileSystem.copyFile(tempFilePath, targetFilePath)
                PlatformFileSystem.deleteFile(tempFilePath)

                val completedTrack =
                    offlineTrack.copy(
                        localFilePath = targetFilePath,
                        downloadStatus = DownloadStatus.COMPLETED,
                        downloadedAt = Clock.System.now().toEpochMilliseconds(),
                        fileSize = totalBytes,
                    )
                offlineRepository.saveOfflineTrack(completedTrack)
                try {
                    offlineRepository.updateTrackMetadata(
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                    )
                } catch (_: Exception) {
                }
                _activeDownloads.update { it - track.id }
                true
            } catch (_: Exception) {
                offlineRepository.saveOfflineTrack(
                    offlineTrack.copy(downloadStatus = DownloadStatus.FAILED, localFilePath = null),
                )
                _activeDownloads.update { it - track.id }
                PlatformFileSystem.deleteFile(tempFilePath)
                PlatformFileSystem.deleteFile(targetFilePath)
                false
            }
        }
    }

    override suspend fun cacheTrack(track: Track): Boolean =
        when {
            track.id.startsWith("local:") -> true
            track.durationMs !in 1..MAX_AUTO_CACHE_DURATION_MS -> false
            else -> withContext(dispatcher) { performCacheTrack(track) }
        }

    private suspend fun performCacheTrack(track: Track): Boolean {
        val existing = offlineRepository.getOfflineTrack(track.id)
        if (isCachedAndValid(existing)) {
            offlineRepository.markTrackAsAccessed(track.id)
            return true
        }

        val settings = getSettingsUseCase.getSnapshot()
        val formatExtension = settings.downloadFormat.displayName.lowercase()
        val safeFileName = "cache_${track.id.hashCode()}.$formatExtension"
        val targetFolder = settings.cachePath ?: "$configDirPath/cache"

        PlatformFileSystem.makeDirs(targetFolder)
        val targetFilePath = "$targetFolder/$safeFileName"
        val tempFilePath = "$targetFilePath.tmp"

        return executeCacheDownload(track, targetFilePath, tempFilePath, settings.maxOfflineSizeMb)
    }

    private suspend fun executeCacheDownload(
        track: Track,
        targetFilePath: String,
        tempFilePath: String,
        maxOfflineSizeMb: Int,
    ): Boolean =
        try {
            val streamUrl = getStreamUseCase(track)
            if (streamUrl == null) {
                false
            } else {
                val totalBytes = streamToFile(streamUrl, tempFilePath)
                if (totalBytes <= MIN_VALID_AUDIO_BYTES) {
                    throw IllegalStateException("Cached audio file is too small ($totalBytes bytes)")
                }

                PlatformFileSystem.copyFile(tempFilePath, targetFilePath)
                PlatformFileSystem.deleteFile(tempFilePath)

                val cachedTrack =
                    OfflineTrack(
                        track = track,
                        localFilePath = targetFilePath,
                        downloadStatus = DownloadStatus.COMPLETED,
                        downloadType = DownloadType.CACHE,
                        downloadedAt = Clock.System.now().toEpochMilliseconds(),
                        lastAccessedAt = Clock.System.now().toEpochMilliseconds(),
                        fileSize = totalBytes,
                    )
                offlineRepository.saveOfflineTrack(cachedTrack)

                if (maxOfflineSizeMb > 0) {
                    offlineRepository.cleanupCache(maxOfflineSizeMb)
                }
                true
            }
        } catch (_: Exception) {
            PlatformFileSystem.deleteFile(tempFilePath)
            PlatformFileSystem.deleteFile(targetFilePath)
            false
        }

    private suspend fun streamToFile(
        streamUrl: String,
        tempFilePath: String,
        onProgress: ((bytesWritten: Long, totalBytes: Long?) -> Unit)? = null,
    ): Long =
        httpClient.prepareGet(streamUrl).execute { response ->
            val contentLength = response.contentLength()
            val channel: ByteReadChannel = response.bodyAsChannel()
            var bytesWritten = 0L
            val buffer = ByteArray(STREAM_BUFFER_SIZE)

            PlatformFileSystem.writeStream(tempFilePath) { writeChunk ->
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead <= 0) break
                    writeChunk(buffer, 0, bytesRead)
                    bytesWritten += bytesRead
                    onProgress?.invoke(bytesWritten, contentLength)
                }
            }
            bytesWritten
        }

    private fun isCachedAndValid(existing: OfflineTrack?): Boolean {
        val path = existing?.localFilePath ?: return false
        return existing.downloadStatus == DownloadStatus.COMPLETED &&
            PlatformFileSystem.fileExists(path) &&
            PlatformFileSystem.fileSize(path) > MIN_VALID_AUDIO_BYTES
    }

    override suspend fun downloadTracks(
        tracks: List<Track>,
        customPath: String?,
    ) {
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
        val track = offlineRepository.getOfflineTrack(trackId)
        val path = track?.localFilePath
        val isManualCompleted =
            track?.downloadStatus == DownloadStatus.COMPLETED &&
                track.downloadType == DownloadType.MANUAL
        return isManualCompleted &&
            path != null &&
            isDownloadedFileValid(path)
    }

    private fun isDownloadedFileValid(path: String): Boolean =
        PlatformFileSystem.fileExists(path) &&
            PlatformFileSystem.fileSize(path) > MIN_VALID_AUDIO_BYTES

    companion object {
        const val MAX_AUTO_CACHE_DURATION_MS = 45 * 60 * 1000L
        private const val STREAM_BUFFER_SIZE = 64 * 1024
        private const val MIN_VALID_AUDIO_BYTES = 64 * 1024L
    }
}
