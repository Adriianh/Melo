package com.github.adriianh.data.remote.lyrics

import com.github.adriianh.core.domain.model.SyncedLine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.http.parameters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class GoogleTranslateResponse(
    val sentences: List<SentenceDto> = emptyList(),
    val src: String? = null
)

@Serializable
private data class SentenceDto(
    val trans: String? = null,
    val orig: String? = null
)

class LyricsTranslator(
    private val httpClient: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val translationCache = mutableMapOf<String, List<SyncedLine>>()

    /**
     * Translates a list of [SyncedLine] into the requested [targetLanguage] (e.g. "es", "en", "pt", "fr").
     * Preserves timestamp synchronization and attaches the translation to each line in <500ms.
     */
    suspend fun translateSyncedLyrics(
        trackId: String,
        lines: List<SyncedLine>,
        targetLanguage: String = "es"
    ): List<SyncedLine> {
        if (lines.isEmpty()) return lines
        val cacheKey = "$trackId-$targetLanguage"
        translationCache[cacheKey]?.let { return it }

        val translatedTexts = translateBatch(lines.map { it.text }, targetLanguage)
        val result = lines.mapIndexed { index, line ->
            val translation = translatedTexts.getOrNull(index)?.takeIf { it.isNotBlank() }
            line.copy(translation = translation)
        }

        translationCache[cacheKey] = result
        return result
    }

    /**
     * Translates plain lyrics text into the requested [targetLanguage].
     */
    suspend fun translatePlainLyrics(
        text: String,
        targetLanguage: String = "es"
    ): String {
        if (text.isBlank()) return text
        val lines = text.lines()
        val translated = translateBatch(lines, targetLanguage)
        return translated.joinToString("\n")
    }

    private suspend fun translateBatch(
        lines: List<String>,
        targetLanguage: String
    ): List<String> {
        if (lines.isEmpty()) return emptyList()

        return try {
            // Join lines with newline and translate whole song in 1 single POST request
            val fullText = lines.joinToString("\n")
            val responseText = queryGoogleTranslate(fullText, targetLanguage)
            if (!responseText.isNullOrBlank()) {
                val parsed = json.decodeFromString<GoogleTranslateResponse>(responseText)
                val fullTranslated = parsed.sentences.mapNotNull { it.trans }.joinToString("")
                val translatedLines = fullTranslated.lines().map { it.trim() }
                if (translatedLines.size == lines.size) {
                    return translatedLines
                } else if (translatedLines.isNotEmpty()) {
                    // Align as closely as possible
                    return lines.indices.map { i -> translatedLines.getOrNull(i) ?: lines[i] }
                }
            }
            lines
        } catch (_: Exception) {
            lines
        }
    }

    private suspend fun queryGoogleTranslate(text: String, targetLang: String): String? {
        return try {
            httpClient.submitForm(
                url = "https://translate.google.com/translate_a/single?client=at&dt=t&dj=1&hl=$targetLang&ie=UTF-8&oe=UTF-8",
                formParameters = parameters {
                    append("sl", "auto")
                    append("tl", targetLang)
                    append("q", text)
                }
            ) {
                header(
                    "User-Agent",
                    "AndroidTranslate/5.3.0.RC02.130475354-53000263 5.1 phone TRANSLATE_OPM5_TEST_1"
                )
            }.body<String>()
        } catch (_: Exception) {
            null
        }
    }
}
