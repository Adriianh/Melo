package com.github.adriianh.innertube.utils

import com.github.adriianh.innertube.models.response.PlayerResponse
import com.github.adriianh.innertube.pages.NewPipeUtils

actual object YouTubeStreamUtils {
    actual fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): Result<String> {
        return NewPipeUtils.getStreamUrl(format, videoId)
    }

    actual fun getSignatureTimestamp(videoId: String): Result<Int> {
        return NewPipeUtils.getSignatureTimestamp(videoId)
    }
}
