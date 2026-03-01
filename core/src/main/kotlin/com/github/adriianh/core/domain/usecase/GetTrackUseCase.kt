package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.repository.MusicRepository

class GetTrackUseCase(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(id: String): Track? {
        return repository.getTrack(id)
    }
}