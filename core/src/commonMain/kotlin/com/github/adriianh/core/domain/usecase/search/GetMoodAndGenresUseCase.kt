package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.model.MoodAndGenreGroup
import com.github.adriianh.core.domain.repository.MusicRepository

class GetMoodAndGenresUseCase(
    private val repository: MusicRepository,
) {
    suspend operator fun invoke(): List<MoodAndGenreGroup> = repository.getMoodAndGenres()
}