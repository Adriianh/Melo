package com.github.adriianh.core.domain.provider

interface AudioProvider {
    suspend fun getSourceId(artist: String, title: String, durationMs: Long = 0L): String?
    suspend fun getStreamUrl(sourceId: String): String?

    /** Age-gate-specific resolution (e.g. with cookies + po_token). Defaults to [getStreamUrl]. */
    suspend fun getAgeRestrictedStreamUrl(sourceId: String): String? = getStreamUrl(sourceId)

    suspend fun downloadAudio(source: String, destination: String, format: String, quality: String, embedMetadata: Boolean = true): String?
}