package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.SavedSession
import com.github.adriianh.core.domain.repository.SessionRepository
import com.github.adriianh.data.local.MeloDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionRepositoryImpl(database: MeloDatabase) : SessionRepository {

    private val queries = database.sessionQueries
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: SavedSession) = withContext(Dispatchers.IO) {
        if (session.queue.isEmpty() || session.queueIndex < 0) return@withContext
        queries.upsertSession(
            queue_json  = json.encodeToString(session.queue.map { it.toDto() }),
            queue_index = session.queueIndex.toLong(),
            position_ms = session.positionMs,
            saved_at    = System.currentTimeMillis(),
        )
    }

    override suspend fun restoreSession(): SavedSession? = withContext(Dispatchers.IO) {
        val row = queries.selectSession().executeAsOneOrNull() ?: return@withContext null
        val queue = runCatching {
            json.decodeFromString<List<TrackDto>>(row.queue_json).map { it.toTrack() }
        }.getOrDefault(emptyList())
        if (queue.isEmpty()) return@withContext null
        SavedSession(
            queue      = queue,
            queueIndex = row.queue_index.toInt(),
            positionMs = row.position_ms,
        )
    }

    override suspend fun clearSession() = withContext(Dispatchers.IO) {
        queries.deleteSession()
    }

    @Serializable
    private data class TrackDto(
        val id: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val genres: List<String>,
        val artworkUrl: String? = null,
        val sourceId: String? = null,
    )

    private fun Track.toDto() = TrackDto(id, title, artist, album, durationMs, genres, artworkUrl, sourceId)
    private fun TrackDto.toTrack() = Track(id, title, artist, album, durationMs, genres, artworkUrl, sourceId)
}
