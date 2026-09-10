package com.github.adriianh.data.provider.music

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.MusicProvider

class FallbackMusicProvider(
    val primary: MusicProvider,
    private val fallback: MusicProvider?
) : MusicProvider {

    override suspend fun search(query: String): List<Track> {
        val results = primary.search(query)
        if (results.isNotEmpty()) return results
        return fallback?.search(query) ?: emptyList()
    }

    override suspend fun searchAll(query: String): List<Track> {
        val results = primary.searchAll(query)
        if (results.isNotEmpty()) return results
        return fallback?.searchAll(query) ?: emptyList()
    }

    override suspend fun getTrack(id: String): Track? {
        // Enruta al provider correcto según el prefijo del id
        val belongsToPrimary = id.startsWith("mb:") || id.startsWith("itunes:")
        return if (belongsToPrimary) {
            primary.getTrack(id)
        } else {
            // Sin prefijo conocido → id de Spotify u otro fallback
            fallback?.getTrack(id) ?: primary.getTrack(id)
        }
    }
}
