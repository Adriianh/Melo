package com.github.adriianh.core.domain.provider

interface ArtworkProvider {
    /** Returns the best matching artwork URL for the given title and artist, or null. */
    suspend fun resolveArtwork(title: String, artist: String): String?
}