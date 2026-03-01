package com.github.adriianh.data.remote.lastfm

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