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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.ColorMatrix
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import kotlin.math.min

private const val BACKDROP_CROP_ZOOM = 1.08f
private const val BACKDROP_SATURATION_BOOST = 1.22f
private const val LUMA_R = 0.2126f
private const val LUMA_G = 0.7152f
private const val LUMA_B = 0.0722f

private val backdropSaturationMatrix =
    ColorMatrix(
        (1f - BACKDROP_SATURATION_BOOST) * LUMA_R + BACKDROP_SATURATION_BOOST,
        (1f - BACKDROP_SATURATION_BOOST) * LUMA_G,
        (1f - BACKDROP_SATURATION_BOOST) * LUMA_B,
        0f,
        0f,
        (1f - BACKDROP_SATURATION_BOOST) * LUMA_R,
        (1f - BACKDROP_SATURATION_BOOST) * LUMA_G + BACKDROP_SATURATION_BOOST,
        (1f - BACKDROP_SATURATION_BOOST) * LUMA_B,
        0f,
        0f,
        (1f - BACKDROP_SATURATION_BOOST) * LUMA_R,
        (1f - BACKDROP_SATURATION_BOOST) * LUMA_G,
        (1f - BACKDROP_SATURATION_BOOST) * LUMA_B + BACKDROP_SATURATION_BOOST,
        0f,
        0f,
        0f,
        0f,
        0f,
        1f,
        0f,
    )

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

internal actual suspend fun loadBlurredArtworkBitmap(
    url: String,
    targetSizePx: Int,
    blurSigmaPx: Float,
): ImageBitmap? =
    if (!isJvmImageLoaderConfigured) {
        null
    } else {
        withContext(Dispatchers.Default) {
            val loader = SingletonImageLoader.get(PlatformContext.INSTANCE)
            val request =
                ImageRequest
                    .Builder(PlatformContext.INSTANCE)
                    .data(url)
                    .size(targetSizePx, targetSizePx)
                    .build()
            val result = loader.execute(request) as? SuccessResult
            val srcBitmap = result?.image?.toBitmap()
            if (srcBitmap == null || srcBitmap.width <= 0 || srcBitmap.height <= 0) {
                null
            } else {
                renderBlurredSkiaBitmap(srcBitmap, targetSizePx, blurSigmaPx)
            }
        }
    }

private fun renderBlurredSkiaBitmap(
    srcBitmap: Bitmap,
    targetSizePx: Int,
    blurSigmaPx: Float,
): ImageBitmap {
    val srcW = srcBitmap.width.toFloat()
    val srcH = srcBitmap.height.toFloat()
    val cropSpan = min(srcW, srcH) / BACKDROP_CROP_ZOOM
    val cropLeft = (srcW - cropSpan) * 0.5f
    val cropTop = (srcH - cropSpan) * 0.5f
    val srcRect = Rect.makeXYWH(cropLeft, cropTop, cropSpan, cropSpan)
    val dstRect = Rect.makeWH(targetSizePx.toFloat(), targetSizePx.toFloat())

    val croppedBitmap = Bitmap().apply { allocN32Pixels(targetSizePx, targetSizePx, true) }
    Image.makeFromBitmap(srcBitmap).use { srcImage ->
        Canvas(croppedBitmap).use { cropCanvas ->
            cropCanvas.drawImageRect(srcImage, srcRect, dstRect)
        }
    }

    val outBitmap = Bitmap().apply { allocN32Pixels(targetSizePx, targetSizePx, true) }
    val saturationFilter = ColorFilter.makeMatrix(backdropSaturationMatrix)
    val blurFilter = ImageFilter.makeBlur(blurSigmaPx, blurSigmaPx, FilterTileMode.CLAMP)
    val blurPaint =
        Paint().apply {
            colorFilter = saturationFilter
            imageFilter = blurFilter
        }
    val croppedImage = Image.makeFromBitmap(croppedBitmap)
    val outCanvas = Canvas(outBitmap)
    try {
        outCanvas.drawImage(croppedImage, 0f, 0f, blurPaint)
    } finally {
        outCanvas.close()
        croppedImage.close()
        blurPaint.close()
        blurFilter.close()
        saturationFilter.close()
        croppedBitmap.close()
    }
    outBitmap.setImmutable()
    return outBitmap.asComposeImageBitmap()
}
