package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.cache.LibraryCache
import com.github.adriianh.core.domain.model.LibraryCacheData
import com.github.adriianh.core.platform.PlatformFileSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class LibraryCacheImpl(
    configDirPath: String,
    private val dispatcher: CoroutineDispatcher,
) : LibraryCache {
    private val cacheFilePath = "$configDirPath/library_cache.json"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var memoryCache: LibraryCacheData? = null

    override fun getSync(): LibraryCacheData? = memoryCache

    override suspend fun get(): LibraryCacheData? = memoryCache ?: withContext(dispatcher) {
        loadSync().also { memoryCache = it }
    }

    override suspend fun save(data: LibraryCacheData) {
        memoryCache = data
        withContext(dispatcher) {
            try {
                val text = json.encodeToString(LibraryCacheData.serializer(), data)
                PlatformFileSystem.writeText(cacheFilePath, text)
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun clear() {
        memoryCache = null
        withContext(dispatcher) {
            try {
                PlatformFileSystem.deleteFile(cacheFilePath)
            } catch (_: Exception) {
            }
        }
    }

    private fun loadSync(): LibraryCacheData? = runCatching {
        if (PlatformFileSystem.fileExists(cacheFilePath)) {
            PlatformFileSystem.readText(cacheFilePath)?.let { content ->
                json.decodeFromString(LibraryCacheData.serializer(), content)
            }
        } else {
            null
        }
    }.getOrNull()
}
