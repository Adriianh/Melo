package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.ImageBitmap

internal expect suspend fun loadCachedArtworkBitmap(
    url: String,
    targetSizePx: Int,
): ImageBitmap?
