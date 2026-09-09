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
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized

private val GOOGLE_USERCONTENT_WH_REGEX = Regex("=w\\d+-h\\d+[^?&]*")
private val GOOGLE_USERCONTENT_S_REGEX = Regex("=s\\d+[^?&]*")
private val normalizedUrlCache = mutableMapOf<Pair<String, Int>, String>()
private val normalizedUrlCacheLock = Any()

@OptIn(InternalCoroutinesApi::class)
fun normalizeArtworkUrl(url: String?, size: Int = 360): String? {
    if (url.isNullOrBlank()) return url
    val cacheKey = Pair(url, size)
    synchronized(normalizedUrlCacheLock as SynchronizedObject) {
        val cached = normalizedUrlCache[cacheKey]
        if (cached != null) return cached

        var result = url
        if (result.contains("googleusercontent.com") || result.contains("ggpht.com")) {
            result = result.replace(GOOGLE_USERCONTENT_WH_REGEX, "=w$size-h$size-l90-rj")
            result = result.replace(GOOGLE_USERCONTENT_S_REGEX, "=s$size-c")
            if (!result.contains("=") && !result.contains("?")) {
                result = "$result=w$size-h$size-l90-rj"
            }
        } else if (result.contains("i.ytimg.com/vi/") || result.contains("img.youtube.com/vi/")) {
            result = if (size <= 120) {
                result.replace("/default.jpg", "/mqdefault.jpg")
                    .replace("/hq720.jpg", "/mqdefault.jpg")
                    .replace("/sddefault.jpg", "/mqdefault.jpg")
                    .replace("/hqdefault.jpg", "/mqdefault.jpg")
            } else {
                result.replace("/default.jpg", "/hqdefault.jpg")
                    .replace("/hq720.jpg", "/hqdefault.jpg")
                    .replace("/sddefault.jpg", "/hqdefault.jpg")
                    .replace("/mqdefault.jpg", "/hqdefault.jpg")
            }
        }
        if (normalizedUrlCache.size > 500) {
            normalizedUrlCache.clear()
        }
        normalizedUrlCache[cacheKey] = result
        return result
    }
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
        val targetPx = when {
            size == null -> 544
            size <= 64.dp -> 180
            size <= 160.dp -> 360
            else -> 544
        }
        val highResUrl = remember(url, targetPx) { normalizeArtworkUrl(url, targetPx) }
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