package com.github.adriianh.data.remote.lastfm

import kotlinx.serialization.Serializable

@Serializable
data class LastFmSimilarTracksResponse(
    val similartracks: LastFmSimilarTrackList = LastFmSimilarTrackList()
)

@Serializable
data class LastFmSimilarTrackList(
    val track: List<LastFmSimilarTrackDto> = emptyList()
)

@Serializable
data class LastFmSimilarTrackDto(
    val name: String,
    val match: Double,
    val artist: LastFmSimilarArtistDto,
    val playcount: Long = 0L,
    val mbid: String = "",
    val url: String = "",
    val duration: Int = 0,
    val streamable: LastFmStreamable = LastFmStreamable(),
    val image: List<LastFmImage> = emptyList()
)

@Serializable
data class LastFmStreamable(
    val text: String = "",
    val fulltrack: String = ""
)

@Serializable
data class LastFmImage(
    val text: String = "",
    val size: String = ""
)

@Serializable
data class LastFmSimilarArtistDto(
    val name: String
)