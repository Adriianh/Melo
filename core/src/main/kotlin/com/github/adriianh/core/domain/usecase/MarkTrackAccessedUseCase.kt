package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.repository.OfflineRepository

class MarkTrackAccessedUseCase(private val repository: OfflineRepository) {
    suspend operator fun invoke(trackId: String) {
        repository.markTrackAsAccessed(trackId)
    }
}