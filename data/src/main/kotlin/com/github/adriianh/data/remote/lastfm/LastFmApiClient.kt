package com.github.adriianh.data.remote.lastfm

import com.github.adriianh.core.domain.model.SimilarTrack
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import java.security.MessageDigest
import java.util.SortedMap

private const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"

class LastFmApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val sharedSecret: String = "",
) {

    suspend fun getTopTags(artist: String, limit: Int = 5): List<String> {
        return try {
            val response = httpClient.get(BASE_URL) {
                parameter("method", "artist.getTopTags")
                parameter("artist", artist)
                parameter("api_key", apiKey)
                parameter("format", "json")
                parameter("limit", limit)
            }.body<LastFmTopTagsResponse>()
            response.toptags.tag.map { it.name }
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Authenticate with Last.fm and return a session key for the given credentials. */
    suspend fun getMobileSession(username: String, password: String): String? {
        return try {
            val params = sortedMapOf(
                "api_key" to apiKey,
                "method"  to "auth.getMobileSession",
                "password" to password,
                "username" to username,
            )
            val sig = sign(params)
            httpClient.submitForm(
                url = BASE_URL,
                formParameters = parameters {
                    params.forEach { (k, v) -> append(k, v) }
                    append("api_sig", sig)
                    append("format", "json")
                }
            ).body<LastFmSessionResponse>().session.key
        } catch (e: kotlinx.coroutines.CancellationException) {
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
            httpClient.submitForm(
                url = BASE_URL,
                formParameters = parameters {
                    params.forEach { (k, v) -> append(k, v) }
                    append("api_sig", sig)
                    append("format", "json")
                }
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
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
                "api_key"   to apiKey,
                "artist[0]" to artist,
                "method"    to "track.scrobble",
                "sk"        to sessionKey,
                "timestamp[0]" to timestamp.toString(),
                "track[0]"  to title,
            ).also { if (album.isNotBlank()) it["album[0]"] = album }
            val sig = sign(params)
            httpClient.submitForm(
                url = BASE_URL,
                formParameters = parameters {
                    params.forEach { (k, v) -> append(k, v) }
                    append("api_sig", sig)
                    append("format", "json")
                }
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) { }
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