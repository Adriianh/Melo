package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.model.Settings
import com.github.adriianh.core.domain.repository.SettingsRepository

class UpdateSettingsUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(settings: Settings) {
        repository.updateSettings(settings)
    }

    suspend operator fun invoke(update: (Settings) -> Settings) {
        val current = repository.getSettings()
        repository.updateSettings(update(current))
    }
}