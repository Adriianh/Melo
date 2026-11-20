package com.github.adriianh.melo.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.github.adriianh.melo.androidImageLoaderAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

private const val BACKDROP_CROP_ZOOM = 1.08f
private const val BACKDROP_SATURATION_BOOST = 1.22f

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

internal actual suspend fun loadBlurredArtworkBitmap(
    url: String,
    targetSizePx: Int,
    blurSigmaPx: Float,
): ImageBitmap? =
    androidImageLoaderAppContext?.let { context ->
        withContext(Dispatchers.Default) {
            val loader = SingletonImageLoader.get(context)
            val request =
                ImageRequest
                    .Builder(context)
                    .data(url)
                    .size(targetSizePx, targetSizePx)
                    .allowHardware(false)
                    .build()
            val result = loader.execute(request) as? SuccessResult
            val srcBitmap = result?.image?.toBitmap()
            if (srcBitmap == null || srcBitmap.width <= 0 || srcBitmap.height <= 0) {
                null
            } else {
                renderBlurredAndroidBitmap(srcBitmap, targetSizePx, blurSigmaPx.roundToInt())
            }
        }
    }

private fun renderBlurredAndroidBitmap(
    srcBitmap: Bitmap,
    targetSizePx: Int,
    blurRadiusPx: Int,
): ImageBitmap {
    val srcW = srcBitmap.width
    val srcH = srcBitmap.height
    val cropSpan = (min(srcW, srcH) / BACKDROP_CROP_ZOOM).roundToInt().coerceAtLeast(1)
    val cropLeft = ((srcW - cropSpan) / 2).coerceAtLeast(0)
    val cropTop = ((srcH - cropSpan) / 2).coerceAtLeast(0)

    val outBitmap = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(outBitmap)
    val srcRect = Rect(cropLeft, cropTop, cropLeft + cropSpan, cropTop + cropSpan)
    val dstRect = RectF(0f, 0f, targetSizePx.toFloat(), targetSizePx.toFloat())
    val saturationPaint =
        Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter =
                ColorMatrixColorFilter(
                    ColorMatrix().apply { setSaturation(BACKDROP_SATURATION_BOOST) },
                )
        }
    canvas.drawBitmap(srcBitmap, srcRect, dstRect, saturationPaint)

    val pixels = IntArray(targetSizePx * targetSizePx)
    outBitmap.getPixels(pixels, 0, targetSizePx, 0, 0, targetSizePx, targetSizePx)
    blurArgbPixelsInPlace(pixels, targetSizePx, targetSizePx, blurRadiusPx)
    outBitmap.setPixels(pixels, 0, targetSizePx, 0, 0, targetSizePx, targetSizePx)
    return outBitmap.asImageBitmap()
}
