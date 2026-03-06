package com.github.adriianh.core.domain.usecase

import com.github.adriianh.core.domain.repository.LyricsRepository

class GetSyncedLyricsUseCase(
    private val repository: LyricsRepository
) {
    suspend operator fun invoke(artist: String, title: String): String? {
        if (artist.isBlank() || title.isBlank()) return null
        return repository.getSyncedLyrics(artist, title)
    }
}