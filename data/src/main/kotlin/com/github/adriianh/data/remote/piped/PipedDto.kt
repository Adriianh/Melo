package com.github.adriianh.data.remote.piped

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
    val duration: Long = 0L
)

@Serializable
data class PipedStreamsResponse(
    val audioStreams: List<PipedAudioStreamDto> = emptyList()
)

@Serializable
data class PipedAudioStreamDto(
    val url: String = "",
    val mimeType: String = "",
    val quality: String = "",
    val bitrate: Int = 0
)
