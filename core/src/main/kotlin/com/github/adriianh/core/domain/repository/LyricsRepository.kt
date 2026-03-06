package com.github.adriianh.core.domain.repository

interface LyricsRepository {
    suspend fun getLyrics(artist: String, title: String): String?
    suspend fun getSyncedLyrics(artist: String, title: String): String?
}