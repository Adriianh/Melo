package com.github.adriianh.melo.util

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
actual fun PlatformAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier,
    size: Dp?,
    shape: Shape,
    filterQuality: FilterQuality,
    decodeSizePx: Int?,
) {
    val sizeMod = if (size != null) Modifier.size(size) else Modifier
    val context = LocalPlatformContext.current
    val density = LocalDensity.current

    val model = remember(url, size, density, decodeSizePx) {
        if (url == null) null
        else {
            val builder = ImageRequest.Builder(context)
                .data(url)
            if (size == null || size >= 160.dp) {
                builder.crossfade(150)
            }
            val targetPx = decodeSizePx ?: size?.let { with(density) { it.roundToPx() } }
            if (targetPx != null && targetPx > 0) {
                builder.size(targetPx, targetPx)
            }
            builder.build()
        }
    }

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier
            .then(sizeMod)
            .clip(shape),
        contentScale = ContentScale.Crop,
        filterQuality = filterQuality,
    )
}