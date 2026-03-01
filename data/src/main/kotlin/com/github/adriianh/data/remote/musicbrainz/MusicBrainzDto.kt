package com.github.adriianh.data.remote.musicbrainz

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class MusicBrainzRecordingSearchResponse(
    val recordings: List<MusicBrainzRecording> = emptyList()
)

@Serializable
data class MusicBrainzRecording(
    val id: String,
    val title: String,
    val length: Long? = null,
    @SerialName("artist-credit") val artistCredit: List<ArtistCredit> = emptyList(),
    val releases: List<MusicBrainzRelease> = emptyList()
)

@Serializable
data class ArtistCredit(val name: String)

@Serializable
data class MusicBrainzRelease(val id: String, val title: String = "")

@Serializable
data class CoverArtResponse(val images: List<CoverArtImage> = emptyList())

@Serializable
data class CoverArtImage(
    val image: String,
    val front: Boolean = false,
    val thumbnails: CoverArtThumbnails = CoverArtThumbnails()
)

@Serializable
data class CoverArtThumbnails(
    val large: String? = null,
    @SerialName("500") val medium: String? = null
)

data class MusicBrainzTrackResult(
    val mbid: String,
    val title: String,
    val artist: String,
    val duration: Long?,
    val releaseId: String?,
    val album: String? = null
)