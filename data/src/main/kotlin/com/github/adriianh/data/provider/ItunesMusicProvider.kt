package com.github.adriianh.data.provider

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.data.remote.itunes.ItunesApiClient
import com.github.adriianh.data.remote.itunes.toDomain

class ItunesMusicProvider(
    private val apiClient: ItunesApiClient
) : MusicProvider {

    override suspend fun search(query: String): List<Track> {
        return apiClient.search(query, limit = 20).map { it.toDomain() }
    }

    override suspend fun searchAll(query: String): List<Track> {
        return apiClient.search(query, limit = 200).map { it.toDomain() }
    }

    override suspend fun getTrack(id: String): Track? {
        if (!id.startsWith("itunes:") && id.contains(':')) return null
        val trackId = id.removePrefix("itunes:")
        return apiClient.getTrack(trackId)?.toDomain()
    }
}
