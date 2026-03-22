package com.github.adriianh.data.provider.artwork

import com.github.adriianh.core.domain.model.ResolvedMetadata
import com.github.adriianh.core.domain.provider.MetadataProvider

class CompositeArtworkProvider(private vararg val providers: MetadataProvider) : MetadataProvider {
    override suspend fun resolveMetadata(title: String, artist: String): ResolvedMetadata? {
        for (provider in providers) {
            val metadata = provider.resolveMetadata(title, artist)
            if (metadata != null && (!metadata.artworkUrl.isNullOrBlank() || !metadata.album.isNullOrBlank())) {
                return metadata
            }
        }
        return null
    }
}