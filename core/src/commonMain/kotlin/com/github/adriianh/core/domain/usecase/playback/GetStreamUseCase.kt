package com.github.adriianh.core.domain.usecase.playback

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.platform.PlatformFileSystem
import com.github.adriianh.core.util.MeloDispatchers
import kotlinx.coroutines.withContext

class GetStreamUseCase(
    private val audioProvider: AudioProvider,
    private val offlineRepository: OfflineRepository
) {
    /**
     * Resolves the direct audio stream URL for a given track.
     * First checks if the track is available offline.
     * Otherwise, obtains the sourceId (YouTube videoId via Piped search),
     * and resolves the actual stream URL from that sourceId.
     *
     * @return the stream URL, or null if resolution failed.
     */
    suspend fun resolveSourceId(track: Track): String? = withContext(MeloDispatchers.IO) {
        if (!track.sourceId.isNullOrBlank() && !track.sourceId.contains(":")) {
            return@withContext track.sourceId
        }
        if (track.id.startsWith("piped:")) {
            val id = track.id.removePrefix("piped:")
            if (!id.contains(":") && id.isNotBlank()) return@withContext id
        }
        if (!track.id.contains(":") && track.id.length == 11) {
            return@withContext track.id
        }
        audioProvider.getSourceId(
            artist = track.artist,
            title = track.title,
            durationMs = track.durationMs,
        )
    }

    suspend operator fun invoke(track: Track): String? = withContext(MeloDispatchers.IO) {
        if (track.id.startsWith("local:")) {
            val path = track.id.removePrefix("local:")
            if (PlatformFileSystem.fileExists(path) && PlatformFileSystem.fileSize(path) > 1024) {
                return@withContext PlatformFileSystem.toFileUri(path)
            }
        }

        val offlineTrack = offlineRepository.getOfflineTrack(track.id)
            ?: track.sourceId?.let { sid ->
                offlineRepository.getOfflineTracks().find {
                    it.track.sourceId == sid && it.downloadStatus == DownloadStatus.COMPLETED
                }
            }

        if (offlineTrack?.downloadStatus == DownloadStatus.COMPLETED && offlineTrack.localFilePath != null) {
            val localPath = offlineTrack.localFilePath
            if (PlatformFileSystem.fileExists(localPath) && PlatformFileSystem.fileSize(localPath) > 64 * 1024) {
                offlineRepository.markTrackAsAccessed(offlineTrack.track.id)
                return@withContext PlatformFileSystem.toFileUri(localPath)
            }
        }

        val sourceId = resolveSourceId(track) ?: return@withContext null

        audioProvider.getStreamUrl(sourceId)
    }
}