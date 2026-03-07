package com.github.adriianh.data.remote.lastfm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LastFmTopTagsResponse(
    val toptags: LastFmTagList
)

@Serializable
data class LastFmTagList(
    val tag: List<LastFmTagDto>
)

@Serializable
data class LastFmTagDto(
    val name: String,
    val url: String
)

@Serializable
data class LastFmTokenResponse(
    val token: String,
)

@Serializable
data class LastFmSessionResponse(
    val session: LastFmSession
)

@Serializable
data class LastFmSession(
    val name: String,
    val key: String,
    val subscriber: Int = 0,
)

@Serializable
data class LastFmScrobbleResponse(
    val scrobbles: LastFmScrobbles? = null,
)

@Serializable
data class LastFmScrobbles(
    @SerialName("@attr") val attr: LastFmScrobblesAttr? = null,
)

@Serializable
data class LastFmScrobblesAttr(
    val accepted: Int = 0,
    val ignored: Int = 0,
)