package com.github.adriianh.innertube.pages

/**
 * Platform-specific access to NewPipe's StreamInfo extraction.
 * Implementations must be provided per platform (android/jvm/ios) using `actual`.
 */
expect fun getNewPipeStreamUrls(videoId: String): List<Pair<Int, String>>
