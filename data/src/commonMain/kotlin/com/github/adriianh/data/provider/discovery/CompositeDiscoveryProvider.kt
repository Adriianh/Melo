package com.github.adriianh.data.provider.discovery

import com.github.adriianh.core.domain.model.SimilarTrack
import com.github.adriianh.core.domain.provider.DiscoveryProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Queries all [DiscoveryProvider]s in parallel and merges their results.
 * Duplicate tracks (same normalized title + artist) are deduplicated, keeping
 * the entry with the highest match score. Results are sorted by match descending.
 *
 * [getGenres] still uses a first-wins strategy since genre lists don't benefit
 * from merging.
 */
class CompositeDiscoveryProvider(
    private val providers: List<DiscoveryProvider>,
) : DiscoveryProvider {

    override suspend fun getSimilarTracks(artist: String, title: String, limit: Int): List<SimilarTrack> =
        coroutineScope {
            providers
                .map { provider -> async { runCatching { provider.getSimilarTracks(artist, title, limit) }.getOrElse { emptyList() } } }
                .flatMap { it.await() }
                .groupBy { normalize(it.artist) to normalize(it.title) }
                .values
                .map { duplicates -> duplicates.maxByOrNull { it.match } ?: duplicates.first() }
                .sortedByDescending { it.match }
        }

    override suspend fun getGenres(artist: String): List<String> {
        for (provider in providers) {
            val result = runCatching { provider.getGenres(artist) }
                .getOrElse { emptyList() }
            if (result.isNotEmpty()) return result
        }
        return emptyList()
    }

    private fun normalize(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
}