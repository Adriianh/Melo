package com.github.adriianh.core.domain.usecase.lyrics

import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.core.domain.repository.LyricsRepository

class TranslateLyricsUseCase(
    private val lyricsRepository: LyricsRepository
) {
    suspend operator fun invoke(
        trackId: String,
        lyrics: TrackLyrics,
        targetLanguage: String = "es"
    ): TrackLyrics {
        return lyricsRepository.translateLyrics(trackId, lyrics, targetLanguage)
    }
}