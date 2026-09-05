package com.github.adriianh.data.provider.audio

import com.github.adriianh.core.domain.model.AudioQuality
import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.provider.AudioProvider
import com.github.adriianh.core.domain.repository.SettingsRepository
import com.github.adriianh.core.platform.PlatformFileSystem
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.core.util.retryWithBackoff
import com.github.adriianh.innertube.YouTube
import com.github.adriianh.innertube.models.SongItem
import com.github.adriianh.innertube.models.YouTubeClient
import com.github.adriianh.innertube.models.response.PlayerResponse
import com.github.adriianh.innertube.pages.getNewPipeStreamUrls
import com.github.adriianh.innertube.utils.YouTubeStreamUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

class InnerTubeAudioProvider(
    private val configDirPath: String? = null,
    private val fallback: AudioProvider? = null,
    private val settingsRepository: SettingsRepository? = null,
) : AudioProvider {

    companion object {
        private var cachedSignatureTimestamp: Int? = null
        private var stsLastFetchedMs: Long = 0
        private const val STS_CACHE_DURATION_MS = 6 * 60 * 60 * 1000L
        private val stsMutex = Mutex()

        private val CLIENTS_TO_TRY = listOf(
            YouTubeClient.WEB_REMIX,
            YouTubeClient.ANDROID_VR_NO_AUTH,
            YouTubeClient.IOS,
        )
    }

    private val resolvedUrlCache = mutableMapOf<String, Pair<String, Long>>()
    private val sourceIdCache = mutableMapOf<String, String>()
    private val mutex = Mutex()

    init {
        CoroutineScope(MeloDispatchers.IO).launch {
            try {
                YouTubeStreamUtils.prewarm("dQw4w9WgXcQ")
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun <T> withTimeoutOrNull(timeoutMs: Long, block: suspend () -> T): T? {
        return try {
            withTimeout(timeoutMs.milliseconds) { block() }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getSourceId(artist: String, title: String, durationMs: Long): String? {
        val cacheKey = "$artist$title$durationMs"
        mutex.withLock { sourceIdCache[cacheKey]?.let { return it } }
        return try {
            val query = "$artist - $title"
            val firstItem = retryWithBackoff(maxRetries = 1, initialDelayMs = 200L) {
                val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                result?.items?.filterIsInstance<SongItem>()?.firstOrNull()?.id
            }
            if (firstItem != null) {
                mutex.withLock { sourceIdCache[cacheKey] = firstItem }
                firstItem
            } else {
                fallback?.getSourceId(artist, title, durationMs)
            }
        } catch (_: Exception) {
            fallback?.getSourceId(artist, title, durationMs)
        }
    }

    override suspend fun getStreamUrl(sourceId: String): String? {
        val now = Clock.System.now().toEpochMilliseconds()
        mutex.withLock {
            resolvedUrlCache[sourceId]?.let { (url, expiry) ->
                if (now < expiry) return url
                resolvedUrlCache.remove(sourceId)
            }
        }
        return withContext(MeloDispatchers.IO) {
            val settings = settingsRepository?.getSettings() ?: Settings()
            val targetQuality = if (settings.dataSaver) AudioQuality.LOW else settings.audioQuality

            var resolvedUrl: String? = null
            for (client in CLIENTS_TO_TRY) {
                if (client.loginRequired && YouTube.cookie == null) continue
                try {
                    val sts = if (client.useSignatureTimestamp) getSts(sourceId) else null
                    val resp = withTimeoutOrNull(3000L) {
                        retryWithBackoff(maxRetries = 1, initialDelayMs = 200L) {
                            YouTube.player(sourceId, null, client, sts).getOrNull()
                        }
                    }
                    if (resp?.playabilityStatus?.status != "OK") continue
                    val fmt = findAudioFormat(resp, targetQuality) ?: continue
                    val urlResult = YouTubeStreamUtils.getStreamUrl(fmt, sourceId)
                    urlResult.exceptionOrNull()?.let { e ->
                        if (e.message?.contains("deobfuscat", ignoreCase = true) == true ||
                            e.message?.contains("parse", ignoreCase = true) == true
                        ) {
                            invalidateStsCache()
                        }
                    }
                    val url = urlResult.getOrNull()
                    if (url.isNullOrEmpty()) continue
                    if (url.contains("rqh=1")) continue
                    resolvedUrl = url
                    break
                } catch (_: Exception) {
                }
            }
            if (resolvedUrl.isNullOrEmpty()) {
                val streams = runCatching {
                    retryWithBackoff(maxRetries = 1, initialDelayMs = 300L) {
                        getNewPipeStreamUrls(sourceId)
                    }
                }.getOrDefault(emptyList())

                val preferredItags = when (targetQuality) {
                    AudioQuality.LOW -> listOf(249, 250, 139, 140, 251)
                    AudioQuality.MEDIUM -> listOf(140, 250, 251, 139, 249)
                    AudioQuality.HIGH, AudioQuality.AUTO -> listOf(140, 141, 251, 250, 139)
                }
                resolvedUrl = preferredItags
                    .firstNotNullOfOrNull { itag -> streams.firstOrNull { it.first == itag }?.second }
                    ?: streams.firstOrNull()?.second
            }
            if (resolvedUrl.isNullOrEmpty()) {
                return@withContext fallback?.getStreamUrl(sourceId)
            }
            val expiry = extractExpiry(resolvedUrl) ?: (Clock.System.now()
                .toEpochMilliseconds() + 5 * 60 * 1000L)
            mutex.withLock {
                resolvedUrlCache[sourceId] = resolvedUrl to expiry
            }
            resolvedUrl
        }
    }

    private suspend fun getSts(sourceId: String): Int? {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        return stsMutex.withLock {
            if (cachedSignatureTimestamp != null && (nowMs - stsLastFetchedMs) < STS_CACHE_DURATION_MS) {
                return@withLock cachedSignatureTimestamp
            }
            val stsFile = configDirPath?.let { "$it/sts.txt" }
            stsFile?.let { path ->
                try {
                    PlatformFileSystem.readText(path)?.let { content ->
                        val parts = content.split("|")
                        if (parts.size == 2) {
                            val sts = parts[0].toIntOrNull()
                            val time = parts[1].toLongOrNull()
                            if (sts != null && time != null && (nowMs - time) < STS_CACHE_DURATION_MS) {
                                cachedSignatureTimestamp = sts
                                stsLastFetchedMs = time
                                return@withLock sts
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
            val sts = withTimeoutOrNull(3000L) {
                YouTubeStreamUtils.getSignatureTimestamp(sourceId).getOrNull()
            }
            if (sts != null) {
                cachedSignatureTimestamp = sts
                stsLastFetchedMs = nowMs
                stsFile?.let { path ->
                    try {
                        PlatformFileSystem.writeText(path, "$sts|$nowMs")
                    } catch (_: Exception) {
                    }
                }
            }
            sts
        }
    }

    private suspend fun invalidateStsCache() {
        stsMutex.withLock {
            cachedSignatureTimestamp = null
            stsLastFetchedMs = 0
            configDirPath?.let { path ->
                try {
                    PlatformFileSystem.writeText("$path/sts.txt", "")
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun extractExpiry(url: String): Long? {
        val match = Regex("""[?&]expire=(\d+)""").find(url) ?: return null
        val expireEpochSec = match.groupValues[1].toLongOrNull() ?: return null
        return (expireEpochSec - 5 * 60) * 1000L
    }

    private fun findAudioFormat(
        response: PlayerResponse,
        targetQuality: AudioQuality
    ): PlayerResponse.StreamingData.Format? {
        val audioFormats = (response.streamingData?.adaptiveFormats ?: emptyList())
            .filter {
                (it.mimeType ?: "").startsWith("audio/") && it.audioTrack?.isAutoDubbed != true
            }
        if (audioFormats.isEmpty()) return null
        return when (targetQuality) {
            AudioQuality.LOW -> {
                audioFormats.minByOrNull { it.bitrate ?: 0 }
            }

            AudioQuality.MEDIUM -> {
                audioFormats.minByOrNull { abs((it.bitrate ?: 0) - 128000) }
            }

            AudioQuality.HIGH, AudioQuality.AUTO -> {
                audioFormats.maxByOrNull {
                    (it.bitrate ?: 0) + (if (it.mimeType?.contains("webm") == true) 10240 else 0)
                }
            }
        }
    }

    override suspend fun downloadAudio(
        source: String,
        destination: String,
        format: String,
        quality: String,
        embedMetadata: Boolean,
    ): String? = fallback?.downloadAudio(source, destination, format, quality, embedMetadata)
}