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
) {
    val sizeMod = if (size != null) Modifier.size(size) else Modifier
    val context = LocalPlatformContext.current
    val density = LocalDensity.current

    val model = remember(url, size, density) {
        if (url == null) null
        else {
            val builder = ImageRequest.Builder(context)
                .data(url)
                .crossfade(150)
            if (size != null) {
                val px = with(density) { size.roundToPx() }
                if (px > 0) {
                    builder.size(px, px)
                }
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
        filterQuality = FilterQuality.Low,
    )
}