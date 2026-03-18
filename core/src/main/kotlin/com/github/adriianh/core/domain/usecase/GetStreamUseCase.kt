package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.repository.OfflineRepository
import com.github.adriianh.core.domain.model.DownloadStatus
import java.io.File

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
            val file = File(path)
            if (file.exists()) return "file://${file.absolutePath}"
        }

        val offlineTrack = offlineRepository.getOfflineTrack(track.id)
        if (offlineTrack?.downloadStatus == DownloadStatus.COMPLETED && offlineTrack.localFilePath != null) {
            val file = File(offlineTrack.localFilePath)
            if (file.exists()) return "file://${file.absolutePath}"
        }

        val sourceId = track.sourceId ?: audioProvider.getSourceId(
            artist = track.artist,
            title = track.title,
            durationMs = track.durationMs,
        ) ?: return null

        return audioProvider.getStreamUrl(sourceId)
    }
}