package com.github.adriianh.cli.command.player

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.usecase.search.GetLyricsUseCase
import com.github.adriianh.core.domain.usecase.search.GetSimilarTracksUseCase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object SearchOutputFormatter {
    private val prettyJson = Json { prettyPrint = true }
    suspend fun outputJsonTracks(
        tracks: List<Track>,
        query: String,
        similar: Boolean,
        lyrics: Boolean,
        getSimilarTracks: GetSimilarTracksUseCase,
        getLyrics: GetLyricsUseCase,
    ): String {
        val first = tracks.first()
        val similarArray = if (similar) {
            val similarTracks = getSimilarTracks(first.artist, first.title)
            buildJsonArray {
                similarTracks.take(5).forEach { s ->
                    add(buildJsonObject {
                        put("title", s.title)
                        put("artist", s.artist)
                        put("match", s.match)
                    })
                }
            }
        } else null
        val lyricsText = if (lyrics) getLyrics(first.artist, first.title) else null
        val root = buildJsonObject {
            put("query", query)
            put("count", tracks.size)
            put("results", buildJsonArray {
                tracks.forEach { track ->
                    add(buildJsonObject {
                        put("id", track.id)
                        put("title", track.title)
                        put("artist", track.artist)
                        put("album", track.album)
                        put("durationMs", track.durationMs)
                        put("genres", track.genres.joinToString(", "))
                        put("artworkUrl", track.artworkUrl ?: "")
                    })
                }
            })
            if (similarArray != null) put("similar", similarArray)
            if (lyricsText != null) put("lyrics", lyricsText)
        }
        return prettyJson.encodeToString(root)
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}
