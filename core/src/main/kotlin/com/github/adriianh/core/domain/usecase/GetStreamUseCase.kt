package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.AudioProvider

class GetStreamUseCase(
    private val audioProvider: AudioProvider
) {
    /**
     * Resolves the direct audio stream URL for a given track.
     * First obtains the sourceId (YouTube videoId via Piped search),
     * then resolves the actual stream URL from that sourceId.
     *
     * @return the stream URL, or null if resolution failed.
     */
    suspend operator fun invoke(track: Track): String? {
        val sourceId = track.sourceId ?: audioProvider.getSourceId(
            artist = track.artist,
            title = track.title,
            durationMs = track.durationMs,
        ) ?: return null

        return audioProvider.getStreamUrl(sourceId)
    }
}