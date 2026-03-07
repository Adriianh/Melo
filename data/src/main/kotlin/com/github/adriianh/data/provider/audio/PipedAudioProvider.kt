package com.github.adriianh.data.provider.audio

import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.data.remote.piped.PipedApiClient

class PipedAudioProvider(
    private val apiClient: PipedApiClient
) : AudioProvider {
    override suspend fun getSourceId(artist: String, title: String, durationMs: Long): String? {
        val cleanTitle = title
            .replace(Regex("\\s*\\(.*?\\)"), "")
            .replace(Regex("\\s*\\[.*?]"), "")
            .trim()
        val query = "$cleanTitle $artist"
        return apiClient.search(query, title, artist, durationMs)
    }

    override suspend fun getStreamUrl(sourceId: String): String? {
        return apiClient.getStreamUrl(sourceId)
    }
}