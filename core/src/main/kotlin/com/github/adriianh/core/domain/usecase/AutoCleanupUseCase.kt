package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.repository.OfflineRepository

class AutoCleanupUseCase(private val repository: OfflineRepository) {
    suspend operator fun invoke(maxSizeMb: Int) {
        repository.cleanupCache(maxSizeMb)
    }
}