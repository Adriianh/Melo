package com.github.adriianh.data.provider.audio

import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.data.remote.piped.PipedApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class PipedAudioProvider(
    private val apiClient: PipedApiClient,
    private val fallback: AudioProvider? = null
) : AudioProvider {

    override suspend fun getSourceId(artist: String, title: String, durationMs: Long): String? {
        val id = apiClient.search(title, title, artist, durationMs)
        return if (!id.isNullOrBlank()) id else fallback?.getSourceId(artist, title, durationMs)
    }

    override suspend fun getStreamUrl(sourceId: String): String? = withContext(Dispatchers.IO) {
        val url = apiClient.getStreamUrl(sourceId)
        if (url != null) return@withContext url
        fallback?.getStreamUrl(sourceId)
    }

    override suspend fun downloadAudio(
        source: String,
        destination: String,
        format: String,
        quality: String,
        embedMetadata: Boolean
    ): String? = null
}
