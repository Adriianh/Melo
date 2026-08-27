package com.github.adriianh.core.domain.usecase.lyrics

import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.core.domain.repository.LyricsRepository

class GetTrackLyricsUseCase(
    private val lyricsRepository: LyricsRepository
) {
    suspend operator fun invoke(artist: String, title: String): TrackLyrics {
        return lyricsRepository.getTrackLyrics(artist, title)
    }
}