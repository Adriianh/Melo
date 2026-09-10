package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.repository.StreamCacheRepository
import com.github.adriianh.core.platform.currentTimeSeconds
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.data.local.MeloDatabase
import kotlinx.coroutines.withContext

class StreamCacheRepositoryImpl(database: MeloDatabase) : StreamCacheRepository {

    private val queries = database.streamCacheQueries

    override suspend fun getCachedUrl(trackId: String, maxAgeMs: Long): String? =
        withContext(MeloDispatchers.IO) {
            runCatching {
                val row = queries.selectStream(trackId).executeAsOneOrNull()
                    ?: return@runCatching null
                val nowMs = currentTimeSeconds() * 1000L
                if (nowMs - row.resolved_at > maxAgeMs) {
                    queries.deleteStream(trackId)
                    null
                } else {
                    row.stream_url
                }
            }.getOrNull()
        }

    override suspend fun cacheUrl(trackId: String, url: String) {
        withContext(MeloDispatchers.IO) {
            runCatching {
                queries.upsertStream(trackId, url, currentTimeSeconds() * 1000L)
            }
        }
    }

    override suspend fun invalidate(trackId: String) {
        withContext(MeloDispatchers.IO) {
            runCatching { queries.deleteStream(trackId) }
        }
    }
}