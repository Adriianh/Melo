package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.Track

data class SavedSession(
    val queue: List<Track>,
    val queueIndex: Int,
    val positionMs: Long,
)

interface SessionRepository {
    suspend fun saveSession(session: SavedSession)

    /** Restores the row owned by this instance, if any. */
    suspend fun restoreSession(): SavedSession?

    /** Restores the most recently saved row across all instances (opt-in multi-instance resume). */
    suspend fun restoreMostRecentSession(): SavedSession?

    suspend fun clearSession()
}
