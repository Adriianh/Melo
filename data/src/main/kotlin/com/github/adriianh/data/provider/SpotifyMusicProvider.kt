package com.github.adriianh.data.provider

import com.github.adriianh.core.domain.model.Track
import com.github.adriianh.core.domain.provider.MusicProvider
import com.github.adriianh.data.remote.spotify.SpotifyApiClient
import com.github.adriianh.data.remote.spotify.toDomain

class SpotifyMusicProvider(
    private val apiClient: SpotifyApiClient
) : MusicProvider {

    override suspend fun search(query: String): List<Track> {
        return apiClient.search(query, limit = 20).tracks.items.map { it.toDomain() }
    }

    override suspend fun searchAll(query: String): List<Track> {
        return apiClient.search(query, limit = 50).tracks.items.map { it.toDomain() }
    }

    override suspend fun getTrack(id: String): Track? {
        return apiClient.getTrack(id)?.toDomain()
    }
}