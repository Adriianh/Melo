package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.repository.OfflineRepository

class DeleteDownloadedTrackUseCase(private val repository: OfflineRepository) {
    suspend operator fun invoke(trackId: String) {
        repository.removeOfflineTrack(trackId)
    }
}