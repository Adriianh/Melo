package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.repository.OfflineRepository
import kotlinx.coroutines.flow.Flow

class GetOfflineTracksUseCase(private val repository: OfflineRepository) {
    operator fun invoke(): Flow<List<OfflineTrack>> = repository.getOfflineTracksFlow()
    suspend fun getSnapshot(): List<OfflineTrack> = repository.getOfflineTracks()
}