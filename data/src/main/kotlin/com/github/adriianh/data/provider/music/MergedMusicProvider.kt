package com.github.adriianh.data.provider.music

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
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
        deduplicate(mergeLists(jobs.awaitAll()))
    }

    override suspend fun searchAlbums(query: String): List<SearchResult.Album> = coroutineScope {
        val jobs = providers.map { async { runCatching { it.searchAlbums(query) }.getOrDefault(emptyList()) } }
        mergeLists(jobs.awaitAll()).distinctBy { it.id }
    }

    override suspend fun searchArtists(query: String): List<SearchResult.Artist> = coroutineScope {
        val jobs = providers.map { async { runCatching { it.searchArtists(query) }.getOrDefault(emptyList()) } }
        mergeLists(jobs.awaitAll()).distinctBy { it.id }
    }

    override suspend fun searchPlaylists(query: String): List<SearchResult.Playlist> = coroutineScope {
        val jobs = providers.map { async { runCatching { it.searchPlaylists(query) }.getOrDefault(emptyList()) } }
        mergeLists(jobs.awaitAll()).distinctBy { it.id }
    }

    override suspend fun searchAll(query: String): List<Track> = coroutineScope {
        val jobs = providers.map { async { runCatching { it.searchAll(query) }.getOrDefault(emptyList()) } }
        deduplicate(mergeLists(jobs.awaitAll()))
    }

    override suspend fun searchAllAlbums(query: String): List<SearchResult.Album> = coroutineScope {
        val jobs = providers.map { async { runCatching { it.searchAllAlbums(query) }.getOrDefault(emptyList()) } }
        mergeLists(jobs.awaitAll()).distinctBy { it.id }
    }

    override suspend fun searchAllArtists(query: String): List<SearchResult.Artist> = coroutineScope {
        val jobs = providers.map { async { runCatching { it.searchAllArtists(query) }.getOrDefault(emptyList()) } }
        mergeLists(jobs.awaitAll()).distinctBy { it.id }
    }

    override suspend fun searchAllPlaylists(query: String): List<SearchResult.Playlist> = coroutineScope {
        val jobs = providers.map { async { runCatching { it.searchAllPlaylists(query) }.getOrDefault(emptyList()) } }
        mergeLists(jobs.awaitAll()).distinctBy { it.id }
    }

    override suspend fun getAlbumDetails(id: String): SearchResult.Album? {
        for (provider in providers) {
            val result = runCatching { provider.getAlbumDetails(id) }.getOrNull()
            if (result != null && result.songs != null) return result
        }
        return null
    }

    override suspend fun getArtistDetails(id: String): SearchResult.Artist? {
        for (provider in providers) {
            val result = runCatching { provider.getArtistDetails(id) }.getOrNull()
            if (result != null && (result.topSongs != null || result.description != null)) return result
        }
        return null
    }

    override suspend fun getPlaylistDetails(id: String): SearchResult.Playlist? {
        for (provider in providers) {
            val result = runCatching { provider.getPlaylistDetails(id) }.getOrNull()
            if (result != null && result.trackCount != null) return result
        }
        return null
    }

    override suspend fun getTrack(id: String): Track? {
        val provider = when {
            id.startsWith("itunes:") -> providers.filterIsInstance<ItunesMusicProvider>().firstOrNull()
            id.startsWith("piped:")  -> providers.filterIsInstance<PipedMusicProvider>().firstOrNull()
            id.startsWith("spotify:") || !id.contains(':') ->
                providers.filterIsInstance<SpotifyMusicProvider>().firstOrNull()
            else -> null
        }
        if (provider != null) return runCatching { provider.getTrack(id) }.getOrNull()

        for (p in providers) {
            val track = runCatching { p.getTrack(id) }.getOrNull()
            if (track != null) return track
        }
        return null
    }

    /** Round-robin interleave: [A1,A2,A3] + [B1,B2] → [A1,B1,A2,B2,A3] */
    private fun <T> mergeLists(lists: List<List<T>>): List<T> {
        val result = mutableListOf<T>()
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