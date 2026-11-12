package com.github.adriianh.core.domain.usecase.login

import com.github.adriianh.core.domain.repository.LoginRepository
import com.github.adriianh.core.domain.repository.SettingsRepository

class SetActiveAccountProfileUseCase(
    private val loginRepository: LoginRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(pageId: String?) {
        loginRepository.setActivePageId(pageId)
        val current = settingsRepository.getSettings()
        if (current.accountPageId != pageId) {
            settingsRepository.updateSettings(current.copy(accountPageId = pageId))
        }
    }
}
