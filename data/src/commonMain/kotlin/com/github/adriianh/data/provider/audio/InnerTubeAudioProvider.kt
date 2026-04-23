package com.github.adriianh.data.provider.audio

import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.models.SongItem
import com.github.adriianh.innertube.models.YouTubeClient
import com.github.adriianh.innertube.utils.YouTubeStreamUtils
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * AudioProvider backed by InnerTube API (YouTube Music).
 * Cross-platform implementation using Ktor and InnerTube.
 */
class InnerTubeAudioProvider(
    private val fallback: AudioProvider? = null
) : AudioProvider {

    private val streamUrlCache = mutableMapOf<String, String>()
    private val sourceIdCache = mutableMapOf<String, String>()
    private val mutex = Mutex()

    private fun getUrlExpiryTimeMs(url: String): Long? {
        val regex = Regex("""[?&]expire=(\d+)""")
        val match = regex.find(url) ?: return null
        return match.groupValues[1].toLongOrNull()?.times(1000L)
    }

    override suspend fun getSourceId(artist: String, title: String, durationMs: Long): String? {
        val cacheKey = "$artist$title$durationMs"
        mutex.withLock {
            sourceIdCache[cacheKey]?.let { return it }
        }

        return try {
            val query = "$artist - $title"
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()

            val firstItem = result?.items?.filterIsInstance<SongItem>()?.firstOrNull()?.id

            if (firstItem != null) {
                mutex.withLock {
                    sourceIdCache[cacheKey] = firstItem
                }
                firstItem
            } else {
                fallback?.getSourceId(artist, title, durationMs)
            }
        } catch (_: Exception) {
            fallback?.getSourceId(artist, title, durationMs)
        }
    }

    override suspend fun getStreamUrl(sourceId: String): String? {
        mutex.withLock {
            streamUrlCache[sourceId]?.let { url ->
                val expiryTimeMs = getUrlExpiryTimeMs(url)
                val now = Clock.System.now().toEpochMilliseconds()
                if (expiryTimeMs == null || now < (expiryTimeMs - 5 * 60 * 1000L)) {
                    return url
                }
                streamUrlCache.remove(sourceId)
            }
        }

        return withContext(MeloDispatchers.IO) {
            val clients = listOf(
                YouTubeClient.ANDROID_TESTSUITE,
                YouTubeClient.WEB_REMIX,
                YouTubeClient.ANDROID_VR_1_61_48
            )
            var lastError: Exception? = null

            for (client in clients) {
                try {
                    val sts = if (client.useSignatureTimestamp) {
                        YouTubeStreamUtils.getSignatureTimestamp(sourceId).getOrNull()
                    } else null

                    val playerResponse = YouTube.player(sourceId, null, client, sts).getOrThrow()

                    if (playerResponse.streamingData == null) {
                        println("YouTube Player [${client.clientName}] Error: status=${playerResponse.playabilityStatus.status}, reason=${playerResponse.playabilityStatus.reason}")
                        continue
                    }

                    val formats = playerResponse.streamingData!!.adaptiveFormats.filter {
                        it.mimeType.startsWith("audio/")
                    }.sortedByDescending { it.bitrate }
                    val bestFormat = formats.firstOrNull() ?: continue
                    val url = YouTubeStreamUtils.getStreamUrl(bestFormat, sourceId).getOrThrow()

                    mutex.withLock {
                        streamUrlCache[sourceId] = url
                    }
                    return@withContext url
                } catch (e: Exception) {
                    println("YouTube Player [${client.clientName}] Failed: ${e.message}")
                    lastError = e
                }
            }

            println("All clients failed to resolve stream URL for $sourceId. Last error: ${lastError?.message}")
            fallback?.getStreamUrl(sourceId)
        }
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
