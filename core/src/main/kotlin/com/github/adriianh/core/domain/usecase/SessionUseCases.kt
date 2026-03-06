package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.repository.SavedSession
import com.github.adriianh.core.domain.repository.SessionRepository

class SaveSessionUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke(session: SavedSession) = repository.saveSession(session)
}

class RestoreSessionUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke(): SavedSession? = repository.restoreSession()
}

class ClearSessionUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke() = repository.clearSession()
}