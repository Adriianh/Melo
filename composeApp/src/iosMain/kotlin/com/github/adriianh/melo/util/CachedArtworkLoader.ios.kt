package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.ImageBitmap

internal actual suspend fun loadCachedArtworkBitmap(
    url: String,
    targetSizePx: Int,
): ImageBitmap? = null

internal actual suspend fun loadBlurredArtworkBitmap(
    url: String,
    targetSizePx: Int,
    blurSigmaPx: Float,
): ImageBitmap? = null
