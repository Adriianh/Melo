package com.github.adriianh.data.player

import com.github.adriianh.core.domain.manager.DownloadManager
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.AgeRestrictedException
import com.github.adriianh.core.domain.repository.StreamCacheRepository
import com.github.adriianh.core.domain.usecase.playback.GetStreamUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

/**
 * Resolves playable stream URLs with retries, manages the in-memory prefetch cache,
 * and schedules background auto-caching of played tracks.
 */
internal class StreamResolver(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val getStreamUseCase: GetStreamUseCase,
    private val streamCacheRepository: StreamCacheRepository?,
    private val downloadManager: DownloadManager?,
    private val currentDurationMs: () -> Long,
) {
    private val prefetchCache = mutableMapOf<String, String>()
    private val cacheMutex = Mutex()
    private var prefetchJob: Job? = null

    suspend fun resolve(track: Track): String? {
        val cachedUrl = cacheMutex.withLock { prefetchCache.remove(track.id) }
        val url =
            cachedUrl
                ?: streamCacheRepository?.getCachedUrl(track.id, STREAM_URL_TTL_MS)
                ?: resolveWithRetry(track)
        if (url != null && !url.startsWith("file:")) {
            streamCacheRepository?.cacheUrl(track.id, url)
        }
        return url
    }

    fun schedulePrefetch(
        tracks: List<Track>,
        currentIndex: Int,
    ) {
        cancelPrefetch()
        val nextIndices = (currentIndex + 1 until minOf(tracks.size, currentIndex + PREFETCH_AHEAD_COUNT))
        if (nextIndices.isEmpty()) return

        prefetchJob =
            scope.launch(dispatcher) {
                delay(PREFETCH_DELAY_MS.milliseconds)
                for (idx in nextIndices) {
                    val nextTrack = tracks.getOrNull(idx)
                    if (nextTrack != null) {
                        prefetchSingleTrack(nextTrack)
                    }
                }
            }
    }

    private suspend fun prefetchSingleTrack(nextTrack: Track) {
        val alreadyCached = cacheMutex.withLock { nextTrack.id in prefetchCache }
        if (alreadyCached) return
        val url = getStreamUseCase(nextTrack) ?: return
        if (!url.startsWith("file:")) {
            streamCacheRepository?.cacheUrl(nextTrack.id, url)
        }
        cacheMutex.withLock { prefetchCache[nextTrack.id] = url }
    }

    fun scheduleAutoCache(
        track: Track,
        url: String,
    ) {
        if (track.id.startsWith("local:") || url.startsWith("file:")) return
        scope.launch(dispatcher) {
            delay(AUTO_CACHE_DELAY_MS.milliseconds)
            val effectiveDuration =
                if (track.durationMs > 0) track.durationMs else currentDurationMs()
            if (effectiveDuration in 1..DownloadManager.MAX_AUTO_CACHE_DURATION_MS) {
                val trackToCache =
                    if (track.durationMs <= 0) track.copy(durationMs = effectiveDuration) else track
                downloadManager?.cacheTrack(trackToCache)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun resolveSourceIdAsync(
        track: Track,
        onResolved: (String) -> Unit,
    ) {
        if (track.sourceId != null) return
        scope.launch(dispatcher) {
            try {
                val sid = getStreamUseCase.resolveSourceId(track)
                if (sid != null) onResolved(sid)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort metadata enrichment; failure does not affect playback.
            }
        }
    }

    fun cancelPrefetch() {
        prefetchJob?.cancel()
        prefetchJob = null
    }

    fun clearCache() {
        scope.launch(dispatcher) { cacheMutex.withLock { prefetchCache.clear() } }
    }

    suspend fun invalidate(trackId: String) {
        streamCacheRepository?.invalidate(trackId)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun resolveWithRetry(track: Track): String? {
        var attempts = 0
        while (attempts < MAX_STREAM_RESOLVE_ATTEMPTS) {
            try {
                val resolved = getStreamUseCase(track)
                if (resolved != null) return resolved
            } catch (e: AgeRestrictedException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Transient failure — retry below.
            }
            attempts++
            if (attempts < MAX_STREAM_RESOLVE_ATTEMPTS) {
                delay(STREAM_RETRY_DELAY_MS.milliseconds)
            }
        }
        return null
    }

    private companion object {
        private const val STREAM_URL_TTL_MS = 3 * 60 * 60 * 1000L
        private const val MAX_STREAM_RESOLVE_ATTEMPTS = 3
        private const val STREAM_RETRY_DELAY_MS = 700L
        private const val PREFETCH_DELAY_MS = 750L
        private const val PREFETCH_AHEAD_COUNT = 3
        private const val AUTO_CACHE_DELAY_MS = 4000L
    }
}
