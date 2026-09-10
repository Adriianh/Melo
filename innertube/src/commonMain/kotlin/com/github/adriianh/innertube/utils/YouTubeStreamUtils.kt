package com.github.adriianh.innertube.utils

import com.github.adriianh.innertube.models.response.PlayerResponse

/**
 * Platform-specific stream URL de-obfuscation and processing.
 */
expect object YouTubeStreamUtils {
    /**
     * Resolves the actual stream URL from a YouTube format.
     * Handles signature decryption and throttling de-obfuscation.
     */
    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): Result<String>

    /**
     * Fetches a signature timestamp (sts) for the given video.
     */
    fun getSignatureTimestamp(videoId: String): Result<Int>

    /**
     * Pre-warms the JavaScript player engine and STS cache in the background.
     */
    fun prewarm(videoId: String = "dQw4w9WgXcQ")
}
