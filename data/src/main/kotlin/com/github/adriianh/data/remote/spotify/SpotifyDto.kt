package com.github.adriianh.data.remote.spotify

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifySearchResponse(
    val tracks: SpotifyTrackPaging
)

@Serializable
data class SpotifyTrackPaging(
    val items: List<SpotifyTrackDto>
)

@Serializable
data class SpotifyTrackDto(
    val id: String,
    val name: String,
    @SerialName("duration_ms") val durationMs: Int = 0,
    @SerialName("preview_url") val previewUrl: String? = null,
    val artists: List<SpotifyArtistDto> = emptyList(),
    val album: SpotifyAlbumDto? = null
)

@Serializable
data class SpotifyArtistDto(
    val id: String,
    val name: String
)

@Serializable
data class SpotifyAlbumDto(
    val name: String = "",
    @SerialName("images") val images: List<SpotifyImageDto> = emptyList()
)

@Serializable
data class SpotifyImageDto(
    val url: String,
    val width: Int,
    val height: Int
)