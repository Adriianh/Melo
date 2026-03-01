package com.github.adriianh.core.domain.provider

interface AudioProvider {
    suspend fun getSourceId(artist: String, title: String, durationMs: Long = 0L): String?
}