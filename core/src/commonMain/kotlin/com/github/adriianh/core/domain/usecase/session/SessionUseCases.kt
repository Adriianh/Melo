package com.github.adriianh.core.domain.usecase.session

import com.github.adriianh.core.domain.repository.SavedSession
import com.github.adriianh.core.domain.repository.SessionRepository

class SaveSessionUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke(session: SavedSession) = repository.saveSession(session)
}

class RestoreSessionUseCase(
    private val repository: SessionRepository,
) {
    /**
     * Restores this instance's session, or the most recently saved one across instances
     * when [mostRecent] is enabled by the "resume last active session" setting.
     */
    suspend operator fun invoke(mostRecent: Boolean = false): SavedSession? =
        if (mostRecent) repository.restoreMostRecentSession() else repository.restoreSession()
}

class ClearSessionUseCase(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke() = repository.clearSession()
}
