package com.github.adriianh.data.provider

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.data.remote.musicbrainz.MusicBrainzApiClient
import com.github.adriianh.data.remote.musicbrainz.toDomain
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class MusicBrainzMusicProvider(
    private val apiClient: MusicBrainzApiClient
) : MusicProvider {

    override suspend fun search(query: String): List<Track> = coroutineScope {
        apiClient.search(query, limit = 20).map { recording ->
            val artworkUrl = recording.releases.firstOrNull()?.id?.let { async { apiClient.getCoverArtUrl(it) } }
            recording.toDomain(artworkUrl = artworkUrl?.await())
        }
    }

    override suspend fun searchAll(query: String): List<Track> = coroutineScope {
        apiClient.search(query, limit = 100).map { recording ->
            val artworkUrl = recording.releases.firstOrNull()?.id?.let { async { apiClient.getCoverArtUrl(it) } }
            recording.toDomain(artworkUrl = artworkUrl?.await())
        }
    }

    override suspend fun getTrack(id: String): Track? = coroutineScope {
        val mbid = id.removePrefix("mb:")
        val recording = apiClient.search("rid:$mbid", limit = 1).firstOrNull() ?: return@coroutineScope null
        val artworkUrl = recording.releases.firstOrNull()?.id?.let { apiClient.getCoverArtUrl(it) }
        recording.toDomain(artworkUrl = artworkUrl)
    }
}
