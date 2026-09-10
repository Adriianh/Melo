package com.github.adriianh.core.domain.repository

import com.github.adriianh.core.domain.model.TrackLyrics

interface LyricsRepository {
    suspend fun getLyrics(artist: String, title: String): String?
    suspend fun getSyncedLyrics(artist: String, title: String): String?
    suspend fun getTrackLyrics(artist: String, title: String): TrackLyrics
    suspend fun translateLyrics(
        trackId: String,
        lyrics: TrackLyrics,
        targetLanguage: String
    ): TrackLyrics
}