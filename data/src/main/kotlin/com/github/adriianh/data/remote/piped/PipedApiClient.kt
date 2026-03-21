package com.github.adriianh.data.remote.piped

import com.github.adriianh.core.domain.model.Track
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.utils.io.CancellationException
import kotlin.math.abs

class PipedApiClient(
    private val httpClient: HttpClient
) {
    companion object {
        private val PARENS_RE = Regex("""\s*\(.*?\)""")
        private val BRACKETS_RE = Regex("""\s*\[.*?]""")
        private val NON_ALNUM_RE = Regex("[^a-z0-9 ]")
    }

    private val baseUrl = "https://api.piped.private.coffee"

    suspend fun search(query: String, title: String, artist: String, durationMs: Long): String? {
        return try {
            val cleanTitle = normalize(title)
            val cleanArtist = normalize(artist)
            val response = httpClient.get("$baseUrl/search") {
                parameter("q", query)
                parameter("filter", "music_songs")
            }.body<PipedSearchResponse>()

            val best = response.items
                .filter { it.type == "stream" }
                .firstOrNull { item ->
                    val itemTitle = normalize(item.title)
                    val itemUploader = normalize(item.uploaderName)
                    val titleMatches = itemTitle.contains(cleanTitle) || cleanTitle.contains(itemTitle)
                    val artistMatches = itemUploader.contains(cleanArtist) || cleanArtist.contains(itemUploader)
                    val durationMatches = durationMs == 0L ||
                            abs(item.duration * 1000 - durationMs) < 10_000
                    titleMatches && artistMatches && durationMatches
                }

            best?.url?.substringAfter("v=")
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getTrackDetails(videoId: String): Track? {
        return try {
            val response = httpClient.get("$baseUrl/streams/$videoId")
                .body<PipedStreamsResponse>()
            if (response.title.isBlank()) return null
            Track(
                id = "piped:$videoId",
                title = response.title,
                artist = response.uploader,
                album = "",
                durationMs = response.duration * 1_000L,
                genres = emptyList(),
                artworkUrl = null,
                sourceId = videoId
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns a list of [Track]s from Piped's `music_songs` search, suitable for
     * use as a [com.github.adriianh.core.domain.provider.MusicProvider].
     * Duration comes from Piped in **seconds**; it is converted to milliseconds here.
     */
    suspend fun searchTracks(query: String, limit: Int = 20): List<Track> {
        return try {
            val response = httpClient.get("$baseUrl/search") {
                parameter("q", query)
                parameter("filter", "music_songs")
            }.body<PipedSearchResponse>()

            response.items
                .filter { it.type == "stream" }
                .take(limit)
                .map { it.toDomain() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Returns tracks related to [videoId] based on YouTube's recommendation algorithm.
     *
     * Strategy:
     * 1. Fetch `/streams/{videoId}` — YouTube's own related tracks graph. Best source.
     * 2. If that yields < [limit] results, supplement with search-based discovery using
     *    varied queries so the radio mode never loops the same tracks.
     *
     * Tracks from the original [videoId] and any tracks by [fallbackArtist] that
     * match the seed exactly are deprioritised to maximise variety.
     */
    suspend fun getRelatedTracks(
        videoId: String,
        limit: Int = 20,
        fallbackArtist: String? = null,
        fallbackTitle: String? = null,
    ): List<Track> {
        val seen = mutableSetOf(videoId)
        val results = mutableListOf<Track>()

        try {
            val response = httpClient.get("$baseUrl/streams/$videoId")
                .body<PipedStreamsResponse>()

            val normalizedSeedTitle  = fallbackTitle?.let  { normalize(it) } ?: ""
            val normalizedSeedArtist = fallbackArtist?.let { normalize(it) } ?: ""

            val related = response.relatedStreams
                .filter { it.type == "stream" && it.duration in 30L..600L && it.title.isNotBlank() }
                .mapNotNull { r ->
                    val id = r.url.substringAfter("v=").substringBefore("&")
                    if (id.isBlank() || id in seen) return@mapNotNull null
                    seen += id
                    Track(
                        id = "piped:$id",
                        title = r.title,
                        artist = r.uploaderName,
                        album = "",
                        durationMs = r.duration * 1_000L,
                        genres = emptyList(),
                        artworkUrl = null,
                        sourceId = id,
                    )
                }
                .sortedBy { track ->
                    val sameTitle  = normalize(track.title)  == normalizedSeedTitle
                    val sameArtist = normalize(track.artist) == normalizedSeedArtist
                    if (sameTitle && sameArtist) 1 else 0
                }

            results += related
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) { }

        if (results.size >= limit) return results.take(limit)

        val artist = fallbackArtist ?: return results.take(limit)
        val title  = fallbackTitle ?: ""

        val queries = buildList {
            if (title.isNotBlank()) add("$title $artist")
            add("$artist mix")
            add("$artist best songs")
            if (title.isNotBlank()) add("songs similar to $title")
            add("artists like $artist")
            add("$artist playlist")
            add("$artist type beat")
            add("$artist essentials")
        }.shuffled()

        for (query in queries) {
            if (results.size >= limit) break
            val needed = limit - results.size
            val batch = runCatching { searchTracks(query, needed * 2) }.getOrElse { emptyList() }
            for (track in batch) {
                val id = track.sourceId ?: continue
                if (id !in seen) {
                    seen += id
                    results += track
                }
                if (results.size >= limit) break
            }
        }

        return results.take(limit)
    }

    /**
     * Resolves a YouTube video ID for [track].
     * If the track already has a [Track.sourceId] (e.g. it came from Piped), it is returned directly.
     * Otherwise, Piped is searched by title + artist to find the matching video ID.
     */
    suspend fun resolveVideoId(track: Track): String? {
        track.sourceId?.let { return it }
        return search("${track.title} ${track.artist}", track.title, track.artist, track.durationMs)
    }

    private fun normalize(text: String): String = text
        .lowercase()
        .replace(PARENS_RE, "")
        .replace(BRACKETS_RE, "")
        .replace(NON_ALNUM_RE, "")
        .trim()
}