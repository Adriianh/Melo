package com.github.adriianh.melo.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adriianh.core.domain.model.Track

@Composable
expect fun PlatformAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(4.dp),
)

@Composable
fun MeloAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    if (url != null) {
        PlatformAsyncImage(
            url = url,
            contentDescription = contentDescription,
            modifier = modifier,
            size = size,
            shape = shape,
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MeloColors.surface1),
            contentAlignment = Alignment.Center,
        ) {}
    }
}

@Composable
fun TrackArtwork(track: Track, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    MeloAsyncImage(
        url = track.artworkUrl,
        contentDescription = track.title,
        modifier = modifier,
        size = size,
    )
}
