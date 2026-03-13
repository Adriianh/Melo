package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.repository.OfflineRepository

class AutoCleanupUseCase(private val repository: OfflineRepository) {
    suspend operator fun invoke(maxAgeDays: Int, maxSizeMb: Int) {
        repository.cleanupExpired(maxAgeDays)
        repository.cleanupCache(maxSizeMb)
    }
}