package com.github.adriianh.data.remote.piped

import com.github.adriianh.core.domain.model.Track
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.utils.io.CancellationException
import kotlin.math.abs
import kotlinx.coroutines.sync.withLock

class PipedApiClient(
    private val httpClient: HttpClient
) {
    companion object {
        private val PARENS_RE = Regex("""\s*\(.*?\)""")
        private val BRACKETS_RE = Regex("""\s*\[.*?]""")
        private val NON_ALNUM_RE = Regex("[^a-z0-9 ]")
        private val initialInstances = listOf(
            "https://api.piped.private.coffee",
            "https://pipedapi.kavin.rocks",
            "https://api.piped.privacydev.net",
            "https://pipedapi.tokhmi.xyz",
            "https://piped-api.garudalinux.org",
            "https://pipedapi.adminforge.de",
            "https://pipedapi.ducks.party",
            "https://pipedapi.drgns.space",
            "https://pipedapi.leptons.xyz"
        )
    }

    private val instanceOrder = initialInstances.toMutableList()
    private val instanceLock = kotlinx.coroutines.sync.Mutex()

    private suspend fun currentInstances(): List<String> =
        instanceLock.withLock { instanceOrder.toList() }

    private suspend fun promoteInstance(baseUrl: String) {
        instanceLock.withLock {
            val index = instanceOrder.indexOf(baseUrl)
            if (index > 0) {
                instanceOrder.add(0, instanceOrder.removeAt(index))
            }
        }
    }

    private fun HttpRequestBuilder.commonHeaders() {
        header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        )
    }

    suspend fun search(query: String, title: String, artist: String, durationMs: Long): String? {
        for (baseUrl in currentInstances()) {
            try {
                val cleanTitle = normalize(title)
                val cleanArtist = normalize(artist)
                val response = httpClient.get("$baseUrl/search") {
                    commonHeaders()
                    parameter("q", query)
                    parameter("filter", "music_songs")
                }.body<PipedSearchResponse>()

                val best = response.items
                    .filter { it.type == "stream" }
                    .firstOrNull { item ->
                        val itemTitle = normalize(item.title)
                        val itemUploader = normalize(item.uploaderName)
                        val titleMatches =
                            itemTitle.contains(cleanTitle) || cleanTitle.contains(itemTitle)
                        val artistMatches =
                            itemUploader.contains(cleanArtist) || cleanArtist.contains(itemUploader)
                        val durationMatches = durationMs == 0L ||
                                abs(item.duration * 1000 - durationMs) < 10_000
                        titleMatches && artistMatches && durationMatches
                    }

                if (best != null) {
                    promoteInstance(baseUrl)
                    return best.url.substringAfter("v=")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
        }
        return null
    }

    suspend fun getStreamUrl(videoId: String): String? {
        for (baseUrl in currentInstances()) {
            try {
                val response = httpClient.get("$baseUrl/streams/$videoId") {
                    commonHeaders()
                }
                if (response.status.value in 200..299) {
                    val body = response.body<PipedStreamsResponse>()
                    val url = body.audioStreams.maxByOrNull { it.bitrate }?.url
                    if (url != null) {
                        promoteInstance(baseUrl)
                        return url
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
        }
        return null
    }

    suspend fun getTrackDetails(videoId: String): Track? {
        for (baseUrl in currentInstances()) {
            try {
                val response = httpClient.get("$baseUrl/streams/$videoId") {
                    commonHeaders()
                }
                if (response.status.value in 200..299) {
                    val body = response.body<PipedStreamsResponse>()
                    if (body.title.isBlank()) continue
                    promoteInstance(baseUrl)
                    return Track(
                        id = "piped:$videoId",
                        title = body.title,
                        artist = body.uploader,
                        album = "",
                        durationMs = body.duration * 1_000L,
                        genres = emptyList(),
                        artworkUrl = null,
                        sourceId = videoId
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    suspend fun searchTracks(query: String, limit: Int = 20): List<Track> {
        return searchEntities(query, "music_songs", "stream", limit).map { it.toDomain() }
    }

    suspend fun searchAlbums(query: String, limit: Int = 20): List<PipedStreamDto> {
        return searchEntities(query, "music_albums", "playlist", limit)
    }

    suspend fun searchArtists(query: String, limit: Int = 20): List<PipedStreamDto> {
        return searchEntities(query, "music_artists", "channel", limit)
    }

    suspend fun searchPlaylists(query: String, limit: Int = 20): List<PipedStreamDto> {
        return searchEntities(query, "music_playlists", "playlist", limit)
    }

    private suspend fun searchEntities(
        query: String,
        filter: String,
        expectedType: String,
        limit: Int
    ): List<PipedStreamDto> {
        for (baseUrl in currentInstances()) {
            try {
                val response = httpClient.get("$baseUrl/search") {
                    commonHeaders()
                    parameter("q", query)
                    parameter("filter", filter)
                }.body<PipedSearchResponse>()

                promoteInstance(baseUrl)
                return response.items
                    .filter { it.type == expectedType }
                    .take(limit)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                continue
            }
        }
        return emptyList()
    }

    suspend fun getRelatedTracks(
        videoId: String,
        limit: Int = 20,
        fallbackArtist: String? = null,
        fallbackTitle: String? = null,
    ): List<Track> {
        val seen = mutableSetOf(videoId)
        val results = mutableListOf<Track>()

        for (baseUrl in currentInstances()) {
            try {
                val response = httpClient.get("$baseUrl/streams/$videoId") {
                    commonHeaders()
                }
                if (response.status.value !in 200..299) continue

                val body = response.body<PipedStreamsResponse>()

                val normalizedSeedTitle = fallbackTitle?.let { normalize(it) } ?: ""
                val normalizedSeedArtist = fallbackArtist?.let { normalize(it) } ?: ""

                val related = body.relatedStreams
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
                        val sameTitle = normalize(track.title) == normalizedSeedTitle
                        val sameArtist = normalize(track.artist) == normalizedSeedArtist
                        if (sameTitle && sameArtist) 1 else 0
                    }

                results += related
                if (results.isNotEmpty()) {
                    promoteInstance(baseUrl)
                    break
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }

        if (results.size >= limit) return results.take(limit)

        val artist = fallbackArtist ?: return results.take(limit)
        val title = fallbackTitle ?: ""

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