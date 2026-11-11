package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.SavedSession
import com.github.adriianh.core.domain.repository.SessionRepository
import com.github.adriianh.core.platform.currentTimeSeconds
import com.github.adriianh.core.util.MeloDispatchers
import com.github.adriianh.data.local.MeloDatabase
import com.github.adriianh.data.local.Session
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SQLite-backed playback session store.
 *
 * Each process owns its own row, keyed by [instanceKey] (the same identity used for the
 * MPRIS player name, e.g. `gui`, `tui` or `gui.instance<PID>`). Keeping sessions scoped
 * per instance prevents concurrent Melo processes from overwriting or clearing each
 * other's queue.
 */
class SessionRepositoryImpl(
    database: MeloDatabase,
    private val instanceKey: String = DEFAULT_INSTANCE_KEY,
) : SessionRepository {
    private val queries = database.sessionQueries
    private val json = Json { ignoreUnknownKeys = true }

    /** Role derived from the instance key (`gui.instance123` -> `gui`). */
    private val role: String = instanceKey.substringBefore('.')

    override suspend fun saveSession(session: SavedSession) =
        withContext(MeloDispatchers.IO) {
            if (session.queue.isEmpty() || session.queueIndex < 0) return@withContext
            queries.upsertSession(
                instance_key = instanceKey,
                role = role,
                queue_json = json.encodeToString(session.queue.map { it.toDto() }),
                queue_index = session.queueIndex.toLong(),
                position_ms = session.positionMs,
                saved_at = currentTimeSeconds() * 1000L,
                pid = null,
                host = null,
            )
        }

    override suspend fun restoreSession(): SavedSession? =
        withContext(MeloDispatchers.IO) {
            cleanupStaleSessions()
            queries.selectSession(instanceKey).executeAsOneOrNull()?.toSavedSession()
        }

    override suspend fun restoreMostRecentSession(): SavedSession? =
        withContext(MeloDispatchers.IO) {
            cleanupStaleSessions()
            queries.selectMostRecentSession().executeAsOneOrNull()?.toSavedSession()
        }

    override suspend fun clearSession() {
        withContext(MeloDispatchers.IO) {
            queries.deleteSession(instanceKey)
        }
    }

    /**
     * Drops rows from other instances that have not been saved for [STALE_SESSION_TTL_MS].
     *
     * Secondary instances are keyed by PID, so every extra process leaves a row behind.
     * The current row is never touched, and a long TTL keeps a paused-but-alive instance
     * from losing its session.
     */
    private fun cleanupStaleSessions() {
        val threshold = currentTimeSeconds() * 1000L - STALE_SESSION_TTL_MS
        queries.deleteStaleSessions(instanceKey, threshold)
    }

    private fun Session.toSavedSession(): SavedSession? {
        val queue =
            runCatching {
                json.decodeFromString<List<TrackDto>>(queue_json).map { it.toTrack() }
            }.getOrDefault(emptyList())
        if (queue.isEmpty()) return null
        return SavedSession(
            queue = queue,
            queueIndex = queue_index.toInt(),
            positionMs = position_ms,
        )
    }

    companion object {
        const val DEFAULT_INSTANCE_KEY: String = "gui"

        /** Rows from other instances older than 30 days are garbage-collected on startup. */
        internal const val STALE_SESSION_TTL_MS: Long = 30L * 24 * 60 * 60 * 1000
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
