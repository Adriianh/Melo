package com.github.adriianh.core.domain.usecase.search

import com.github.adriianh.core.domain.repository.LyricsRepository

class GetLyricsUseCase(
    private val repository: LyricsRepository
) {
    suspend operator fun invoke(artist: String, title: String): String? {
        if (artist.isBlank() || title.isBlank()) return null
        return repository.getLyrics(artist, title)
    }
}