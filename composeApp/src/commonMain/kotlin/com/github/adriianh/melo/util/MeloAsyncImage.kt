package com.github.adriianh.melo.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun normalizeArtworkUrl(url: String?, size: Int = 1080): String? {
    if (url.isNullOrBlank()) return url
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

@Composable
expect fun PlatformAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    shape: Shape = RoundedCornerShape(4.dp),
)

@Composable
fun MeloAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    if (!url.isNullOrBlank()) {
        val highResUrl = remember(url) { normalizeArtworkUrl(url) }
        PlatformAsyncImage(
            url = highResUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            size = size,
            shape = shape,
        )
    } else {
        val sizeMod = if (size != null) Modifier.size(size) else Modifier
        Box(
            modifier = modifier
                .then(sizeMod)
                .clip(shape)
                .background(MeloColors.surface1),
            contentAlignment = Alignment.Center,
        ) {}
    }
}