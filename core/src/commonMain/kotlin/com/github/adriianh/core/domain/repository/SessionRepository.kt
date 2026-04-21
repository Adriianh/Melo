package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.Track

data class SavedSession(
    val queue: List<Track>,
    val queueIndex: Int,
    val positionMs: Long,
)

interface SessionRepository {
    suspend fun saveSession(session: SavedSession)
    suspend fun restoreSession(): SavedSession?
    suspend fun clearSession()
}