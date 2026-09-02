package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.core.domain.repository.LyricsRepository
import com.github.adriianh.core.util.LrcParser
import com.github.adriianh.data.remote.lyrics.LyricsApiClient
import com.github.adriianh.data.remote.lyrics.LyricsTranslator

class LyricsRepositoryImpl(
    val lyricsApiClient: LyricsApiClient,
    val lyricsTranslator: LyricsTranslator? = null
) : LyricsRepository {

    private val lyricsCache = mutableMapOf<String, TrackLyrics>()

    override suspend fun getLyrics(artist: String, title: String): String? {
        val cached = lyricsCache["$artist - $title".lowercase()]
        if (cached?.plainLyrics != null) return cached.plainLyrics

        val raw = lyricsApiClient.getLyricsResponse(artist, title)?.plainLyrics ?: return null
        return sanitizePlainLyrics(raw)
    }

    override suspend fun getSyncedLyrics(artist: String, title: String): String? {
        val cached = lyricsCache["$artist - $title".lowercase()]
        if (cached?.syncedLyrics?.isNotEmpty() == true) {
            return cached.syncedLyrics.joinToString("\n") { "[${it.timeMs}] ${it.text}" }
        }
        return lyricsApiClient.getLyricsResponse(artist, title)?.syncedLyrics
    }

    override suspend fun getTrackLyrics(artist: String, title: String): TrackLyrics {
        val cacheKey = "$artist - $title".lowercase()
        lyricsCache[cacheKey]?.let { return it }

        val response = lyricsApiClient.getLyricsResponse(artist, title)
            ?: return TrackLyrics(error = "No lyrics found")
        val plain = response.plainLyrics?.let { sanitizePlainLyrics(it) }
        val synced = LrcParser.parse(response.syncedLyrics)

        val result = TrackLyrics(
            plainLyrics = plain,
            syncedLyrics = synced,
            hasSync = synced.isNotEmpty()
        )
        lyricsCache[cacheKey] = result
        return result
    }

    override suspend fun translateLyrics(
        trackId: String,
        lyrics: TrackLyrics,
        targetLanguage: String
    ): TrackLyrics {
        val translator = lyricsTranslator ?: return lyrics
        val plain = lyrics.plainLyrics
        return if (lyrics.hasSync) {
            val translatedLines =
                translator.translateSyncedLyrics(trackId, lyrics.syncedLyrics, targetLanguage)
            lyrics.copy(
                syncedLyrics = translatedLines,
                targetLanguage = targetLanguage,
                isTranslating = false
            )
        } else if (!plain.isNullOrBlank()) {
            val translatedPlain = translator.translatePlainLyrics(plain, targetLanguage)
            lyrics.copy(
                plainLyrics = translatedPlain,
                targetLanguage = targetLanguage,
                isTranslating = false
            )
        } else {
            lyrics.copy(targetLanguage = targetLanguage, isTranslating = false)
        }
    }

    private fun sanitizePlainLyrics(raw: String): String {
        return if (raw.startsWith("Paroles") || raw.startsWith("Lyrics of")) {
            val firstNewline = raw.indexOf('\n')
            if (firstNewline != -1) raw.substring(firstNewline).trimStart() else raw
        } else {
            raw
        }
    }
}