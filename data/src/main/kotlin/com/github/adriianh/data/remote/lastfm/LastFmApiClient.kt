package com.github.adriianh.data.remote.lastfm

import com.github.adriianh.core.domain.model.SimilarTrack
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.CancellationException
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.SortedMap

private const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"

class LastFmApiClient(
    private val httpClient: HttpClient,
    internal val apiKey: String,
    private val sharedSecret: String = "",
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getTopTags(artist: String, limit: Int = 5): List<String> {
        return try {
            val response = httpClient.get(BASE_URL) {
                parameter("method", "artist.getTopTags")
                parameter("artist", artist)
                parameter("api_key", apiKey)
                parameter("format", "json")
                parameter("limit", limit)
            }.body<LastFmTopTagsResponse>()
            response.toptags?.tag?.map { it.name } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getSimilarTracks(artist: String, title: String, limit: Int = 10): List<SimilarTrack> {
        return try {
            val response = httpClient.get(BASE_URL) {
                parameter("method", "track.getSimilar")
                parameter("track", title)
                parameter("artist", artist)
                parameter("api_key", apiKey)
                parameter("format", "json")
                parameter("limit", limit)
            }.body<LastFmSimilarTracksResponse>()
            response.similartracks.track.map {
                SimilarTrack(title = it.name, artist = it.artist.name, match = it.match)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Authenticate with Last.fm and return a session key for the given credentials. */
    suspend fun getMobileSession(username: String, password: String): String? {
        return try {
            val params = sortedMapOf(
                "api_key"  to apiKey,
                "method"   to "auth.getMobileSession",
                "password" to password,
                "username" to username,
            )
            val sig = sign(params)
            val body = post(params + mapOf("api_sig" to sig, "format" to "json")).bodyAsText()
            json.decodeFromString<LastFmSessionResponse>(body).session.key
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Request a temporary token to begin the web authentication flow.
     * The token must then be authorized by the user via the Last.fm website.
     * Uses GET as required by the Last.fm auth.getToken spec.
     */
    suspend fun getToken(): String? {
        return try {
            val params = sortedMapOf(
                "api_key" to apiKey,
                "method"  to "auth.getToken",
            )
            val sig = sign(params)
            val body = httpClient.get(BASE_URL) {
                parameter("method", "auth.getToken")
                parameter("api_key", apiKey)
                parameter("api_sig", sig)
                parameter("format", "json")
            }.bodyAsText()
            json.decodeFromString<LastFmTokenResponse>(body).token
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Exchange a user-authorized token for a permanent session key.
     * Must be called after the user has approved the token via the browser.
     */
    suspend fun getSession(token: String): String? {
        return try {
            val params = sortedMapOf(
                "api_key" to apiKey,
                "method"  to "auth.getSession",
                "token"   to token,
            )
            val sig = sign(params)
            val body = post(params + mapOf("api_sig" to sig, "format" to "json")).bodyAsText()
            json.decodeFromString<LastFmSessionResponse>(body).session.key
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /** Notify Last.fm of the currently playing track (no scrobble recorded). */
    suspend fun updateNowPlaying(
        sessionKey: String,
        artist: String,
        title: String,
        album: String,
        durationSecs: Int,
    ) {
        try {
            val params = sortedMapOf(
                "api_key"  to apiKey,
                "artist"   to artist,
                "duration" to durationSecs.toString(),
                "method"   to "track.updateNowPlaying",
                "sk"       to sessionKey,
                "track"    to title,
            ).also { if (album.isNotBlank()) it["album"] = album }
            val sig = sign(params)
            post(params + mapOf("api_sig" to sig, "format" to "json"))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) { }
    }

    /** Scrobble a track to the user's Last.fm profile. */
    suspend fun scrobble(
        sessionKey: String,
        artist: String,
        title: String,
        album: String,
        timestamp: Long,
    ) {
        try {
            val params = sortedMapOf(
                "api_key"      to apiKey,
                "artist[0]"    to artist,
                "method"       to "track.scrobble",
                "sk"           to sessionKey,
                "timestamp[0]" to timestamp.toString(),
                "track[0]"     to title,
            ).also { if (album.isNotBlank()) it["album[0]"] = album }
            val sig = sign(params)
            post(params + mapOf("api_sig" to sig, "format" to "json"))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) { }
    }


    /**
     * POST to the Last.fm API with parameters as query string.
     * Last.fm expects params in the URL query string even for POST requests,
     * with an empty body
     */
    private suspend fun post(params: Map<String, String>): HttpResponse =
        httpClient.post(BASE_URL) {
            params.forEach { (k, v) -> parameter(k, v) }
            setBody("")
        }

    /**
     * Builds the Last.fm API signature: concatenate sorted key=value pairs
     * (excluding format and callback), append the shared secret, then MD5.
     */
    private fun sign(params: SortedMap<String, String>): String {
        val base = params.entries.joinToString("") { (k, v) -> "$k$v" } + sharedSecret
        return MessageDigest.getInstance("MD5")
            .digest(base.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}