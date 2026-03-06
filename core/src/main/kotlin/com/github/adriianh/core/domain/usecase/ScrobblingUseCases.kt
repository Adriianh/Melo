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