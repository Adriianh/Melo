package com.github.adriianh.innertube.utils

import com.github.adriianh.innertube.models.response.PlayerResponse

actual object YouTubeStreamUtils {
    actual fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): Result<String> {
        return format.url?.let { Result.success(it) }
            ?: Result.failure(Exception("Direct URL not available and de-obfuscation not implemented for this platform yet"))
    }

    actual fun getSignatureTimestamp(videoId: String): Result<Int> {
        return Result.failure(Exception("Not implemented"))
    }

    actual fun prewarm(videoId: String) {}
}
