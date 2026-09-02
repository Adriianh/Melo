package com.github.adriianh.core.domain.usecase.playback

import com.github.adriianh.core.domain.model.DownloadStatus
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.platform.PlatformFileSystem

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
    suspend operator fun invoke(track: Track): String? {
        if (track.id.startsWith("local:")) {
            val path = track.id.removePrefix("local:")
            if (PlatformFileSystem.fileExists(path)) {
                return PlatformFileSystem.toFileUri(path)
            }
        }

        val offlineTrack = offlineRepository.getOfflineTrack(track.id)
            ?: track.sourceId?.let { sid ->
                offlineRepository.getOfflineTracks().find {
                    it.track.sourceId == sid && it.downloadStatus == DownloadStatus.COMPLETED
                }
            }

        if (offlineTrack?.downloadStatus == DownloadStatus.COMPLETED && offlineTrack.localFilePath != null) {
            if (PlatformFileSystem.fileExists(offlineTrack.localFilePath)) {
                offlineRepository.markTrackAsAccessed(offlineTrack.track.id)
                return PlatformFileSystem.toFileUri(offlineTrack.localFilePath)
            }
        }

        val sourceId = track.sourceId
            ?: (if (track.id.startsWith("piped:")) track.id.removePrefix("piped:") else null)
            ?: audioProvider.getSourceId(
                artist = track.artist,
                title = track.title,
                durationMs = track.durationMs,
            ) ?: return null

        return audioProvider.getStreamUrl(sourceId)
    }
}