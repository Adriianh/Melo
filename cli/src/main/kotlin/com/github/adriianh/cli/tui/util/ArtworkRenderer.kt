package com.github.adriianh.cli.tui.util
import dev.tamboui.image.ImageData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
class ArtworkRenderer(private val httpClient: HttpClient) {
    companion object {
        private const val MAX_CACHE_SIZE = 10
    }
    private val cache = object : LinkedHashMap<String, ImageData>(MAX_CACHE_SIZE + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageData>): Boolean =
            size > MAX_CACHE_SIZE
    }
    private val mutex = Mutex()
    suspend fun load(artworkUrl: String): ImageData? = withContext(Dispatchers.IO) {
        mutex.withLock {
            cache[artworkUrl]?.let { return@withContext it }
        }
        try {
            val bytes = httpClient.get(artworkUrl).body<ByteArray>()
            withContext(Dispatchers.Default) {
                ImageData.fromBytes(bytes)?.also {
                    mutex.withLock {
                        cache[artworkUrl] = it
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
