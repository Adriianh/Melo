package com.github.adriianh.data.provider

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.MusicProvider

class FallbackMusicProvider(
    val primary: MusicProvider,
    private val fallback: MusicProvider?
) : MusicProvider {

    inline fun <reified T : MusicProvider> primaryAs(): T? = primary as? T

    override suspend fun search(query: String): List<Track> {
        val results = primary.search(query)
        if (results.isNotEmpty()) return results
        println("Primary provider returned no results, trying fallback")
        return fallback?.search(query) ?: emptyList()
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
