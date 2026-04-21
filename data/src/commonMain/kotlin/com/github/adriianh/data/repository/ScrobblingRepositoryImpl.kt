
package com.github.adriianh.data.repository
import com.github.adriianh.core.util.MeloDispatchers

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.ScrobblingRepository
import com.github.adriianh.data.remote.lastfm.LastFmApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.github.adriianh.core.platform.PlatformFileSystem

private const val AUTH_URL_BASE = "https://www.last.fm/api/auth/"
private const val KEY_SESSION = "LASTFM_SESSION_KEY"
private const val KEY_TOKEN = "LASTFM_PENDING_TOKEN"

class ScrobblingRepositoryImpl(
    private val client: LastFmApiClient,
    private val configDir: String,
) : ScrobblingRepository {

    private val envFilePath get() = "$configDir/.env"

    override fun getSessionKey(): String? = readEnvKey(KEY_SESSION)

    override suspend fun authenticate(username: String, password: String): Boolean =
        withContext(MeloDispatchers.IO) {
            val key = client.getMobileSession(username, password) ?: return@withContext false
            writeEnvKey(KEY_SESSION, key)
            true
        }

    override suspend fun startWebAuth(): String? =
        withContext(MeloDispatchers.IO) {
            val token = client.getToken() ?: return@withContext null
            writeEnvKey(KEY_TOKEN, token)
            "$AUTH_URL_BASE?api_key=${client.apiKey}&token=$token"
        }

    override suspend fun completeWebAuth(token: String): Boolean =
        withContext(MeloDispatchers.IO) {
            val sessionKey = client.getSession(token) ?: return@withContext false
            writeEnvKey(KEY_SESSION, sessionKey)
            removeEnvKey(KEY_TOKEN)
            true
        }

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

    override suspend fun logout() {
        removeEnvKey(KEY_SESSION)
    }

    @Suppress("SameParameterValue")
    private fun readEnvKey(key: String): String? {
        val content = PlatformFileSystem.readText(envFilePath) ?: return null
        return content.lines()
            .firstOrNull { it.startsWith("$key=") }
            ?.substringAfter("=")
            ?.takeIf { it.isNotBlank() }
    }

    private fun writeEnvKey(key: String, value: String) {
        val content = PlatformFileSystem.readText(envFilePath) ?: ""
        val lines = content.lines().filter { it.isNotBlank() }
        val updated = if (lines.any { it.startsWith("$key=") }) {
            lines.map { if (it.startsWith("$key=")) "$key=$value" else it }
        } else {
            lines + "$key=$value"
        }
        PlatformFileSystem.writeText(envFilePath, updated.joinToString("\n") + "\n")
    }

    private fun removeEnvKey(key: String) {
        val content = PlatformFileSystem.readText(envFilePath) ?: return
        val lines = content.lines()
        val updated = lines.filter { !it.startsWith("$key=") && it.isNotBlank() }
        PlatformFileSystem.writeText(envFilePath, updated.joinToString("\n") + "\n")
    }
}