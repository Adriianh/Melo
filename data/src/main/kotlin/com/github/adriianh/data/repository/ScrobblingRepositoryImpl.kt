package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.ScrobblingRepository
import com.github.adriianh.data.remote.lastfm.LastFmApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ScrobblingRepositoryImpl(
    private val client: LastFmApiClient,
    private val configDir: String,
) : ScrobblingRepository {

    private val envFile get() = File("$configDir/.env")

    override fun getSessionKey(): String? =
        readEnvKey("LASTFM_SESSION_KEY")

    override suspend fun authenticate(username: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            val key = client.getMobileSession(username, password) ?: return@withContext false
            writeEnvKey("LASTFM_SESSION_KEY", key)
            true
        }

    override suspend fun updateNowPlaying(track: Track) {
        val key = getSessionKey() ?: return
        client.updateNowPlaying(
            sessionKey  = key,
            artist      = track.artist,
            title       = track.title,
            album       = track.album,
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
}