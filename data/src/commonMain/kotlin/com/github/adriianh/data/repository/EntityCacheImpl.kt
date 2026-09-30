package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.cache.EntityCache
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.model.search.entityId
import com.github.adriianh.core.platform.PlatformFileSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class EntityCacheImpl(
    configPathDir: String,
    private val dispatcher: CoroutineDispatcher,
) : EntityCache {
    private val cacheFilePath = "$configPathDir/entity_cache.json"
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val serializer = MapSerializer(String.serializer(), SearchResult.serializer())
    private val memoryCache = LinkedHashMap<String, SearchResult>()
    private var isLoaded = false

    override fun getSync(entityId: String): SearchResult? {
        val cleanId = entityId.removePrefix("VL").removePrefix("piped:").removePrefix("itunes:")
        return memoryCache[entityId] ?: memoryCache[cleanId]
    }

    override suspend fun get(entityId: String): SearchResult? = withContext(dispatcher) {
        ensureLoaded()
        getSync(entityId)
    }

    override suspend fun save(entity: SearchResult) {
        val id = entity.entityId
        val cleanId = id.removePrefix("VL").removePrefix("piped:").removePrefix("itunes:")

        withContext(dispatcher) {
            ensureLoaded()
            memoryCache[id] = entity

            if (cleanId != id) {
                memoryCache[cleanId] = entity
            }

            while (memoryCache.size > 100) {
                val oldestKey = memoryCache.keys.firstOrNull() ?: break
                memoryCache.remove(oldestKey)
            }

            try {
                val text = json.encodeToString(serializer, memoryCache)
                PlatformFileSystem.writeText(cacheFilePath, text)
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun clear() {
        withContext(dispatcher) {
            memoryCache.clear()
            isLoaded = true

            try {
                PlatformFileSystem.deleteFile(cacheFilePath)
            } catch (_: Exception) {
            }
        }
    }

    private fun ensureLoaded() {
        if (isLoaded) return
        if (!PlatformFileSystem.fileExists(cacheFilePath)) {
            isLoaded = true
            return
        }

        try {
            val content = PlatformFileSystem.readText(cacheFilePath)
            if (!content.isNullOrBlank()) {
                val decoded = json.decodeFromString(serializer, content)
                memoryCache.putAll(decoded)
            }
        } catch (_: Exception) {
        } finally {
            isLoaded = true
        }
    }
}
