package com.github.adriianh.data.cache

import com.github.adriianh.core.domain.cache.HomeFeedCache
import com.github.adriianh.core.domain.model.HomeFeed
import com.github.adriianh.core.platform.PlatformFileSystem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class HomeFeedCacheImpl(
    configDirPath: String,
    private val dispatcher: CoroutineDispatcher
) : HomeFeedCache {

    private val cacheFilePath = "$configDirPath/home_feed_cache.json"
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var memoryCache: HomeFeed? = null

    override fun getSync(): HomeFeed? = memoryCache

    override suspend fun get(): HomeFeed? = memoryCache ?: withContext(dispatcher) {
        loadSync().also { memoryCache = it }
    }

    override suspend fun save(feed: HomeFeed) {
        memoryCache = feed
        withContext(dispatcher) {
            try {
                val text = json.encodeToString(HomeFeed.serializer(), feed)
                PlatformFileSystem.writeText(cacheFilePath, text)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadSync(): HomeFeed? {
        if (!PlatformFileSystem.fileExists(cacheFilePath)) return null
        return try {
            val content = PlatformFileSystem.readText(cacheFilePath) ?: return null
            json.decodeFromString(HomeFeed.serializer(), content)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}