package com.github.adriianh.core.domain.provider

interface AudioProvider {
    suspend fun getSourceId(artist: String, title: String, durationMs: Long = 0L): String?
    suspend fun getStreamUrl(sourceId: String): String?
    suspend fun downloadAudio(source: String, destination: String, format: String): String?
}