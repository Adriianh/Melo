package com.github.adriianh.data.provider.music

import com.github.adriianh.core.domain.model.BrowseCategoryResult
import com.github.adriianh.core.domain.model.HomeSection
import com.github.adriianh.core.domain.model.MoodAndGenreGroup
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.model.search.SearchResult
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.core.util.CircuitBreaker
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration.Companion.seconds

/**
 * MusicProvider that fans out a query to multiple providers **in parallel** and
 * merges the results, deduplicating by `(artist, normalised-title, duration-bucket)`.
 *
 * Employs per-provider [CircuitBreaker]s to fail-fast and isolate failing providers.
 *
 * Result ordering: each provider's results keep their original relevance order;
 * providers are interleaved round-robin so the merged list starts with the most
 * relevant result from each source rather than exhausting one source first.
 */
class MergedMusicProvider(
    private val providers: List<MusicProvider>
) : MusicProvider {

    private val circuitBreakers = providers.associateWith { provider ->
        CircuitBreaker(
            name = provider::class.simpleName ?: "MusicProvider",
            failureThreshold = 3,
            cooldownDuration = 60.seconds,
        )
    }

    private suspend fun <T> runWithBreaker(
        provider: MusicProvider,
        block: suspend (MusicProvider) -> T
    ): T? {
        val breaker = circuitBreakers[provider]
        return if (breaker != null) {
            breaker.executeOrNull { block(provider) }
        } else {
            runCatching { block(provider) }.getOrNull()
        }
    }

    override suspend fun search(query: String): List<Track> = coroutineScope {
        val jobs =
            providers.map { async { runWithBreaker(it) { p -> p.search(query) } ?: emptyList() } }
        deduplicate(mergeLists(jobs.awaitAll()))
    }

    override suspend fun searchAlbums(query: String): List<SearchResult.Album> = coroutineScope {
        val jobs =
            providers.map {
                async {
                    runWithBreaker(it) { p -> p.searchAlbums(query) } ?: emptyList()
                }
            }
        mergeLists(jobs.awaitAll()).distinctBy { it.id }
    }

    override suspend fun searchArtists(query: String): List<SearchResult.Artist> = coroutineScope {
        val jobs =
            providers.map {
                async {
                    runWithBreaker(it) { p -> p.searchArtists(query) } ?: emptyList()
                }
            }
        mergeLists(jobs.awaitAll()).distinctBy { it.id }
    }

    override suspend fun searchPlaylists(query: String): List<SearchResult.Playlist> =
        coroutineScope {
            val jobs = providers.map {
                async {
                    runWithBreaker(it) { p -> p.searchPlaylists(query) } ?: emptyList()
                }
            }
            mergeLists(jobs.awaitAll()).distinctBy { it.id }
        }

    override suspend fun searchVideos(query: String): List<Track> = coroutineScope {
        val jobs =
            providers.map {
                async {
                    runWithBreaker(it) { p -> p.searchVideos(query) } ?: emptyList()
                }
            }
        deduplicate(mergeLists(jobs.awaitAll()))
    }

    override suspend fun searchSummary(query: String): List<HomeSection> {
        for (p in providers) {
            val sections = runWithBreaker(p) { it.searchSummary(query) }
            if (!sections.isNullOrEmpty()) return sections
        }
        return emptyList()
    }

    override suspend fun searchAll(query: String): List<Track> = coroutineScope {
        val jobs =
            providers.map {
                async {
                    runWithBreaker(it) { p -> p.searchAll(query) } ?: emptyList()
                }
            }
        deduplicate(mergeLists(jobs.awaitAll()))
    }

    override suspend fun searchAllAlbums(query: String): List<SearchResult.Album> = coroutineScope {
        val jobs =
            providers.map {
                async {
                    runWithBreaker(it) { p -> p.searchAllAlbums(query) } ?: emptyList()
                }
            }
        mergeLists(jobs.awaitAll()).distinctBy { it.id }
    }

    override suspend fun searchAllArtists(query: String): List<SearchResult.Artist> =
        coroutineScope {
            val jobs = providers.map {
                async {
                    runWithBreaker(it) { p -> p.searchAllArtists(query) } ?: emptyList()
                }
            }
            mergeLists(jobs.awaitAll()).distinctBy { it.id }
        }

    override suspend fun searchAllPlaylists(query: String): List<SearchResult.Playlist> =
        coroutineScope {
            val jobs = providers.map {
                async {
                    runWithBreaker(it) { p -> p.searchAllPlaylists(query) } ?: emptyList()
                }
            }
            mergeLists(jobs.awaitAll()).distinctBy { it.id }
        }

    override suspend fun getAlbumDetails(id: String): SearchResult.Album? {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.getAlbumDetails(id) }
            if (result != null && !result.songs.isNullOrEmpty()) return result
        }
        return null
    }

    override suspend fun getArtistDetails(id: String): SearchResult.Artist? {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.getArtistDetails(id) }
            if (result != null && (!result.topSongs.isNullOrEmpty() || result.description != null)) return result
        }
        return null
    }

    override suspend fun getPlaylistDetails(id: String): SearchResult.Playlist? {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.getPlaylistDetails(id) }
            if (result != null) return result
        }
        return null
    }

    override suspend fun getSearchSuggestions(query: String): List<String> {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.getSearchSuggestions(query) }
            if (!result.isNullOrEmpty()) return result
        }
        return emptyList()
    }

    override suspend fun getHome(): List<HomeSection> {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.getHome() }
            if (!result.isNullOrEmpty()) return result
        }
        return emptyList()
    }

    override suspend fun getExplore(): List<HomeSection> {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.getExplore() }
            if (!result.isNullOrEmpty()) return result
        }
        return emptyList()
    }

    override suspend fun getCharts(): List<HomeSection> {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.getCharts() }
            if (!result.isNullOrEmpty()) return result
        }
        return emptyList()
    }

    override suspend fun getTrending(): List<Track> {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.getTrending() }
            if (!result.isNullOrEmpty()) return result
        }
        return emptyList()
    }

    override suspend fun getMoodAndGenres(): List<MoodAndGenreGroup> {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.getMoodAndGenres() }
            if (!result.isNullOrEmpty()) return result
        }
        return emptyList()
    }

    override suspend fun getRadio(videoId: String): List<Track> {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.getRadio(videoId) }
            if (!result.isNullOrEmpty()) return result
        }
        return emptyList()
    }

    override suspend fun getArtistRadio(artistId: String): List<Track> {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.getArtistRadio(artistId) }
            if (!result.isNullOrEmpty()) return result
        }
        return emptyList()
    }

    override suspend fun getRelated(videoId: String): List<Track> {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.getRelated(videoId) }
            if (!result.isNullOrEmpty()) return result
        }
        return emptyList()
    }

    override suspend fun browseCategory(browseId: String, params: String?): BrowseCategoryResult? {
        for (provider in providers) {
            val result = runWithBreaker(provider) { it.browseCategory(browseId, params) }
            if (result != null) return result
        }
        return null
    }

    override suspend fun getTrack(id: String): Track? {
        val provider = when {
            id.startsWith("itunes:") -> providers.filterIsInstance<ItunesMusicProvider>()
                .firstOrNull()

            id.startsWith("piped:") -> providers.filterIsInstance<PipedMusicProvider>()
                .firstOrNull()

            id.startsWith("spotify:") || !id.contains(':') ->
                providers.filterIsInstance<SpotifyMusicProvider>().firstOrNull()

            else -> null
        }
        if (provider != null) return runWithBreaker(provider) { it.getTrack(id) }

        for (p in providers) {
            val track = runWithBreaker(p) { it.getTrack(id) }
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