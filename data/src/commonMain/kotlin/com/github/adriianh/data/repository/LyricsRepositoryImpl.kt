package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.repository.LyricsRepository
import com.github.adriianh.data.remote.lyrics.LyricsApiClient

class LyricsRepositoryImpl(
    val lyricsApiClient: LyricsApiClient
) : LyricsRepository {

    override suspend fun getLyrics(artist: String, title: String): String? {
        val raw = lyricsApiClient.getLyricsResponse(artist, title)?.plainLyrics ?: return null
        return if (raw.startsWith("Paroles") || raw.startsWith("Lyrics of")) {
            val firstNewline = raw.indexOf('\n')
            if (firstNewline != -1) raw.substring(firstNewline).trimStart() else raw
        } else {
            raw
        }
    }

    override suspend fun getSyncedLyrics(artist: String, title: String): String? =
        lyricsApiClient.getLyricsResponse(artist, title)?.syncedLyrics
}