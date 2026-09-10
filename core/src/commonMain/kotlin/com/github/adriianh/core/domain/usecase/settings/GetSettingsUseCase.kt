package com.github.adriianh.core.domain.usecase.settings

import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class GetSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): StateFlow<Settings> = repository.getSettingsFlow()
    fun getInitial(): Settings = repository.getSettingsSync()
    suspend fun getSnapshot(): Settings = repository.getSettings()
}