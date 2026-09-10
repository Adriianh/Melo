package com.github.adriianh.core.domain.usecase.playback

import com.github.adriianh.core.domain.model.HistoryEntry
import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.HistoryRepository
import com.github.adriianh.core.platform.currentTimeSeconds
import kotlinx.coroutines.flow.Flow

class GetRecentTracksUseCase(private val repository: HistoryRepository) {
    operator fun invoke(limit: Int = 20): Flow<List<HistoryEntry>> =
        repository.getRecentTracks(limit)
}

class RecordPlayUseCase(private val repository: HistoryRepository) {
    suspend operator fun invoke(track: Track, playedAt: Long = currentTimeSeconds() * 1000L) {
        repository.recordPlay(HistoryEntry(track = track, playedAt = playedAt))
        repository.pruneOldHistory()
    }
}