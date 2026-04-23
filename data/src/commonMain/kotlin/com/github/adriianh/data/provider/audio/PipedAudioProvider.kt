package com.github.adriianh.data.provider.audio

import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.data.remote.piped.PipedApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * AudioProvider that resolves stream URLs via a Piped instance.
 * Useful as a fallback when direct InnerTube requests are blocked.
 */
class PipedAudioProvider(
    private val apiClient: PipedApiClient
) : AudioProvider {

    override suspend fun getSourceId(artist: String, title: String, durationMs: Long): String? {
        return apiClient.search(title, title, artist, durationMs)
    }

    override suspend fun getStreamUrl(sourceId: String): String? = withContext(Dispatchers.IO) {
        println("Attempting Piped fallback for $sourceId...")
        val url = apiClient.getStreamUrl(sourceId)
        if (url != null) {
            println("Piped fallback successful!")
        } else {
            println("Piped fallback failed to return a URL.")
        }
        url
    }

    override suspend fun downloadAudio(
        source: String,
        destination: String,
        format: String,
        quality: String,
        embedMetadata: Boolean
    ): String? {
        return null
    }
}
