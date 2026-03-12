package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.model.OfflineTrack
import com.github.adriianh.core.domain.repository.OfflineRepository

class DownloadTrackUseCase(private val repository: OfflineRepository) {
    suspend operator fun invoke(offlineTrack: OfflineTrack) {
        repository.saveOfflineTrack(offlineTrack)
    }
}