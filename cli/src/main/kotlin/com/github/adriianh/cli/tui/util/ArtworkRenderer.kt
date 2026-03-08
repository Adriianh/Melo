package com.github.adriianh.cli.tui.util

import com.github.adriianh.cli.tui.*

import dev.tamboui.image.ImageData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Loads remote artwork images for display in the TUI.
 *
 * Receives the shared [HttpClient] from the DI container instead of owning
 * a separate instance — this avoids spinning up a second CIO engine with its
 * own thread pool alongside the one already used for all API calls.
 *
 * Uses an LRU cache (max [MAX_CACHE_SIZE] entries) to avoid re-downloading
 * the same artwork and to bound the number of decoded images held in memory.
 */
class ArtworkRenderer(private val httpClient: HttpClient) {

    companion object {
        private const val MAX_CACHE_SIZE = 3
    }

    private val cache = object : LinkedHashMap<String, ImageData>(MAX_CACHE_SIZE + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageData>): Boolean =
            size > MAX_CACHE_SIZE
    }

    suspend fun load(artworkUrl: String): ImageData? {
        cache[artworkUrl]?.let { return it }
        return try {
            val bytes = httpClient.get(artworkUrl).body<ByteArray>()
            ImageData.fromBytes(bytes)?.also { cache[artworkUrl] = it }
        } catch (_: Exception) {
            null
        }
    }

    /** Removes all cached artwork, freeing memory immediately. */
    fun clearCache() {
        cache.clear()
    }
}