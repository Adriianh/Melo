package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.ImageBitmap

internal actual suspend fun loadCachedArtworkBitmap(
    url: String,
    targetSizePx: Int,
): ImageBitmap? = null
