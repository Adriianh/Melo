package com.github.adriianh.innertube.utils

import com.github.adriianh.innertube.models.response.PlayerResponse
import com.github.adriianh.innertube.pages.NewPipeExtractor

actual object YouTubeStreamUtils {
    actual fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): Result<String> {
        return NewPipeExtractor.getStreamUrl(format, videoId)
    }

    actual fun getSignatureTimestamp(videoId: String): Result<Int> {
        return NewPipeExtractor.getSignatureTimestamp(videoId)
    }

    actual fun prewarm(videoId: String) {
        NewPipeExtractor.prewarm(videoId)
    }
}