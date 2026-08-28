package com.github.adriianh.melo.util

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage

@Composable
actual fun PlatformAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier,
    size: Dp?,
    shape: Shape,
) {
    val sizeMod = if (size != null) Modifier.size(size) else Modifier
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier.then(sizeMod).clip(shape),
        contentScale = ContentScale.Crop,
    )
}
