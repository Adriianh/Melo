package com.github.adriianh.core.domain.cache

import com.github.adriianh.core.domain.model.search.SearchResult

interface EntityCache {
    fun getSync(entityId: String): SearchResult?

    suspend fun get(entityId: String): SearchResult?

    suspend fun save(entity: SearchResult)

    suspend fun clear()
}
