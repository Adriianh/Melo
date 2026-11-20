package com.github.adriianh.melo.util

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MAX_BLURRED_BACKDROP_CACHE_ENTRIES = 6
private const val DEFAULT_BLUR_TARGET_SIZE_PX = 256
private const val DEFAULT_BLUR_SIGMA_PX = 10f

private val blurredBackdropCache = LinkedHashMap<String, ImageBitmap>()
private val blurredBackdropLock = Any()
private val blurredBackdropMutex = Mutex()

internal expect suspend fun loadCachedArtworkBitmap(
    url: String,
    targetSizePx: Int,
): ImageBitmap?

internal expect suspend fun loadBlurredArtworkBitmap(
    url: String,
    targetSizePx: Int = DEFAULT_BLUR_TARGET_SIZE_PX,
    blurSigmaPx: Float = DEFAULT_BLUR_SIGMA_PX,
): ImageBitmap?

@OptIn(InternalCoroutinesApi::class)
internal fun getCachedBlurredArtwork(url: String): ImageBitmap? =
    synchronized(blurredBackdropLock as SynchronizedObject) {
        blurredBackdropCache[url]
    }

@OptIn(InternalCoroutinesApi::class)
internal suspend fun getOrLoadBlurredArtworkBitmap(
    url: String,
    targetSizePx: Int = DEFAULT_BLUR_TARGET_SIZE_PX,
    blurSigmaPx: Float = DEFAULT_BLUR_SIGMA_PX,
): ImageBitmap? {
    getCachedBlurredArtwork(url)?.let { return it }
    return blurredBackdropMutex.withLock {
        getCachedBlurredArtwork(url)?.let { return@withLock it }
        val loaded =
            runCatching {
                loadBlurredArtworkBitmap(url, targetSizePx, blurSigmaPx)
            }.getOrNull()
        if (loaded != null) {
            synchronized(blurredBackdropLock as SynchronizedObject) {
                if (blurredBackdropCache.size >= MAX_BLURRED_BACKDROP_CACHE_ENTRIES) {
                    val eldestKey = blurredBackdropCache.keys.firstOrNull()
                    if (eldestKey != null) {
                        blurredBackdropCache.remove(eldestKey)
                    }
                }
                blurredBackdropCache[url] = loaded
            }
        }
        loaded
    }
}

internal fun blurArgbPixelsInPlace(
    pixels: IntArray,
    width: Int,
    height: Int,
    radius: Int,
) {
    if (radius <= 0 || width <= 0 || height <= 0) return
    val temp = IntArray(width * height)
    val kernelSize = radius * 2 + 1
    repeat(3) {
        boxBlurHorizontal(pixels, temp, width, height, radius, kernelSize)
        boxBlurVertical(temp, pixels, width, height, radius, kernelSize)
    }
}

private fun boxBlurHorizontal(
    src: IntArray,
    dst: IntArray,
    width: Int,
    height: Int,
    radius: Int,
    kernelSize: Int,
) {
    for (y in 0 until height) {
        val rowOffset = y * width
        var rSum = 0
        var gSum = 0
        var bSum = 0
        for (i in -radius..radius) {
            val px = src[rowOffset + i.coerceIn(0, width - 1)]
            rSum += (px ushr 16) and 0xFF
            gSum += (px ushr 8) and 0xFF
            bSum += px and 0xFF
        }
        for (x in 0 until width) {
            dst[rowOffset + x] =
                (0xFF shl 24) or
                ((rSum / kernelSize) shl 16) or
                ((gSum / kernelSize) shl 8) or
                (bSum / kernelSize)
            val removeIdx = (x - radius).coerceIn(0, width - 1)
            val addIdx = (x + radius + 1).coerceIn(0, width - 1)
            val removePx = src[rowOffset + removeIdx]
            val addPx = src[rowOffset + addIdx]
            rSum += ((addPx ushr 16) and 0xFF) - ((removePx ushr 16) and 0xFF)
            gSum += ((addPx ushr 8) and 0xFF) - ((removePx ushr 8) and 0xFF)
            bSum += (addPx and 0xFF) - (removePx and 0xFF)
        }
    }
}

private fun boxBlurVertical(
    src: IntArray,
    dst: IntArray,
    width: Int,
    height: Int,
    radius: Int,
    kernelSize: Int,
) {
    for (x in 0 until width) {
        var rSum = 0
        var gSum = 0
        var bSum = 0
        for (i in -radius..radius) {
            val px = src[i.coerceIn(0, height - 1) * width + x]
            rSum += (px ushr 16) and 0xFF
            gSum += (px ushr 8) and 0xFF
            bSum += px and 0xFF
        }
        for (y in 0 until height) {
            dst[y * width + x] =
                (0xFF shl 24) or
                ((rSum / kernelSize) shl 16) or
                ((gSum / kernelSize) shl 8) or
                (bSum / kernelSize)
            val removeIdx = (y - radius).coerceIn(0, height - 1)
            val addIdx = (y + radius + 1).coerceIn(0, height - 1)
            val removePx = src[removeIdx * width + x]
            val addPx = src[addIdx * width + x]
            rSum += ((addPx ushr 16) and 0xFF) - ((removePx ushr 16) and 0xFF)
            gSum += ((addPx ushr 8) and 0xFF) - ((removePx ushr 8) and 0xFF)
            bSum += (addPx and 0xFF) - (removePx and 0xFF)
        }
    }
}
