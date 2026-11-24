package com.github.adriianh.melo.ui.components.track

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MEDIUM_WINDOW_MIN_WIDTH = 600.dp
private val EXPANDED_WINDOW_MIN_WIDTH = 840.dp

enum class SongMenuWindowSize {
    Compact,
    Medium,
    Expanded,
}

fun songMenuWindowSizeForWidth(width: Dp): SongMenuWindowSize =
    when {
        width >= EXPANDED_WINDOW_MIN_WIDTH -> SongMenuWindowSize.Expanded
        width >= MEDIUM_WINDOW_MIN_WIDTH -> SongMenuWindowSize.Medium
        else -> SongMenuWindowSize.Compact
    }

@Composable
fun rememberSongMenuWindowSize(): SongMenuWindowSize {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = with(density) { windowInfo.containerSize.width.toDp() }
    return songMenuWindowSizeForWidth(widthDp)
}
