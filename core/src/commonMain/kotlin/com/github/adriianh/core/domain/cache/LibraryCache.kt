package com.github.adriianh.core.domain.cache

import com.github.adriianh.core.domain.model.LibraryCacheData

interface LibraryCache {
    fun getSync(): LibraryCacheData?

    suspend fun get(): LibraryCacheData?

    suspend fun save(data: LibraryCacheData)

    suspend fun clear()
}
