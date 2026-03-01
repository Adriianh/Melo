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
        val recordings = apiClient.search(query)
        recordings.map { recording ->
            val releaseId = recording.releases.firstOrNull()?.id
            val artworkUrl = releaseId?.let {
                async { apiClient.getCoverArtUrl(it) }
            }
            recording.toDomain(artworkUrl = artworkUrl?.await())
        }
    }

    override suspend fun getTrack(id: String): Track? = coroutineScope {
        val mbid = id.removePrefix("mb:")
        val recordings = apiClient.search("rid:$mbid", limit = 1)
        val recording = recordings.firstOrNull() ?: return@coroutineScope null
        val releaseId = recording.releases.firstOrNull()?.id
        val artworkUrl = releaseId?.let { apiClient.getCoverArtUrl(it) }

        recording.toDomain(artworkUrl = artworkUrl)
    }
}
