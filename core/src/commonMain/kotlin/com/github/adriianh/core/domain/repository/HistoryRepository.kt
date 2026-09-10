package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getRecentTracks(limit: Int = 20): Flow<List<HistoryEntry>>
    suspend fun recordPlay(entry: HistoryEntry)
    suspend fun pruneOldHistory()
}

