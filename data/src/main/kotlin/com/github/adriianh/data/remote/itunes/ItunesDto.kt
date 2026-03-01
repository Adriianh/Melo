package com.github.adriianh.data.remote.itunes

import kotlinx.serialization.Serializable

@Serializable
data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesTrackDto> = emptyList()
)

@Serializable
data class ItunesTrackDto(
    val trackId: Long = 0,
    val trackName: String = "",
    val artistName: String = "",
    val collectionName: String? = null,
    val trackTimeMillis: Long? = null,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null,
    val primaryGenreName: String? = null,
    val kind: String? = null
)

