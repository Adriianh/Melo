package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.repository.OfflineRepository

class SyncOfflineTracksUseCase(
    private val offlineRepository: OfflineRepository
) {
    suspend operator fun invoke() {
        offlineRepository.syncWithFileSystem()
    }
}
