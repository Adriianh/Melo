package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.github.adriianh.melo.isJvmImageLoaderConfigured

internal actual suspend fun loadCachedArtworkBitmap(
    url: String,
    targetSizePx: Int,
): ImageBitmap? =
    if (!isJvmImageLoaderConfigured) {
        null
    } else {
        val loader = SingletonImageLoader.get(PlatformContext.INSTANCE)
        val request =
            ImageRequest
                .Builder(PlatformContext.INSTANCE)
                .data(url)
                .size(targetSizePx, targetSizePx)
                .memoryCachePolicy(CachePolicy.READ_ONLY)
                .build()
        val result = loader.execute(request) as? SuccessResult
        result?.image?.toBitmap()?.asComposeImageBitmap()
    }
