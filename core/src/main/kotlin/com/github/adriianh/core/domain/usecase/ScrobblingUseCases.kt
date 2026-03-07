package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.ScrobblingRepository

class UpdateNowPlayingUseCase(private val repository: ScrobblingRepository) {
    suspend operator fun invoke(track: Track) = repository.updateNowPlaying(track)
}

class ScrobbleUseCase(private val repository: ScrobblingRepository) {
    suspend operator fun invoke(track: Track, startedAt: Long) =
        repository.scrobble(track, startedAt)
}

class AuthenticateLastFmUseCase(private val repository: ScrobblingRepository) {
    suspend operator fun invoke(username: String, password: String): Boolean =
        repository.authenticate(username, password)
}

/** Starts the web auth flow and returns the URL to open in the browser, or null on failure. */
class StartWebAuthUseCase(private val repository: ScrobblingRepository) {
    suspend operator fun invoke(): String? = repository.startWebAuth()
}

/** Completes the web auth flow by exchanging the token for a session key. Returns true on success. */
class CompleteWebAuthUseCase(private val repository: ScrobblingRepository) {
    suspend operator fun invoke(token: String): Boolean = repository.completeWebAuth(token)
}