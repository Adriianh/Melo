package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<Settings> = repository.getSettingsFlow()
    suspend fun getSnapshot(): Settings = repository.getSettings()
}