package com.github.adriianh.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Thumbnails(
    val thumbnails: List<Thumbnail>,
) {
    fun getHighResUrl(size: Int = 1080): String? {
        val lastUrl = thumbnails.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }?.url
            ?: thumbnails.lastOrNull()?.url
            ?: return null
        return resizeThumbnailUrl(lastUrl, size)
    }
}

@Serializable
data class Thumbnail(
    val url: String,
    val width: Int?,
    val height: Int?,
)

fun resizeThumbnailUrl(url: String, size: Int = 1080): String {
    var result = url
    if (result.contains("googleusercontent.com") || result.contains("ggpht.com")) {
        result = result.replace(Regex("=w\\d+-h\\d+[^?&]*"), "=w$size-h$size-l90-rj")
        result = result.replace(Regex("=s\\d+[^?&]*"), "=s$size-c")
        if (!result.contains("=") && !result.contains("?")) {
            result = "$result=w$size-h$size-l90-rj"
        }
    } else if (result.contains("i.ytimg.com/vi/") || result.contains("img.youtube.com/vi/")) {
        result = result.replace("/default.jpg", "/hq720.jpg")
            .replace("/mqdefault.jpg", "/hq720.jpg")
            .replace("/sddefault.jpg", "/hq720.jpg")
            .replace("/hqdefault.jpg", "/hq720.jpg")
    }
    return result
}