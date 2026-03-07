package com.github.adriianh.data.provider.artwork

import com.github.adriianh.core.domain.provider.ArtworkProvider
import com.github.adriianh.data.remote.itunes.ItunesApiClient

class ItunesArtworkProvider(
    private val apiClient: ItunesApiClient
) : ArtworkProvider {
    override suspend fun resolveArtwork(title: String, artist: String): String? =
        apiClient.searchArtwork(title, artist)
}