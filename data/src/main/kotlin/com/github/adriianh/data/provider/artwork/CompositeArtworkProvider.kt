package com.github.adriianh.data.provider.artwork

import com.github.adriianh.core.domain.provider.ArtworkProvider

class CompositeArtworkProvider(private vararg val providers: ArtworkProvider) : ArtworkProvider {
    override suspend fun resolveArtwork(title: String, artist: String): String? {
        for (provider in providers) {
            val url = provider.resolveArtwork(title, artist)
            if (!url.isNullOrBlank()) return url
        }
        return null
    }
}