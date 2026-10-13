package com.github.adriianh.data.repository

import com.github.adriianh.core.domain.model.TrackLyrics
import com.github.adriianh.core.domain.repository.LyricsRepository
import com.github.adriianh.core.platform.PlatformFileSystem
import com.github.adriianh.core.util.LrcParser
import com.github.adriianh.data.remote.lyrics.LyricsApiClient
import com.github.adriianh.data.remote.lyrics.LyricsTranslator
import kotlinx.serialization.json.Json

class LyricsRepositoryImpl(
    val lyricsApiClient: LyricsApiClient,
    val lyricsTranslator: LyricsTranslator? = null,
    private val configDirPath: String? = null,
) : LyricsRepository {
    private val lyricsCache = mutableMapOf<String, TrackLyrics>()
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    override suspend fun getLyrics(
        artist: String,
        title: String,
    ): String? {
        val cacheKey = "$artist - $title".lowercase()
        val cached = lyricsCache[cacheKey] ?: loadFromDisk(cacheKey)?.also { lyricsCache[cacheKey] = it }
        if (cached?.plainLyrics != null) return cached.plainLyrics

        val raw = lyricsApiClient.getLyricsResponse(artist, title)?.plainLyrics ?: return null
        val sanitized = sanitizePlainLyrics(raw)
        val entry = (cached ?: TrackLyrics()).copy(plainLyrics = sanitized)
        lyricsCache[cacheKey] = entry
        saveToDisk(cacheKey, entry)

        return sanitized
    }

    override suspend fun getSyncedLyrics(
        artist: String,
        title: String,
    ): String? {
        val cacheKey = "$artist - $title".lowercase()
        val cached = lyricsCache[cacheKey] ?: loadFromDisk(cacheKey)?.also { lyricsCache[cacheKey] = it }
        if (cached?.syncedLyrics?.isNotEmpty() == true) {
            return cached.syncedLyrics.joinToString("\n") { "[${it.timeMs}] ${it.text}" }
        }

        val response = lyricsApiClient.getLyricsResponse(artist, title)
        if (response?.syncedLyrics != null) {
            val synced = (LrcParser).parse(response.syncedLyrics)
            val entry =
                (cached ?: TrackLyrics()).copy(
                    syncedLyrics = synced,
                    hasSync = synced.isNotEmpty(),
                )
            lyricsCache[cacheKey] = entry
            saveToDisk(cacheKey, entry)
        }

        return response?.syncedLyrics
    }

    override suspend fun getTrackLyrics(
        artist: String,
        title: String,
    ): TrackLyrics {
        val cacheKey = "$artist - $title".lowercase()
        lyricsCache[cacheKey]?.let { return it }

        loadFromDisk(cacheKey)?.let {
            lyricsCache[cacheKey] = it
            return it
        }

        val response =
            lyricsApiClient.getLyricsResponse(artist, title)
                ?: return TrackLyrics(error = "No lyrics found")
        val plain = response.plainLyrics?.let { sanitizePlainLyrics(it) }
        val synced = LrcParser.parse(response.syncedLyrics)

        val result =
            TrackLyrics(
                plainLyrics = plain,
                syncedLyrics = synced,
                hasSync = synced.isNotEmpty(),
            )
        lyricsCache[cacheKey] = result
        saveToDisk(cacheKey, result)
        return result
    }

    override suspend fun translateLyrics(
        trackId: String,
        lyrics: TrackLyrics,
        targetLanguage: String,
    ): TrackLyrics {
        val translator = lyricsTranslator ?: return lyrics
        val plain = lyrics.plainLyrics
        val translated =
            if (lyrics.hasSync) {
                val translatedLines =
                    translator.translateSyncedLyrics(trackId, lyrics.syncedLyrics, targetLanguage)
                lyrics.copy(
                    syncedLyrics = translatedLines,
                    targetLanguage = targetLanguage,
                    isTranslating = false,
                )
            } else if (!plain.isNullOrBlank()) {
                val translatedPlain = translator.translatePlainLyrics(plain, targetLanguage)
                lyrics.copy(
                    plainLyrics = translatedPlain,
                    targetLanguage = targetLanguage,
                    isTranslating = false,
                )
            } else {
                lyrics.copy(targetLanguage = targetLanguage, isTranslating = false)
            }

        return translated
    }

    private fun sanitizePlainLyrics(raw: String): String =
        if (raw.startsWith("Paroles") || raw.startsWith("Lyrics of")) {
            val firstNewline = raw.indexOf('\n')
            if (firstNewline != -1) raw.substring(firstNewline).trimStart() else raw
        } else {
            raw
        }

    private fun loadFromDisk(cacheKey: String): TrackLyrics? {
        val baseDir = configDirPath ?: return null
        val filePath = "$baseDir/lyrics/lyrics_${cacheKey.hashCode()}.json"

        return try {
            val content = PlatformFileSystem.readText(filePath)

            if (content != null) {
                json.decodeFromString(TrackLyrics.serializer(), content)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun saveToDisk(
        cacheKey: String,
        lyrics: TrackLyrics,
    ) {
        val baseDir = configDirPath
        if (baseDir == null || lyrics.plainLyrics == null && lyrics.syncedLyrics.isEmpty()) {
            return
        }

        try {
            val folder = "$baseDir/lyrics"
            PlatformFileSystem.makeDirs(folder)

            val filePath = "$folder/lyrics_${cacheKey.hashCode()}.json"
            PlatformFileSystem.writeText(filePath, json.encodeToString(lyrics))
        } catch (_: Exception) {
        }
    }
}
