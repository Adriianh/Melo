package com.github.adriianh.core.domain.provider

import com.github.adriianh.core.domain.model.ResolvedMetadata

interface MetadataProvider {
    /** Returns the best matching metadata for the given title and artist, or null. */
    suspend fun resolveMetadata(title: String, artist: String): ResolvedMetadata?
}
