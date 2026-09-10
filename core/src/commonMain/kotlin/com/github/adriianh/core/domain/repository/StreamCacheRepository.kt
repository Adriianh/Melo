package com.github.adriianh.core.domain.repository

interface StreamCacheRepository {
    suspend fun getCachedUrl(trackId: String, maxAgeMs: Long): String?
    suspend fun cacheUrl(trackId: String, url: String)
    suspend fun invalidate(trackId: String)
}