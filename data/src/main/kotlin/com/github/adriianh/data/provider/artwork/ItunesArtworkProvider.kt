package com.github.adriianh.data.provider.artwork

import com.github.adriianh.core.domain.model.ResolvedMetadata
import com.github.adriianh.core.domain.provider.MetadataProvider
import com.github.adriianh.data.remote.itunes.ItunesApiClient

class ItunesArtworkProvider(
    private val apiClient: ItunesApiClient
) : MetadataProvider {
    override suspend fun resolveMetadata(title: String, artist: String): ResolvedMetadata? {
        val results = apiClient.search(query = "$title $artist", limit = 5)
        val normalizedTitle = title.lowercase().trim()
        val normalizedArtist = artist.lowercase().trim()

        val best = results.firstOrNull { dto ->
            dto.artistName.lowercase().contains(normalizedArtist) &&
                    dto.trackName.lowercase().contains(normalizedTitle)
        } ?: return null

        return ResolvedMetadata(
            album = best.collectionName,
            artworkUrl = best.artworkUrl100?.replace("100x100bb", "300x300bb")
        )
    }
}