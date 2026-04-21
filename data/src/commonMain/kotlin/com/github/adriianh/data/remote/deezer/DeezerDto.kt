package com.github.adriianh.data.remote.deezer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeezerSearchResponse<T>(
    val data: List<T> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class DeezerArtistDto(
    val id: Long,
    val name: String,
    val tracklist: String = "",
)

@Serializable
data class DeezerTrackDto(
    val id: Long,
    val title: String,
    val duration: Int = 0,
    val artist: DeezerTrackArtistDto,
    val album: DeezerAlbumDto = DeezerAlbumDto(),
    val rank: Long = 0L,
)

@Serializable
data class DeezerTrackArtistDto(
    val id: Long,
    val name: String,
)

@Serializable
data class DeezerAlbumDto(
    val id: Long = 0L,
    val title: String = "",
    @SerialName("cover_medium") val coverMedium: String = "",
)