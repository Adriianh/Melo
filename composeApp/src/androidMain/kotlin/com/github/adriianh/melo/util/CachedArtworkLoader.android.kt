package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.github.adriianh.melo.androidImageLoaderAppContext

internal actual suspend fun loadCachedArtworkBitmap(
    url: String,
    targetSizePx: Int,
): ImageBitmap? =
    androidImageLoaderAppContext?.let { context ->
        val loader = SingletonImageLoader.get(context)
        val request =
            ImageRequest
                .Builder(context)
                .data(url)
                .size(targetSizePx, targetSizePx)
                .memoryCachePolicy(CachePolicy.READ_ONLY)
                .allowHardware(false)
                .build()
        val result = loader.execute(request) as? SuccessResult
        result?.image?.toBitmap()?.asImageBitmap()
    }
