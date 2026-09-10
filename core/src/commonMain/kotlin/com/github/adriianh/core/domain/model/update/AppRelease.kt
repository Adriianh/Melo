package com.github.adriianh.core.domain.model.update

import kotlinx.serialization.Serializable

@Serializable
enum class UpdatePlatform {
    WINDOWS,
    LINUX,
    MACOS,
    ANDROID,
    UNKNOWN
}

@Serializable
data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val platform: UpdatePlatform
)

@Serializable
data class AppRelease(
    val version: String,
    val tagName: String,
    val name: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val publishedAt: String,
    val assets: List<ReleaseAsset>
) {
    fun getAssetForPlatform(platform: UpdatePlatform): ReleaseAsset? {
        return assets.firstOrNull { it.platform == platform }
    }
}