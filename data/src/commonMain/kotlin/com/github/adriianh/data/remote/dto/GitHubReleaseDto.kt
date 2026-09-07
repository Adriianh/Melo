package com.github.adriianh.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubAssetDto> = emptyList()
)

@Serializable
data class GitHubAssetDto(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0L,
    @SerialName("content_type") val contentType: String? = null
)