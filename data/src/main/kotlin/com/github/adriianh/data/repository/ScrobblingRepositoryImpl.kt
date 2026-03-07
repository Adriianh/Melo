package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.ScrobblingRepository
import com.github.adriianh.data.remote.lastfm.LastFmApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val AUTH_URL_BASE = "https://www.last.fm/api/auth/"
private const val KEY_SESSION = "LASTFM_SESSION_KEY"
private const val KEY_TOKEN = "LASTFM_PENDING_TOKEN"

class ScrobblingRepositoryImpl(
    private val client: LastFmApiClient,
    private val configDir: String,
) : ScrobblingRepository {

    private val envFile get() = File("$configDir/.env")

    override fun getSessionKey(): String? = readEnvKey(KEY_SESSION)

    override suspend fun authenticate(username: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            val key = client.getMobileSession(username, password) ?: return@withContext false
            writeEnvKey(KEY_SESSION, key)
            true
        }

    override suspend fun startWebAuth(): String? =
        withContext(Dispatchers.IO) {
            val token = client.getToken() ?: return@withContext null
            writeEnvKey(KEY_TOKEN, token)
            "$AUTH_URL_BASE?api_key=${client.apiKey}&token=$token"
        }

    override suspend fun completeWebAuth(token: String): Boolean =
        withContext(Dispatchers.IO) {
            val sessionKey = client.getSession(token) ?: return@withContext false
            writeEnvKey(KEY_SESSION, sessionKey)
            removeEnvKey(KEY_TOKEN)
            true
        }

    /** Returns the pending token stored by [startWebAuth], if any. */
    fun getPendingToken(): String? = readEnvKey(KEY_TOKEN)

    override suspend fun updateNowPlaying(track: Track) {
        val key = getSessionKey() ?: return
        client.updateNowPlaying(
            sessionKey   = key,
            artist       = track.artist,
            title        = track.title,
            album        = track.album,
            durationSecs = (track.durationMs / 1000).toInt(),
        )
    }

    override suspend fun scrobble(track: Track, startedAt: Long) {
        val key = getSessionKey() ?: return
        client.scrobble(
            sessionKey = key,
            artist     = track.artist,
            title      = track.title,
            album      = track.album,
            timestamp  = startedAt / 1000,
        )
    }

    private fun readEnvKey(key: String): String? {
        if (!envFile.exists()) return null
        return envFile.readLines()
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.takeIf { it.isNotBlank() }
    }

    private fun writeEnvKey(key: String, value: String) {
        envFile.parentFile?.mkdirs()
        val lines = if (envFile.exists()) envFile.readLines() else emptyList()
        val updated = if (lines.any { it.startsWith("$key=") }) {
            lines.map { if (it.startsWith("$key=")) "$key=$value" else it }
        } else {
            lines + "$key=$value"
        }
        envFile.writeText(updated.joinToString("\n") + "\n")
    }

    private fun removeEnvKey(key: String) {
        if (!envFile.exists()) return
        val updated = envFile.readLines().filter { !it.startsWith("$key=") }
        envFile.writeText(updated.joinToString("\n") + "\n")
    }
}