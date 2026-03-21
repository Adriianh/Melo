package com.github.adriianh.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val genres: List<String>,
    val artworkUrl: String?,
    val sourceId: String?
)