package com.github.adriianh.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.HistoryRepository
import com.github.adriianh.data.local.MeloDatabase
import com.github.adriianh.data.local.Play_history
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HistoryRepositoryImpl(database: MeloDatabase) : HistoryRepository {

    private val queries = database.playHistoryQueries

    override fun getRecentTracks(limit: Int): Flow<List<HistoryEntry>> =
        queries.selectDistinctRecentTracks(limit.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toHistoryEntry() } }

    override suspend fun recordPlay(entry: HistoryEntry) = withContext(Dispatchers.IO) {
        queries.insertPlay(
            track_id = entry.track.id,
            title = entry.track.title,
            artist = entry.track.artist,
            album = entry.track.album,
            duration_ms = entry.track.durationMs,
            artwork_url = entry.track.artworkUrl,
            source_id = entry.track.sourceId,
            played_at = entry.playedAt,
        )
    }

    override suspend fun pruneOldHistory() = withContext(Dispatchers.IO) {
        queries.deleteOldHistory()
    }

    private fun Play_history.toHistoryEntry() = HistoryEntry(
        track = Track(
            id = track_id,
            title = title,
            artist = artist,
            album = album,
            durationMs = duration_ms,
            genres = emptyList(),
            artworkUrl = artwork_url,
            sourceId = source_id,
        ),
        playedAt = played_at,
    )
}
