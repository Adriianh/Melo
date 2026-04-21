package com.github.adriianh.data.provider.audio

import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.models.SongItem
import com.github.adriianh.innertube.models.YouTubeClient
import com.github.adriianh.innertube.pages.NewPipeUtils
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AudioProvider backed by InnerTube API (YouTube Music).
 *
 * Provides fallback to another AudioProvider (e.g. yt-dlp) if InnerTube fails to
 * retrieve the stream URL or if a download is requested (until a native downloader is implemented).
 */
class InnerTubeAudioProvider(
    private val fallback: AudioProvider? = null
) : AudioProvider {

    private val streamUrlCache = ConcurrentHashMap<String, String>()
    private val sourceIdCache = ConcurrentHashMap<String, String>()

    private fun getUrlExpiryTimeMs(url: String): Long? {
        val regex = Regex("""[?&]expire=(\d+)""")
        val match = regex.find(url) ?: return null
        return match.groupValues[1].toLongOrNull()?.times(1000L)
    }

    override suspend fun getSourceId(artist: String, title: String, durationMs: Long): String? {
        val cacheKey = "$artist$title$durationMs"
        sourceIdCache[cacheKey]?.let { return it }

        return try {
            val query = "$artist - $title"
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()

            val firstItem = result?.items?.filterIsInstance<SongItem>()?.firstOrNull()?.id

            if (firstItem != null) {
                sourceIdCache[cacheKey] = firstItem
                firstItem
            } else {
                fallback?.getSourceId(artist, title, durationMs)
            }
        } catch (_: Exception) {
            fallback?.getSourceId(artist, title, durationMs)
        }
    }

    override suspend fun getStreamUrl(sourceId: String): String? {
        streamUrlCache[sourceId]?.let { url ->
            val expiryTimeMs = getUrlExpiryTimeMs(url)
            if (expiryTimeMs == null || System.currentTimeMillis() < (expiryTimeMs - 5 * 60 * 1000L)) {
                return url
            }
            streamUrlCache.remove(sourceId)
        }

        return withContext(Dispatchers.IO) {
            try {
                val playerResponse = YouTube.player(sourceId, null, YouTubeClient.WEB_REMIX).getOrThrow()

                val formats = playerResponse.streamingData?.adaptiveFormats?.filter {
                    it.mimeType.startsWith("audio/")
                }?.sortedByDescending { it.bitrate }

                val bestFormat = formats?.firstOrNull()
                    ?: throw IllegalStateException("No audio formats found")

                val url = NewPipeUtils.getStreamUrl(bestFormat, sourceId).getOrThrow()

                streamUrlCache[sourceId] = url
                url
            } catch (_: Exception) {
                fallback?.getStreamUrl(sourceId)
            }
        }
    }

    override suspend fun downloadAudio(
        source: String,
        destination: String,
        format: String,
        quality: String,
        embedMetadata: Boolean
    ): String? {
        // Delegate download to yt-dlp fallback for now, as it handles ffmpeg conversions
        // and metadata embedding natively
        return fallback?.downloadAudio(source, destination, format, quality, embedMetadata)
    }
}


