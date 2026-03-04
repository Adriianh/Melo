package com.github.adriianh.data.remote.piped

import com.github.adriianh.core.domain.model.Track
import kotlinx.serialization.Serializable

@Serializable
data class PipedSearchResponse(
    val items: List<PipedStreamDto> = emptyList()
)

@Serializable
data class PipedStreamDto(
    val url: String = "",
    val type: String = "",
    val title: String = "",
    val uploaderName: String = "",
    val duration: Long = 0L,
    val thumbnail: String? = null
)

@Serializable
data class PipedStreamsResponse(
    val title: String = "",
    val uploader: String = "",
    val thumbnailUrl: String? = null,
    val duration: Long = 0L,
    val audioStreams: List<PipedAudioStreamDto> = emptyList()
)

@Serializable
data class PipedAudioStreamDto(
    val url: String = "",
    val mimeType: String = "",
    val quality: String = "",
    val bitrate: Int = 0
)

/** Converts a Piped search result to the domain [Track] model. */
fun PipedStreamDto.toDomain(): Track {
    val videoId = url.substringAfter("v=").substringBefore("&")
    return Track(
        id         = "piped:$videoId",
        title      = title,
        artist     = uploaderName,
        album      = "",
        durationMs = duration * 1_000L,
        genres     = emptyList(),
        artworkUrl = null,
        sourceId   = videoId.takeIf { it.isNotBlank() }
    )
}