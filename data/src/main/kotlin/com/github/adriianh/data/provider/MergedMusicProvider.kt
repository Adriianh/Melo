package com.github.adriianh.data.provider

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.MusicProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * MusicProvider that fans out a query to multiple providers **in parallel** and
 * merges the results, deduplicating by `(artist, normalised-title, duration-bucket)`.
 *
 * Result ordering: each provider's results keep their original relevance order;
 * providers are interleaved round-robin so the merged list starts with the most
 * relevant result from each source rather than exhausting one source first.
 */
class MergedMusicProvider(
    private val providers: List<MusicProvider>
) : MusicProvider {

    override suspend fun search(query: String): List<Track> = coroutineScope {
        val jobs = providers.map { async { runCatching { it.search(query) }.getOrDefault(emptyList()) } }
        deduplicate(merge(jobs.awaitAll()))
    }

    override suspend fun searchAll(query: String): List<Track> = coroutineScope {
        val jobs = providers.map { async { runCatching { it.searchAll(query) }.getOrDefault(emptyList()) } }
        deduplicate(merge(jobs.awaitAll()))
    }

    override suspend fun getTrack(id: String): Track? {
        for (provider in providers) {
            val track = runCatching { provider.getTrack(id) }.getOrNull()
            if (track != null) return track
        }
        return null
    }

    /** Round-robin interleave: [A1,A2,A3] + [B1,B2] → [A1,B1,A2,B2,A3] */
    private fun merge(lists: List<List<Track>>): List<Track> {
        val result = mutableListOf<Track>()
        val iterators = lists.map { it.iterator() }
        var anyHasNext = true
        while (anyHasNext) {
            anyHasNext = false
            for (iter in iterators) {
                if (iter.hasNext()) {
                    result.add(iter.next())
                    anyHasNext = true
                }
            }
        }
        return result
    }

    private fun deduplicate(tracks: List<Track>): List<Track> {
        val seen = mutableSetOf<String>()
        return tracks.filter { track ->
            val title = track.title
                .lowercase()
                .replace(Regex("""\s*[(\[{].*?[)\]}]"""), "")
                .trim()
            val durationBucket = track.durationMs / 10_000
            val key = "${track.artist.lowercase()}|$title|$durationBucket"
            seen.add(key)
        }
    }
}